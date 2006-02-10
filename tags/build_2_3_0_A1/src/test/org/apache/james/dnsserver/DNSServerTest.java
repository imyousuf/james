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
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.fetchmail.ReaderInputStream;

import java.io.StringReader;
import java.util.Collection;

import junit.framework.TestCase;

public class DNSServerTest extends TestCase {

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
        DNSServer d = new DNSServer();
        DefaultConfigurationBuilder db = new DefaultConfigurationBuilder();

        Configuration c = db
                .build(
                        new ReaderInputStream(
                                new StringReader(
                                        "<dnsserver><servers><server>192.168.0.1</server></servers><autodiscover>true</autodiscover><authoritative>false</authoritative></dnsserver>")),
                        "dnsserver");
        for (int i = 0; i < c.getAttributeNames().length; i++) {
            System.out.println(c.getAttributeNames()[i]);
        }

        d.enableLogging(new Logger() {

            public void debug(String arg0) {
            }

            public void debug(String arg0, Throwable arg1) {
            }

            public boolean isDebugEnabled() {
                return false;
            }

            public void info(String arg0) {
            }

            public void info(String arg0, Throwable arg1) {
            }

            public boolean isInfoEnabled() {
                return false;
            }

            public void warn(String arg0) {
            }

            public void warn(String arg0, Throwable arg1) {
            }

            public boolean isWarnEnabled() {
                return false;
            }

            public void error(String arg0) {
            }

            public void error(String arg0, Throwable arg1) {
            }

            public boolean isErrorEnabled() {
                return false;
            }

            public void fatalError(String arg0) {
            }

            public void fatalError(String arg0, Throwable arg1) {
            }

            public boolean isFatalErrorEnabled() {
                return false;
            }

            public Logger getChildLogger(String arg0) {
                return null;
            }

        });
        d.configure(c);
        d.initialize();
        Collection records = d.findMXRecords("www.pippo.com.");
        assertEquals(1, records.size());
        assertEquals("pippo.com.inbound.mxlogic.net.", records.iterator()
                .next());
        d.dispose();
    }

}
