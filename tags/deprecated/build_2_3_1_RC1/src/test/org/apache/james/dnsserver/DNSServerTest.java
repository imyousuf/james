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
import org.apache.james.test.mock.avalon.MockLogger;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;
import org.xbill.DNS.Resolver;
import org.xbill.DNS.SetResponse;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Zone;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

public class DNSServerTest extends TestCase {

    private TestableDNSServer dnsServer;

    /**
     * Please note that this is an hardcoded test that works because
     * www.pippo.com. is an alias to pippo.com and pippo.com has
     * "pippo.com.inbound.mxlogic.net." as its mx record.
     * This is the first domain with a record proving a previous james bug.
     * This test will be invalidated by any change in the pippo.com dns records
     * 
     * @param args
     * @throws Exception
     */
    public void testINARecords() throws Exception {
        Zone z = new Zone(Name.fromString("pippo.com."),getClass().getResource("pippo-com.zone").getFile());
        dnsServer.setResolver(null);
        dnsServer.setLookupper(new ZoneLookupper(z));
        Collection records = dnsServer.findMXRecords("www.pippo.com.");
        assertEquals(1, records.size());
        assertEquals("pippo.com.inbound.mxlogic.net.", records.iterator()
                .next());
    }

    /**
     * @throws Exception
     */
    public void testMXCatches() throws Exception {
        Zone z = new Zone(Name.fromString("test-zone.com."),getClass().getResource("test-zone-com.zone").getFile());
        dnsServer.setResolver(null);
        dnsServer.setLookupper(new ZoneLookupper(z));
        Collection res = dnsServer.findMXRecords("test-zone.com.");
        try {
            res.add(new Object());
            fail("MX Collection should not be modifiable");
        } catch (UnsupportedOperationException e) {
        }
        assertEquals(1,res.size());
        assertEquals("mail.test-zone.com.",res.iterator().next());
    }
    
    /**
     * Please note that this is an hardcoded test that works because
     * brandilyncollins.com. has an MX record that point to mxmail.register.com
     * and this is a CNAME to the real address.
     * This test will be invalidated by any change in the brandilyncollins.com dns records
     * 
     * @param args
     * @throws Exception
     */
    public void testCNAMEasMXrecords() throws Exception {
        Zone z = new Zone(Name.fromString("brandilyncollins.com."),getClass().getResource("brandilyncollins-com.zone").getFile());
        dnsServer.setResolver(null);
        dnsServer.setLookupper(new ZoneLookupper(z));
        Iterator records = dnsServer.getSMTPHostAddresses("brandilyncollins.com.");
        assertEquals(true, records.hasNext());
    }

    protected void setUp() throws Exception {
        dnsServer = new TestableDNSServer();
        DefaultConfigurationBuilder db = new DefaultConfigurationBuilder();

        Configuration c = db.build(
                new ByteArrayInputStream("<dnsserver><autodiscover>true</autodiscover><authoritative>false</authoritative></dnsserver>".getBytes()),
                "dnsserver");
        dnsServer.enableLogging(new MockLogger());
        dnsServer.configure(c);
        dnsServer.initialize();
    }

    protected void tearDown() throws Exception {
        dnsServer.setLookupper(null);
        dnsServer.dispose();
    }

    private class ZoneLookupper implements Lookupper {
        private final Zone z;

        private ZoneLookupper(Zone z) {
            super();
            this.z = z;
        }

        public SetResponse lookup(Name name, int type) {
            SetResponse s = z.findRecords(name,type);
            System.out.println("Zone Lookup: "+name+" "+type+" = "+s);
            return s; 
        }
    }

    private interface Lookupper {
        SetResponse lookup(Name name, int type);
    }
    
    private final class TestableDNSServer extends DNSServer {
        
        private Lookupper lookupper;

        public void setLookupper(Lookupper l) {
            this.lookupper = l;
        }
        
        public Record[] lookup(String name, int type) {
            if (lookupper != null) {
                try {
                    SetResponse lookup = lookupper.lookup(Name.fromString(name), type);
                    if (lookup != null && lookup.isSuccessful()) {
                        return processSetResponse(lookup);
                    } else {
                        return null;
                    }
                } catch (TextParseException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                return super.lookup(name, type);
            }
        }

        public void setResolver(Resolver r) {
            resolver = r;
        }

        public Resolver getResolver() {
            return resolver;
        }
    }

}
