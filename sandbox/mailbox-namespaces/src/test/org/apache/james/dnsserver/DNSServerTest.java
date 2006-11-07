/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.dnsserver;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.test.mock.avalon.MockLogger;
import org.xbill.DNS.Cache;
import org.xbill.DNS.DClass;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Message;
import org.xbill.DNS.Name;
import org.xbill.DNS.RRset;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SOARecord;
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.Zone;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

public class DNSServerTest extends TestCase {

    private TestableDNSServer dnsServer;
    private Cache defaultCache;
    private Resolver defaultResolver;
    private Name[] defaultSearchPaths;
    
    public void testNoMX() throws Exception {
        dnsServer.setResolver(null);
        dnsServer.setCache(new ZoneCache("dnstest.com."));
        //a.setSearchPath(new String[] { "searchdomain.com." });
        Collection records = dnsServer.findMXRecords("nomx.dnstest.com.");
        assertEquals(1, records.size());
        assertEquals("nomx.dnstest.com.", records.iterator()
                .next());
    }
    
    public void testBadMX() throws Exception {
        dnsServer.setResolver(null);
        dnsServer.setCache(new ZoneCache("dnstest.com."));
        //a.setSearchPath(new String[] { "searchdomain.com." });
        Collection records = dnsServer.findMXRecords("badmx.dnstest.com.");
        assertEquals(1, records.size());
        assertEquals("badhost.dnstest.com.", records.iterator()
                .next());
        Iterator it = dnsServer.getSMTPHostAddresses("badmx.dnstest.com.");
        assertFalse(it.hasNext());
    }
    
    public void testINARecords() throws Exception {
        // Zone z = loadZone("pippo.com.");
        dnsServer.setResolver(null);
        dnsServer.setCache(new ZoneCache("pippo.com."));
        // dnsServer.setLookupper(new ZoneLookupper(z));
        Collection records = dnsServer.findMXRecords("www.pippo.com.");
        assertEquals(1, records.size());
        assertEquals("pippo.com.inbound.mxlogic.net.", records.iterator()
                .next());
    }

    public void testMXCatches() throws Exception {
        // Zone z = loadZone("test-zone.com.");
        dnsServer.setResolver(null);
        dnsServer.setCache(new ZoneCache("test-zone.com."));
        // dnsServer.setLookupper(new ZoneLookupper(z));
        Collection res = dnsServer.findMXRecords("test-zone.com.");
        try {
            res.add(new Object());
            fail("MX Collection should not be modifiable");
        } catch (UnsupportedOperationException e) {
        }
        assertEquals(1,res.size());
        assertEquals("mail.test-zone.com.",res.iterator().next());
    }

    public void testCNAMEasMXrecords() throws Exception {
        // Zone z = loadZone("brandilyncollins.com.");
        dnsServer.setResolver(null);
        dnsServer.setCache(new ZoneCache("brandilyncollins.com."));
        // dnsServer.setLookupper(new ZoneLookupper(z));
        Iterator records = dnsServer.getSMTPHostAddresses("brandilyncollins.com.");
        assertEquals(true, records.hasNext());
    }

    protected void setUp() throws Exception {
        dnsServer = new TestableDNSServer();
        DefaultConfigurationBuilder db = new DefaultConfigurationBuilder();

        Configuration c = db.build(
                new ByteArrayInputStream("<dnsserver><autodiscover>true</autodiscover><authoritative>false</authoritative></dnsserver>".getBytes()),
                "dnsserver");
        ContainerUtil.enableLogging(dnsServer, new MockLogger());
        ContainerUtil.configure(dnsServer, c);
        ContainerUtil.initialize(dnsServer);
        
        
        defaultCache = Lookup.getDefaultCache(DClass.IN);
        defaultResolver = Lookup.getDefaultResolver();
        defaultSearchPaths = Lookup.getDefaultSearchPath();
        Lookup.setDefaultCache(null, DClass.IN);
        Lookup.setDefaultResolver(null);
        Lookup.setDefaultSearchPath(new Name[] {});
    }

    protected void tearDown() throws Exception {
        dnsServer.setCache(null);
        ContainerUtil.dispose(dnsServer);
        Lookup.setDefaultCache(defaultCache, DClass.IN);
        Lookup.setDefaultResolver(defaultResolver);
        Lookup.setDefaultSearchPath(defaultSearchPaths);
    }

    private Zone loadZone(String zoneName) throws IOException {
        String zoneFilename = zoneName + "zone";
        URL zoneResource = getClass().getResource(zoneFilename);
        assertNotNull("test resource for zone could not be loaded: " + zoneFilename, zoneResource);
        String zoneFile = zoneResource.getFile();
        Zone zone = new Zone(Name.fromString(zoneName),zoneFile);
        return zone;
    }

    private final class ZoneCache extends Cache {

        Zone z = null;
        
        public ZoneCache(String string) throws IOException {
            z = loadZone(string);
        }

        public SetResponse addMessage(Message arg0) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public synchronized void addNegative(Name arg0, int arg1, SOARecord arg2, int arg3) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public synchronized void addRecord(Record arg0, int arg1, Object arg2) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public synchronized void addRRset(RRset arg0, int arg1) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public synchronized void clearCache() {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public RRset[] findAnyRecords(Name arg0, int arg1) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public RRset[] findRecords(Name arg0, int arg1) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public void flushName(Name arg0) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public void flushSet(Name arg0, int arg1) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public int getDClass() {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public int getMaxCache() {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public int getMaxEntries() {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public int getMaxNCache() {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public int getSize() {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        protected synchronized SetResponse lookup(Name arg0, int arg1, int arg2) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public SetResponse lookupRecords(Name arg0, int arg1, int arg2) {
            System.out.println("Cache.lookupRecords "+arg0+","+arg1+","+arg2);
            return z.findRecords(arg0, arg1);
            //return super.lookupRecords(arg0, arg1, arg2);
        }

        public void setCleanInterval(int arg0) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public void setMaxCache(int arg0) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public void setMaxEntries(int arg0) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }

        public void setMaxNCache(int arg0) {
            throw new UnsupportedOperationException("ZoneCache is a mock used only for testing purpose");
        }
    }

    private final class TestableDNSServer extends DNSServer {
        
        public void setResolver(Resolver r) {
            resolver = r;
        }

        public Resolver getResolver() {
            return resolver;
        }
        
        public void setCache(Cache c) {
            cache = c;
        }
        
        public Cache getCache() {
            return cache;
        }
    }

}
