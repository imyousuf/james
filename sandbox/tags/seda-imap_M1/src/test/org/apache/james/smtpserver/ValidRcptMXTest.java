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

package org.apache.james.smtpserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.mail.internet.ParseException;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.services.AbstractDNSServer;
import org.apache.james.services.DNSServer;
import org.apache.james.smtpserver.core.filter.fastfail.ValidRcptMX;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.util.junkscore.JunkScore;
import org.apache.james.util.junkscore.JunkScoreImpl;
import org.apache.mailet.MailAddress;

import junit.framework.TestCase;

public class ValidRcptMXTest extends TestCase {
    private String response = null;

    private final static String INVALID_HOST = "invalid.host.de";

    private final static String INVALID_MX = "mx." + INVALID_HOST;

    private final static String LOOPBACK = "127.0.0.1";

    protected void setUp() throws Exception {
        response = null;

        super.setUp();
    }

    private SMTPSession setupMockedSMTPSession(final MailAddress rcpt) {
        SMTPSession session = new AbstractSMTPSession() {
            HashMap state = new HashMap();
            boolean stopProcessing = false;

            public Map getState() {
                state.put(SMTPSession.CURRENT_RECIPIENT, rcpt);
                return state;
            }

            public void writeResponse(String resp) {
                response = resp;
            }
            
            public String getRemoteIPAddress() {
                return "127.0.0.1";
            }
            
            public void setStopHandlerProcessing(boolean stopProcessing) {
                this.stopProcessing = stopProcessing;
            }

        };
        return session;
    }

    private DNSServer setupMockedDNSServer() {
        DNSServer dns = new AbstractDNSServer() {

            public Collection findMXRecords(String hostname) {
                Collection mx = new ArrayList();

                if (hostname.equals(INVALID_HOST)) {
                    mx.add(INVALID_MX);
                }
                return mx;
            }
            
            public InetAddress getByName(String host) throws UnknownHostException {
                if (host.equals(INVALID_MX) || host.equals(LOOPBACK)) {
                    return InetAddress.getByName(LOOPBACK);
                } else if (host.equals("255.255.255.255")) {
                    return InetAddress.getByName("255.255.255.255");
                }
                throw new UnknownHostException("Unknown host");
            }

        };

        return dns;
    }

    public void testRejectLoopbackMX() throws ParseException {
        Collection bNetworks = new ArrayList();
        bNetworks.add("127.0.0.1");
        
        DNSServer dns = setupMockedDNSServer();
        ValidRcptMX handler = new ValidRcptMX();

        ContainerUtil.enableLogging(handler, new MockLogger());

        handler.setDNSServer(dns);
        handler.setBannedNetworks(bNetworks, dns);
        handler.onCommand(setupMockedSMTPSession(new MailAddress("test@" + INVALID_HOST)));

        assertNotNull("Reject", response);
    }
    

    public void testAddJunkScoreLoopbackMX() throws ParseException {
        Collection bNetworks = new ArrayList();
        bNetworks.add("127.0.0.1");
        
        SMTPSession session = setupMockedSMTPSession(new MailAddress("test@" + INVALID_HOST));
        session.getState().put(JunkScore.JUNK_SCORE, new JunkScoreImpl());
        
        DNSServer dns = setupMockedDNSServer();
        ValidRcptMX handler = new ValidRcptMX();
        handler.setScore(20);
        handler.setAction("junkScore");

        ContainerUtil.enableLogging(handler, new MockLogger());

        handler.setDNSServer(dns);
        handler.setBannedNetworks(bNetworks, dns);
        handler.onCommand(session);

        assertNull("Not Reject", response);
        assertEquals("JunkScore added",((JunkScore) session.getState().get(JunkScore.JUNK_SCORE)).getStoredScore("ValidRcptMXCheck"),20.0, 0d);
    }
}
