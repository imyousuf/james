/***********************************************************************
 * Copyright (c) 1999-2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/
package org.apache.james.dnsserver;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.DefaultConfigurationBuilder;
import org.apache.james.test.mock.avalon.MockLogger;

import java.io.ByteArrayInputStream;
import java.util.Collection;
import java.util.Iterator;

import junit.framework.TestCase;

public class DNSServerTest extends TestCase {

    private DNSServer dnsServer;

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
        Collection records = dnsServer.findMXRecords("www.pippo.com.");
        assertEquals(1, records.size());
        assertEquals("pippo.com.inbound.mxlogic.net.", records.iterator()
                .next());
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
        Iterator records = dnsServer.getSMTPHostAddresses("brandilyncollins.com.");
        assertEquals(true, records.hasNext());
    }

    protected void setUp() throws Exception {
        dnsServer = new DNSServer();
        DefaultConfigurationBuilder db = new DefaultConfigurationBuilder();

        Configuration c = db.build(
                new ByteArrayInputStream("<dnsserver><autodiscover>true</autodiscover><authoritative>false</authoritative></dnsserver>".getBytes()),
                "dnsserver");
//        for (int i = 0; i < c.getAttributeNames().length; i++) {
//            System.out.println(c.getAttributeNames()[i]);
//        }

        dnsServer.enableLogging(new MockLogger());
        dnsServer.configure(c);
        dnsServer.initialize();
    }

    protected void tearDown() throws Exception {
        dnsServer.dispose();
    }

}
