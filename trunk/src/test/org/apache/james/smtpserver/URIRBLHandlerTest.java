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

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import junit.framework.TestCase;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.services.DNSServer;
import org.apache.james.smtpserver.core.filter.fastfail.URIRBLHandler;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.javaxmail.MockMimeMessage;
import org.apache.james.test.mock.mailet.MockMail;
import org.apache.mailet.Mail;

public class URIRBLHandlerTest extends TestCase {
    private static final String BAD_DOMAIN1 = "bad.domain.multi.surbl.org";
    private static final String BAD_DOMAIN2 = "bad2.domain.multi.surbl.org";
    private static final String GOOD_DOMAIN = "good.domain.multi.surbl.org";
    private SMTPSession mockedSMTPSession;

    private String response = null;
    
    public void setUp() {
        // reset reponse
        response = null;
    }

    private SMTPSession setupMockedSMTPSession(final Mail mail) {
        mockedSMTPSession = new AbstractSMTPSession() {

            private HashMap state = new HashMap();

            private String ipAddress = "192.168.0.1";

            private String host = "localhost";

            private boolean relayingAllowed;

            public void abortMessage() {
            }

            public Mail getMail() {
                return mail;
            }

            public String getRemoteHost() {
                return host;
            }

            public String getRemoteIPAddress() {
                return ipAddress;
            }

            public Map getState() {
                state.put(SMTPSession.SENDER, "sender@james.apache.org");
                return state;
            }

            public boolean isRelayingAllowed() {
                return relayingAllowed;
            }

            public void setRelayingAllowed(boolean relayingAllowed) {
                this.relayingAllowed = relayingAllowed;
            }

            public void writeResponse(String respString) {
                response = respString;
            }
        };

        return mockedSMTPSession;

    }

    private String getResponse() {
        return response;
    }

    private Mail setupMockedMail(MimeMessage message) {
        MockMail mail = new MockMail();
        mail.setMessage(message);
        return mail;
    }

    public MimeMessage setupMockedMimeMessage(String text)
            throws MessagingException {
        MimeMessage message = new MimeMessage(new MockMimeMessage());
        message.setText(text);
        message.saveChanges();

        return message;
    }

    /**
     * Setup the mocked dnsserver
     *
     */
    private DNSServer setupMockedDnsServer() {
        DNSServer mockedDnsServer = new DNSServer() {

            public Collection findMXRecords(String hostname) {
                throw new UnsupportedOperationException("Unimplemented in mock");
            }

            public Collection findTXTRecords(String hostname) {
                List res = new ArrayList();
                if (hostname == null) {
                    return res;
                }
                ;
                if (BAD_DOMAIN1.equals(hostname)) {
                    res.add("Blocked - see http://www.surbl.org");
                }
                return res;
            }

            public Iterator getSMTPHostAddresses(String domainName) {
                throw new UnsupportedOperationException("Unimplemented in mock");
            }

            public InetAddress[] getAllByName(String host)
                    throws UnknownHostException {
                throw new UnsupportedOperationException("Unimplemented in mock");
            }

            public InetAddress getByName(String host)
                    throws UnknownHostException {
                if (BAD_DOMAIN1.equals(host)) {
                    return InetAddress.getByName("127.0.0.1");
                } else if (BAD_DOMAIN2.equals(host)) {
                    return InetAddress.getByName("127.0.0.1");
                } else if (GOOD_DOMAIN.equals(host)) {
                    return InetAddress.getByName("fesdgaeg.deger");
                }
                return InetAddress.getByName(host);
            }
        };
        
        return mockedDnsServer;
    }
    
    public void testNotBlocked() throws IOException, MessagingException {

        
        ArrayList servers = new ArrayList();
        servers.add("multi.surbl.org");
        
        SMTPSession session = setupMockedSMTPSession(setupMockedMail(setupMockedMimeMessage("http://" + GOOD_DOMAIN + "/")));

        URIRBLHandler handler = new URIRBLHandler();

        ContainerUtil.enableLogging(handler, new MockLogger());
        handler.setDnsServer(setupMockedDnsServer());
        handler.setUriRblServer(servers);
        handler.onMessage(session);

        assertNull("Email was not rejected", getResponse());
    }
    
    public void testBlocked() throws IOException, MessagingException {

        
        ArrayList servers = new ArrayList();
        servers.add("multi.surbl.org");
        
        SMTPSession session = setupMockedSMTPSession(setupMockedMail(setupMockedMimeMessage("http://" + BAD_DOMAIN1 + "/")));

        URIRBLHandler handler = new URIRBLHandler();

        ContainerUtil.enableLogging(handler, new MockLogger());
        handler.setDnsServer(setupMockedDnsServer());
        handler.setUriRblServer(servers);
        handler.onMessage(session);

        assertNull("Email was rejected", getResponse());
    }
}
