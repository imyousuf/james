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
import java.util.List;
import java.util.Map;

import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import junit.framework.TestCase;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.api.dnsservice.AbstractDNSServer;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.smtpserver.integration.URIRBLHandler;
import org.apache.james.smtpserver.protocol.BaseFakeSMTPSession;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.hook.HookResult;
import org.apache.james.smtpserver.protocol.hook.HookReturnCode;
import org.apache.james.test.mock.MockMimeMessage;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.mailet.MockMail;
import org.apache.mailet.Mail;

public class URIRBLHandlerTest extends TestCase {
    private static final String BAD_DOMAIN1 = "bad.domain.de";
    private static final String BAD_DOMAIN2 = "bad2.domain.de";
    private static final String GOOD_DOMAIN = "good.apache.org";
    private static final String URISERVER = "multi.surbl.org.";
    private SMTPSession mockedSMTPSession;
    private Mail mockedMail;

    private SMTPSession setupMockedSMTPSession(final Mail mail) {
        mockedMail = mail;
        mockedSMTPSession = new BaseFakeSMTPSession() {

            private HashMap state = new HashMap();

            private String ipAddress = "192.168.0.1";

            private String host = "localhost";

            private boolean relayingAllowed;

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
        };

        return mockedSMTPSession;

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
    
    public MimeMessage setupMockedMimeMessageMP(String text) throws MessagingException {
        MimeMessage message = new MimeMessage(new MockMimeMessage());
        
        // Create the message part 
        BodyPart messageBodyPart = new MimeBodyPart();

        // Fill the message
        messageBodyPart.setText(text);

        Multipart multipart = new MimeMultipart();
        multipart.addBodyPart(messageBodyPart);
        message.setContent(multipart);
        message.saveChanges();

        return message;
    }
    

    /**
     * Setup the mocked dnsserver
     *
     */
    private DNSService setupMockedDnsServer() {
    	DNSService mockedDnsServer = new AbstractDNSServer() {

            public Collection findTXTRecords(String hostname) {
                List res = new ArrayList();
                if (hostname == null) {
                    return res;
                }
                ;
                if ((BAD_DOMAIN1.substring(4)).equals(hostname)) {
                    res.add("Blocked - see http://www.surbl.org");
                }
                return res;
            }

            public InetAddress getByName(String host)
                    throws UnknownHostException {
                if ((BAD_DOMAIN1.substring(4) + "." + URISERVER).equals(host)) {
                    return InetAddress.getByName("127.0.0.1");
                } else if ((BAD_DOMAIN2.substring(4) + "." + URISERVER).equals(host)) {
                    return InetAddress.getByName("127.0.0.1");
                } else if ((GOOD_DOMAIN.substring(5) + "." + URISERVER).equals(host)) {
                    throw new UnknownHostException();
                }
                throw new UnsupportedOperationException("getByName("+host+") not implemented by this mock");
            }
        };
        
        return mockedDnsServer;
    }
    
    public void testNotBlocked() throws IOException, MessagingException {
        
        ArrayList servers = new ArrayList();
        servers.add(URISERVER);
        
        SMTPSession session = setupMockedSMTPSession(setupMockedMail(setupMockedMimeMessage("http://" + GOOD_DOMAIN + "/")));

        URIRBLHandler handler = new URIRBLHandler();

        ContainerUtil.enableLogging(handler, new MockLogger());
        handler.setDNSService(setupMockedDnsServer());
        handler.setUriRblServer(servers);
        HookResult response = handler.onMessage(session, mockedMail);

        assertEquals("Email was not rejected", response.getResult(),HookReturnCode.DECLINED);
    }
    
    public void testBlocked() throws IOException, MessagingException {
        
        ArrayList servers = new ArrayList();
        servers.add(URISERVER);
        
        SMTPSession session = setupMockedSMTPSession(setupMockedMail(setupMockedMimeMessage("http://" + BAD_DOMAIN1 + "/")));

        URIRBLHandler handler = new URIRBLHandler();

        ContainerUtil.enableLogging(handler, new MockLogger());
        handler.setDNSService(setupMockedDnsServer());
        handler.setUriRblServer(servers);
        HookResult response = handler.onMessage(session, mockedMail);

        assertEquals("Email was rejected", response.getResult(), HookReturnCode.DENY);
    }
    
    public void testBlockedMultiPart() throws IOException, MessagingException {
        
        ArrayList servers = new ArrayList();
        servers.add(URISERVER);
        
        SMTPSession session = setupMockedSMTPSession(setupMockedMail(setupMockedMimeMessageMP("http://" + BAD_DOMAIN1 + "/" + " " +"http://" + GOOD_DOMAIN + "/")));

        URIRBLHandler handler = new URIRBLHandler();

        ContainerUtil.enableLogging(handler, new MockLogger());
        handler.setDNSService(setupMockedDnsServer());
        handler.setUriRblServer(servers);
        HookResult response = handler.onMessage(session, mockedMail);

        assertEquals("Email was rejected", response.getResult(), HookReturnCode.DENY);
    }

    /*
    public void testAddJunkScore() throws IOException, MessagingException {
        
        ArrayList servers = new ArrayList();
        servers.add(URISERVER);
        
        SMTPSession session = setupMockedSMTPSession(setupMockedMail(setupMockedMimeMessage("http://" + BAD_DOMAIN1 + "/")));
        session.getState().put(JunkScore.JUNK_SCORE, new JunkScoreImpl());
        
        URIRBLHandler handler = new URIRBLHandler();

        ContainerUtil.enableLogging(handler, new MockLogger());
        handler.setDnsServer(setupMockedDnsServer());
        handler.setUriRblServer(servers);
        handler.setAction("junkScore");
        handler.setScore(20);
        HookResult response = handler.onMessage(session, mockedMail);

        assertNull("Email was not rejected", response);
        assertEquals("JunkScore added", ((JunkScore) session.getState().get(JunkScore.JUNK_SCORE)).getStoredScore("UriRBLCheck"), 20.0, 0d);
    }
    */
}
