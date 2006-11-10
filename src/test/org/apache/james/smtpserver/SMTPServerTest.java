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

import org.apache.avalon.cornerstone.services.sockets.SocketManager;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.commons.net.smtp.SMTPClient;
import org.apache.commons.net.smtp.SMTPReply;
import org.apache.james.Constants;
import org.apache.james.core.MailImpl;
import org.apache.james.services.DNSServer;
import org.apache.james.services.JamesConnectionManager;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.avalon.MockSocketManager;
import org.apache.james.test.mock.avalon.MockStore;
import org.apache.james.test.mock.avalon.MockThreadManager;
import org.apache.james.test.mock.james.InMemorySpoolRepository;
import org.apache.james.test.mock.james.MockMailServer;
import org.apache.james.test.mock.mailet.MockMailContext;
import org.apache.james.test.mock.mailet.MockMailetConfig;
import org.apache.james.test.util.Util;
import org.apache.james.transport.mailets.RemoteDelivery;
import org.apache.james.userrepository.MockUsersRepository;
import org.apache.james.util.Base64;
import org.apache.james.util.connection.SimpleConnectionManager;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Writer;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

/**
 * Tests the org.apache.james.smtpserver.SMTPServer unit 
 */
public class SMTPServerTest extends TestCase {
    
    private final class AlterableDNSServer implements DNSServer {
        
        private InetAddress localhostByName = null;
        
        public Collection findMXRecords(String hostname) {
            List res = new ArrayList();
            if (hostname == null) {
                return res;
            };
            if ("james.apache.org".equals(hostname)) {
                res.add("nagoya.apache.org");
            }
            return res;
        }

        public Iterator getSMTPHostAddresses(String domainName) {
            throw new UnsupportedOperationException("Unimplemented mock service");
        }

        public InetAddress[] getAllByName(String host) throws UnknownHostException {
            return new InetAddress[] {getByName(host)};
        }

        public InetAddress getByName(String host) throws UnknownHostException {
            if (getLocalhostByName() != null) {
                if ("127.0.0.1".equals(host)) return getLocalhostByName();
            }
            
            if ("1.0.0.127.bl.spamcop.net.".equals(host)) {
                return InetAddress.getByName("localhost");
            }
            
            if ("james.apache.org".equals(host)) {
                return InetAddress.getByName("james.apache.org");
            }
            
            if ("abgsfe3rsf.de".equals(host)) {
                throw new UnknownHostException();
            }
            
            if ("128.0.0.1".equals(host) || "192.168.0.1".equals(host) || "127.0.0.1".equals(host) || "127.0.0.0".equals(host) || "255.0.0.0".equals(host) || "255.255.255.255".equals(host)) {
                return InetAddress.getByName(host);
            }
            
            throw new UnsupportedOperationException("getByName not implemented in mock for host: "+host);
            //return InetAddress.getByName(host);
        }

        public Collection findTXTRecords(String hostname) {
            List res = new ArrayList();
            if (hostname == null) {
                return res;
            };
            if ("2.0.0.127.bl.spamcop.net.".equals(hostname)) {
                res.add("Blocked - see http://www.spamcop.net/bl.shtml?127.0.0.2");
            }
            return res;
        }

        public InetAddress getLocalhostByName() {
            return localhostByName;
        }

        public void setLocalhostByName(InetAddress localhostByName) {
            this.localhostByName = localhostByName;
        }

        public String getHostName(InetAddress addr) {
            return addr.getHostName();
        }

    public InetAddress getLocalHost() throws UnknownHostException {
        return InetAddress.getLocalHost();
    }
    }


    private int m_smtpListenerPort = Util.getNonPrivilegedPort();
    private MockMailServer m_mailServer;
    private SMTPTestConfiguration m_testConfiguration;
    private SMTPServer m_smtpServer;
    private MockUsersRepository m_usersRepository = new MockUsersRepository();

    public SMTPServerTest() {
        super("SMTPServerTest");
    }

    public void verifyLastMail(String sender, String recipient, MimeMessage msg) throws IOException, MessagingException {
        Mail mailData = m_mailServer.getLastMail();
        assertNotNull("mail received by mail server", mailData);

        if (sender == null && recipient == null && msg == null) fail("no verification can be done with all arguments null");

        if (sender != null) assertEquals("sender verfication", sender, ((MailAddress)mailData.getSender()).toString());
        if (recipient != null) assertTrue("recipient verfication", ((Collection) mailData.getRecipients()).contains(new MailAddress(recipient)));
        if (msg != null) {
            ByteArrayOutputStream bo1 = new ByteArrayOutputStream();
            msg.writeTo(bo1);
            ByteArrayOutputStream bo2 = new ByteArrayOutputStream();
            ((MimeMessage) mailData.getMessage()).writeTo(bo2);
            assertEquals(bo1.toString(),bo2.toString());
            assertEquals("message verification", msg, ((MimeMessage) mailData.getMessage()));
        }
    }
    
    protected void setUp() throws Exception {
        m_smtpServer = new SMTPServer();
        ContainerUtil.enableLogging(m_smtpServer,new MockLogger());
        m_serviceManager = setUpServiceManager();
        ContainerUtil.service(m_smtpServer, m_serviceManager);
        m_testConfiguration = new SMTPTestConfiguration(m_smtpListenerPort);
    }

    protected void tearDown() throws Exception {
        ContainerUtil.dispose(m_mailServer);
        super.tearDown();
    }

    private void finishSetUp(SMTPTestConfiguration testConfiguration) throws Exception {
        testConfiguration.init();
        ContainerUtil.configure(m_smtpServer, testConfiguration);
        ContainerUtil.initialize(m_smtpServer);
        m_mailServer.setMaxMessageSizeBytes(m_testConfiguration.getMaxMessageSize()*1024);
    }

    private MockServiceManager setUpServiceManager() throws Exception {
        m_serviceManager = new MockServiceManager();
        SimpleConnectionManager connectionManager = new SimpleConnectionManager();
        ContainerUtil.enableLogging(connectionManager, new MockLogger());
        m_serviceManager.put(JamesConnectionManager.ROLE, connectionManager);
        m_serviceManager.put("org.apache.mailet.MailetContext", new MockMailContext());
        m_mailServer = new MockMailServer();
        m_serviceManager.put(MailServer.ROLE, m_mailServer);
        m_serviceManager.put(UsersRepository.ROLE, m_usersRepository);
        m_serviceManager.put(SocketManager.ROLE, new MockSocketManager(m_smtpListenerPort));
        m_serviceManager.put(ThreadManager.ROLE, new MockThreadManager());
        m_dnsServer = new AlterableDNSServer();
        m_serviceManager.put(DNSServer.ROLE, m_dnsServer);
        m_serviceManager.put(Store.ROLE, new MockStore());
        return m_serviceManager;
    }

    public void testSimpleMailSendWithEHLO() throws Exception {
        finishSetUp(m_testConfiguration);
        
        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol.sendCommand("EHLO "+InetAddress.getLocalHost());
        String[] capabilityRes = smtpProtocol.getReplyStrings();
        
        List capabilitieslist = new ArrayList();
        for (int i = 1; i < capabilityRes.length; i++) {
            capabilitieslist.add(capabilityRes[i].substring(4));
        }
        
        assertEquals("capabilities", 3, capabilitieslist.size());
        assertTrue("capabilities present PIPELINING", capabilitieslist.contains("PIPELINING"));
        assertTrue("capabilities present ENHANCEDSTATUSCODES", capabilitieslist.contains("ENHANCEDSTATUSCODES"));
        assertTrue("capabilities present 8BITMIME", capabilitieslist.contains("8BITMIME"));

        smtpProtocol.setSender("mail@localhost");
        smtpProtocol.addRecipient("mail@localhost");

        smtpProtocol.sendShortMessageData("Subject: test\r\n\r\nBody\r\n\r\n.\r\n");
        smtpProtocol.quit();
        smtpProtocol.disconnect();

        // mail was propagated by SMTPServer
        assertNotNull("mail received by mail server", m_mailServer.getLastMail());
    }

    public void testEmptyMessage() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtp = new SMTPClient();
        smtp.connect("127.0.0.1", m_smtpListenerPort);

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtp.helo(InetAddress.getLocalHost().toString());
        
        smtp.setSender("mail@localhost");
        
        smtp.addRecipient("mail@localhost");

        smtp.sendShortMessageData("");

        smtp.quit();
        
        smtp.disconnect();

        // mail was propagated by SMTPServer
        assertNotNull("mail received by mail server", m_mailServer.getLastMail());

        // added to check a NPE in the test (JAMES-474) due to MockMailServer
        // not cloning the message (added a MimeMessageCopyOnWriteProxy there)
        System.gc();

        int size = ((MimeMessage) m_mailServer.getLastMail().getMessage()).getSize();

        assertEquals(size, 2);
    }

    public void testSimpleMailSendWithHELO() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol.helo(InetAddress.getLocalHost().toString());
        
        smtpProtocol.setSender("mail@localhost");
        
        smtpProtocol.addRecipient("mail@localhost");

        smtpProtocol.sendShortMessageData("Subject: test mail\r\n\r\nTest body testSimpleMailSendWithHELO\r\n.\r\n");

        smtpProtocol.quit();
        smtpProtocol.disconnect();

        // mail was propagated by SMTPServer
        assertNotNull("mail received by mail server", m_mailServer.getLastMail());
    }

    public void testTwoSimultaneousMails() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);
        SMTPClient smtpProtocol2 = new SMTPClient();
        smtpProtocol2.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken",smtpProtocol1.isConnected());
        assertTrue("second connection taken",smtpProtocol2.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost().toString());
        smtpProtocol2.helo(InetAddress.getLocalHost().toString());

        String sender1 = "mail_sender1@localhost";
        String recipient1 = "mail_recipient1@localhost";
        smtpProtocol1.setSender(sender1);
        smtpProtocol1.addRecipient(recipient1);

        String sender2 = "mail_sender2@localhost";
        String recipient2 = "mail_recipient2@localhost";
        smtpProtocol2.setSender(sender2);
        smtpProtocol2.addRecipient(recipient2);

        smtpProtocol1.sendShortMessageData("Subject: test\r\n\r\nTest body testTwoSimultaneousMails1\r\n.\r\n");
        verifyLastMail(sender1, recipient1, null);
            
        smtpProtocol2.sendShortMessageData("Subject: test\r\n\r\nTest body testTwoSimultaneousMails2\r\n.\r\n");
        verifyLastMail(sender2, recipient2, null);

        smtpProtocol1.quit();
        smtpProtocol2.quit();
        
        smtpProtocol1.disconnect();
        smtpProtocol2.disconnect();
    }

    public void testTwoMailsInSequence() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost().toString());

        String sender1 = "mail_sender1@localhost";
        String recipient1 = "mail_recipient1@localhost";
        smtpProtocol1.setSender(sender1);
        smtpProtocol1.addRecipient(recipient1);

        smtpProtocol1.sendShortMessageData("Subject: test\r\n\r\nTest body testTwoMailsInSequence1\r\n");
        verifyLastMail(sender1, recipient1, null);
            
        String sender2 = "mail_sender2@localhost";
        String recipient2 = "mail_recipient2@localhost";
        smtpProtocol1.setSender(sender2);
        smtpProtocol1.addRecipient(recipient2);

        smtpProtocol1.sendShortMessageData("Subject: test2\r\n\r\nTest body2 testTwoMailsInSequence2\r\n");
        verifyLastMail(sender2, recipient2, null);

        smtpProtocol1.quit();
        smtpProtocol1.disconnect();
    }
    
    public void testHeloResolv() throws Exception {
        m_testConfiguration.setHeloResolv();
        m_testConfiguration.setAuthorizedAddresses("192.168.0.1");
        finishSetUp(m_testConfiguration);


        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String helo1 = "abgsfe3rsf.de";
        String helo2 = "james.apache.org";
        String mail = "sender@james.apache.org";
        String rcpt = "rcpt@localhost";
        
        smtpProtocol1.sendCommand("helo",helo1);
        smtpProtocol1.setSender(mail);
        smtpProtocol1.addRecipient(rcpt);
        
        // this should give a 501 code cause the helo could not resolved
        assertEquals("expected error: helo could not resolved", 501, smtpProtocol1.getReplyCode());
            
        smtpProtocol1.sendCommand("helo", helo2);
        smtpProtocol1.setSender(mail);
        smtpProtocol1.addRecipient(rcpt);
        
        // helo is resolvable. so this should give a 250 code
        assertEquals("Helo accepted", 250, smtpProtocol1.getReplyCode());

        smtpProtocol1.quit();
    }
    
    public void testHeloResolvDefault() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);
        
        smtpProtocol1.helo("abgsfe3rsf.de");
        // helo should not be checked. so this should give a 250 code
        assertEquals("Helo accepted", 250, smtpProtocol1.getReplyCode());

        smtpProtocol1.quit();
    }
    
    public void testReverseEqualsHelo() throws Exception {
        m_testConfiguration.setReverseEqualsHelo();
        m_testConfiguration.setAuthorizedAddresses("192.168.0.1");
        // temporary alter the loopback resolution
        m_dnsServer.setLocalhostByName(InetAddress.getByName("james.apache.org"));
        try {
            finishSetUp(m_testConfiguration);
    
            SMTPClient smtpProtocol1 = new SMTPClient();
            smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);
    
            assertTrue("first connection taken", smtpProtocol1.isConnected());
    
            // no message there, yet
            assertNull("no mail received by mail server", m_mailServer
                    .getLastMail());
    
            String helo1 = "abgsfe3rsf.de";
            String helo2 = "james.apache.org";
            String mail = "sender";
            String rcpt = "recipient";
    
            smtpProtocol1.sendCommand("helo", helo1);
            smtpProtocol1.setSender(mail);
            smtpProtocol1.addRecipient(rcpt);
            
            // this should give a 501 code cause the helo not equal reverse of ip
            assertEquals("expected error: helo not equals reverse of ip", 501,
                    smtpProtocol1.getReplyCode());
    
            smtpProtocol1.sendCommand("helo", helo2);
            smtpProtocol1.setSender(mail);
            smtpProtocol1.addRecipient(rcpt);
            
            // helo is resolvable. so this should give a 250 code
            assertEquals("Helo accepted", 250, smtpProtocol1.getReplyCode());
    
            smtpProtocol1.quit();
        } finally {
            m_dnsServer.setLocalhostByName(null);
        }
    }
    
    public void testSenderDomainResolv() throws Exception {
        m_testConfiguration.setSenderDomainResolv();
        m_testConfiguration.setAuthorizedAddresses("192.168.0.1/32");
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        
        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost().toString());

        String sender1 = "mail_sender1@xfwrqqfgfe.de";
        String sender2 = "mail_sender2@james.apache.org";
        
        smtpProtocol1.setSender(sender1);
        assertEquals("expected 501 error", 501, smtpProtocol1.getReplyCode());
    
        smtpProtocol1.setSender(sender2);

        smtpProtocol1.quit();
        
    }
 
    public void testSenderDomainResolvDefault() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);
        
        smtpProtocol1.helo(InetAddress.getLocalHost().toString());
        
        String sender1 = "mail_sender1@xfwrqqfgfe.de";
        
        smtpProtocol1.setSender(sender1);

        smtpProtocol1.quit();
    }
    
    public void testSenderDomainResolvRelayClientDefault() throws Exception {
        m_testConfiguration.setSenderDomainResolv();
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        
        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost().toString());

        String sender1 = "mail_sender1@xfwrqqfgfe.de";
        
        // Both mail shold
        smtpProtocol1.setSender(sender1);

        smtpProtocol1.quit();
        
    }
    
    public void testSenderDomainResolvRelayClient() throws Exception {
        m_testConfiguration.setSenderDomainResolv();
        m_testConfiguration.setCheckAuthClients(true);
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        
        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost().toString());

        String sender1 = "mail_sender1@xfwrqqfgfe.de";
        String sender2 = "mail_sender2@james.apache.org";
        
        smtpProtocol1.setSender(sender1);
        assertEquals("expected 501 error", 501, smtpProtocol1.getReplyCode());
    
        smtpProtocol1.setSender(sender2);

        smtpProtocol1.quit();
        
    }
    
    public void testMaxRcpt() throws Exception {
        m_testConfiguration.setMaxRcpt(1);
        finishSetUp(m_testConfiguration);


        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost().toString());

        String sender1 = "mail_sender1@james.apache.org";
        String rcpt1 = "test@localhost";
        String rcpt2 = "test2@localhost";
    
        smtpProtocol1.setSender(sender1);
        smtpProtocol1.addRecipient(rcpt1);

        smtpProtocol1.addRecipient(rcpt2);
        assertEquals("expected 452 error", 452, smtpProtocol1.getReplyCode());
        
        smtpProtocol1.sendShortMessageData("Subject: test\r\n\r\nTest body testMaxRcpt1\r\n");
        
        // After the data is send the rcpt count is set back to 0.. So a new mail with rcpt should be accepted
        
        smtpProtocol1.setSender(sender1);
 
        smtpProtocol1.addRecipient(rcpt1);
        
        smtpProtocol1.sendShortMessageData("Subject: test\r\n\r\nTest body testMaxRcpt2\r\n");
        
        smtpProtocol1.quit();
        
    }

    public void testMaxRcptDefault() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);
        
        smtpProtocol1.helo(InetAddress.getLocalHost().toString());
        
        String sender1 = "mail_sender1@james.apache.org";
        String rcpt1 = "test@localhost";
        
        smtpProtocol1.setSender(sender1);
        
        smtpProtocol1.addRecipient(rcpt1);
        
        smtpProtocol1.sendShortMessageData("Subject: test\r\n\r\nTest body testMaxRcptDefault\r\n");
        
        smtpProtocol1.quit();
    }
  
    public void testEhloResolv() throws Exception {
        m_testConfiguration.setEhloResolv();
        m_testConfiguration.setAuthorizedAddresses("192.168.0.1");
        finishSetUp(m_testConfiguration);


        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String ehlo1 = "abgsfe3rsf.de";
        String ehlo2 = "james.apache.org";
        String mail = "test@account";
        String rcpt = "test";
        
        smtpProtocol1.sendCommand("ehlo", ehlo1);
        smtpProtocol1.setSender(mail);
        smtpProtocol1.addRecipient(rcpt);
        
        // this should give a 501 code cause the ehlo could not resolved
        assertEquals("expected error: ehlo could not resolved", 501, smtpProtocol1.getReplyCode());
            
        smtpProtocol1.sendCommand("ehlo", ehlo2);
        smtpProtocol1.setSender(mail);
        smtpProtocol1.addRecipient(rcpt);
        
        // ehlo is resolvable. so this should give a 250 code
        assertEquals("ehlo accepted", 250, smtpProtocol1.getReplyCode());

        smtpProtocol1.quit();
    }
    
    public void testEhloResolvDefault() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);
        
        smtpProtocol1.sendCommand("ehlo","abgsfe3rsf.de");
        // ehlo should not be checked. so this should give a 250 code
        assertEquals("ehlo accepted", 250, smtpProtocol1.getReplyCode());

        smtpProtocol1.quit();
    }
    
    public void testEhloResolvIgnoreClientDisabled() throws Exception {
        m_testConfiguration.setEhloResolv();
        m_testConfiguration.setCheckAuthNetworks(true);
        finishSetUp(m_testConfiguration);


        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String ehlo1 = "abgsfe3rsf.de";
        String ehlo2 = "james.apache.org";
        String mail = "sender@localhost";
        String rcpt = "test";
        
        smtpProtocol1.sendCommand("ehlo", ehlo1);
        smtpProtocol1.setSender(mail);
        smtpProtocol1.addRecipient(rcpt);
        
        // this should give a 501 code cause the ehlo could not resolved
        assertEquals("expected error: ehlo could not resolved", 501, smtpProtocol1.getReplyCode());
            
        smtpProtocol1.sendCommand("ehlo", ehlo2);
        smtpProtocol1.setSender(mail);
        smtpProtocol1.addRecipient(rcpt);
        // ehlo is resolvable. so this should give a 250 code
        assertEquals("ehlo accepted", 250, smtpProtocol1.getReplyCode());

        smtpProtocol1.quit();
    }
    
    public void testReverseEqualsEhlo() throws Exception {
        m_testConfiguration.setReverseEqualsEhlo();
        m_testConfiguration.setAuthorizedAddresses("192.168.0.1");
        // temporary alter the loopback resolution
        m_dnsServer.setLocalhostByName(m_dnsServer.getByName("james.apache.org"));
        try {
            finishSetUp(m_testConfiguration);
    
            SMTPClient smtpProtocol1 = new SMTPClient();
            smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);
    
            assertTrue("first connection taken", smtpProtocol1.isConnected());
    
            // no message there, yet
            assertNull("no mail received by mail server", m_mailServer
                    .getLastMail());
    
            String ehlo1 = "abgsfe3rsf.de";
            String ehlo2 = "james.apache.org";
            String mail = "sender";
            String rcpt = "recipient";
            
            smtpProtocol1.sendCommand("ehlo", ehlo1);
            smtpProtocol1.setSender(mail);
            smtpProtocol1.addRecipient(rcpt);
            
            // this should give a 501 code cause the ehlo not equals reverse of ip
            assertEquals("expected error: ehlo not equals reverse of ip", 501,
                    smtpProtocol1.getReplyCode());
    
            smtpProtocol1.sendCommand("ehlo", ehlo2);
            smtpProtocol1.setSender(mail);
            smtpProtocol1.addRecipient(rcpt);
            
            // ehlo is resolvable. so this should give a 250 code
            assertEquals("ehlo accepted", 250, smtpProtocol1.getReplyCode());
    
            smtpProtocol1.quit();
        } finally {
            m_dnsServer.setLocalhostByName(null);
        }
    }
    
    public void testHeloEnforcement() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String sender1 = "mail_sender1@localhost";
        smtpProtocol1.setSender(sender1);
        assertEquals("expected 503 error", 503, smtpProtocol1.getReplyCode());
        
        smtpProtocol1.helo(InetAddress.getLocalHost().toString());
        
        smtpProtocol1.setSender(sender1);
        
        smtpProtocol1.quit();
    }
    
    public void testHeloEnforcementDisabled() throws Exception {
        m_testConfiguration.setHeloEhloEnforcement(false);
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol1 = new SMTPClient();
        smtpProtocol1.connect("127.0.0.1", m_smtpListenerPort);

        assertTrue("first connection taken", smtpProtocol1.isConnected());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String sender1 = "mail_sender1@localhost";
        
        smtpProtocol1.setSender(sender1);
        
        smtpProtocol1.quit();
    }

    public void testAuth() throws Exception {
        m_testConfiguration.setAuthorizedAddresses("128.0.0.1/8");
        m_testConfiguration.setAuthorizingAnnounce();
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo", InetAddress.getLocalHost().toString());
        String[] capabilityRes = smtpProtocol.getReplyStrings();
        
        List capabilitieslist = new ArrayList();
        for (int i = 1; i < capabilityRes.length; i++) {
            capabilitieslist.add(capabilityRes[i].substring(4));
        }
            
        assertTrue("anouncing auth required", capabilitieslist.contains("AUTH LOGIN PLAIN"));
        // is this required or just for compatibility? assertTrue("anouncing auth required", capabilitieslist.contains("AUTH=LOGIN PLAIN"));

        String userName = "test_user_smtp";
        String noexistUserName = "noexist_test_user_smtp";
        String sender ="test_user_smtp@localhost";
        smtpProtocol.sendCommand("AUTH FOO", null);
        assertEquals("expected error: unrecognized authentication type", 504, smtpProtocol.getReplyCode());

        smtpProtocol.setSender(sender);

        smtpProtocol.addRecipient("mail@sample.com");
        assertEquals("expected 530 error", 530, smtpProtocol.getReplyCode());

        assertFalse("user not existing", m_usersRepository.contains(noexistUserName));

        smtpProtocol.sendCommand("AUTH PLAIN");
        smtpProtocol.sendCommand(Base64.encodeAsString("\0"+noexistUserName+"\0pwd\0"));
//        smtpProtocol.sendCommand(noexistUserName+"pwd".toCharArray());
        assertEquals("expected error", 535, smtpProtocol.getReplyCode());

        m_usersRepository.addUser(userName, "pwd");

        smtpProtocol.sendCommand("AUTH PLAIN");
        smtpProtocol.sendCommand(Base64.encodeAsString("\0"+userName+"\0wrongpwd\0"));
        assertEquals("expected error", 535, smtpProtocol.getReplyCode());

        smtpProtocol.sendCommand("AUTH PLAIN");
        smtpProtocol.sendCommand(Base64.encodeAsString("\0"+userName+"\0pwd\0"));
        assertEquals("authenticated", 235, smtpProtocol.getReplyCode());

        smtpProtocol.sendCommand("AUTH PLAIN");
        assertEquals("expected error: User has previously authenticated.", 503, smtpProtocol.getReplyCode());

        smtpProtocol.addRecipient("mail@sample.com");
        smtpProtocol.sendShortMessageData("Subject: test\r\n\r\nTest body testAuth\r\n");

        smtpProtocol.quit();

        // mail was propagated by SMTPServer
        assertNotNull("mail received by mail server", m_mailServer.getLastMail());
    }

    public void testAuthWithEmptySender() throws Exception {
        m_testConfiguration.setAuthorizedAddresses("128.0.0.1/8");
        m_testConfiguration.setAuthorizingAnnounce();
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo "+InetAddress.getLocalHost());

        String userName = "test_user_smtp";
        m_usersRepository.addUser(userName, "pwd");

        smtpProtocol.setSender("");

        smtpProtocol.sendCommand("AUTH PLAIN");
        smtpProtocol.sendCommand(Base64.encodeAsString("\0"+userName+"\0pwd\0"));
        assertEquals("authenticated", 235, smtpProtocol.getReplyCode());

        smtpProtocol.addRecipient("mail@sample.com");
        assertEquals("expected error", 503, smtpProtocol.getReplyCode());
        
        smtpProtocol.quit();
    }

    public void testNoRecepientSpecified() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo "+InetAddress.getLocalHost());

        smtpProtocol.setSender("mail@sample.com");

        // left out for test smtpProtocol.rcpt(new Address("mail@localhost"));

        smtpProtocol.sendShortMessageData("Subject: test\r\n\r\nTest body testNoRecepientSpecified\r\n");
        assertTrue("sending succeeded without recepient", SMTPReply.isNegativePermanent(smtpProtocol.getReplyCode()));

        smtpProtocol.quit();

        // mail was propagated by SMTPServer
        assertNull("no mail received by mail server", m_mailServer.getLastMail());
    }

    public void testMultipleMailsAndRset() throws Exception {
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo "+InetAddress.getLocalHost());

        smtpProtocol.setSender("mail@sample.com");
        
        smtpProtocol.reset();
        
        smtpProtocol.setSender("mail@sample.com");

        smtpProtocol.quit();

        // mail was propagated by SMTPServer
        assertNull("no mail received by mail server", m_mailServer.getLastMail());
    }

    public void testRelayingDenied() throws Exception {
        m_testConfiguration.setAuthorizedAddresses("128.0.0.1/8");
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo "+InetAddress.getLocalHost());

        smtpProtocol.setSender("mail@sample.com");

        smtpProtocol.addRecipient("maila@sample.com");
        assertEquals("expected 550 error", 550, smtpProtocol.getReplyCode());
    }

    public void testHandleAnnouncedMessageSizeLimitExceeded() throws Exception {
        m_testConfiguration.setMaxMessageSize(1); // set message limit to 1kb
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo "+InetAddress.getLocalHost());

        smtpProtocol.sendCommand("MAIL FROM:<mail@localhost> SIZE=1025", null);
        assertEquals("expected error: max msg size exceeded", 552, smtpProtocol.getReplyCode());

        smtpProtocol.addRecipient("mail@localhost");
        assertEquals("expected error", 503, smtpProtocol.getReplyCode());
    }

    public void testHandleMessageSizeLimitExceeded() throws Exception {
        m_testConfiguration.setMaxMessageSize(1); // set message limit to 1kb 
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo "+InetAddress.getLocalHost());

        smtpProtocol.setSender("mail@localhost");
        smtpProtocol.addRecipient("mail@localhost");

        Writer wr = smtpProtocol.sendMessageData();
        // create Body with more than 1kb . 502
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100\r\n");
        // second line
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("123456781012345678201\r\n"); // 521 + CRLF = 523 + 502 => 1025
        wr.close();
        
        assertFalse(smtpProtocol.completePendingCommand());

        assertEquals("expected 552 error", 552, smtpProtocol.getReplyCode());

    }

    public void testHandleMessageSizeLimitRespected() throws Exception {
        m_testConfiguration.setMaxMessageSize(1); // set message limit to 1kb 
        finishSetUp(m_testConfiguration);

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo "+InetAddress.getLocalHost());

        smtpProtocol.setSender("mail@localhost");
        smtpProtocol.addRecipient("mail@localhost");

        Writer wr = smtpProtocol.sendMessageData();
        // create Body with less than 1kb
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        wr.write("1234567810123456782012\r\n"); // 1022 + CRLF = 1024
        wr.close();
        
        assertTrue(smtpProtocol.completePendingCommand());

        assertEquals("expected 250 ok", 250, smtpProtocol.getReplyCode());

    }

    /*
      What we want to see is that for a given connection limit and a
      given backlog, that connection limit requests are handled, and
      that up to the backlog number of connections are queued.  More
      connections than that should error out until space opens up in
      the queue.

      For example:

        # telnet localhost <m_smtpListenPort>
        Trying 127.0.0.1...
        telnet: Unable to connect to remote host: Connection refused

      is the immediate response if the backlog is full.
    */

    public void testConnectionLimitExceeded() throws Exception {
        final int acceptLimit = 1;
        final int backlog = 1;

        m_testConfiguration.setConnectionLimit(acceptLimit);   // allow no more than <acceptLimit> connection(s) in the service
        m_testConfiguration.setConnectionBacklog(backlog);     // allow <backlog> additional connection(s) in the queue
        finishSetUp(m_testConfiguration);


        final SMTPClient[] client = new SMTPClient[acceptLimit];
        for (int i = 0; i < client.length; i++) {
            client[i] = new SMTPClient(); // should connect to worker
            try {
                client[i].connect("127.0.0.1", m_smtpListenerPort);
            } catch (Exception _) {
            }
            assertTrue("client #" + (i+1) + " established", client[i].isConnected());
        }

        // Cannot use SMTPClient.  It appears that even though the
        // client's socket is established, since the client won't be
        // able to connect to the protocol handler, the connect call
        // hangs.

        // not sure why this isn't just backlog, but empirically, I'm
        // getting a few more connections than I should.  I've tested
        // 2 and 4 (as well as 0, of course), and the result is
        // consistent on Ubuntu 2.6.12-10-amd64-k8-smp: <3 means that
        // the connection that should fail succeeds and >3 means that
        // a connection intended to backlog fails.
        final Socket connection[] = new Socket[backlog+3];

        final java.net.SocketAddress server = new java.net.InetSocketAddress("localhost", m_smtpListenerPort);

        for (int i = 0; i < connection.length; i++) {
            connection[i] = new Socket();
            try {
                connection[i].connect(server, 1000);
            } catch (Exception _) {
            }
            assertTrue("connection #" + (i+1) + " established", connection[i].isConnected());
        }

        try {
            final Socket shouldFail = new Socket();
            shouldFail.connect(server, 1000);
            fail("connection # " + (client.length + connection.length + 1) + " did not fail.");
        } catch (Exception _) {
        }

        client[0].quit();
        client[0].disconnect();

        Thread.sleep(100);
        
        // now should be able to connect (backlog)
        try {
            final Socket shouldWork = new Socket();
            shouldWork.connect(server, 1000);
            assertTrue("Additional connection established after close.", shouldWork.isConnected());
            shouldWork.close();
        } catch (Exception e) {
            fail("Could not establish additional connection after close." + e.getMessage());
        }

        // close the pending connections first, so that the server doesn't see them
        for (int i = 0; i < connection.length; i++) connection[i].close();

        // close the remaining clients
        for (int i = 1; i < client.length; i++) {
            client[i].quit();
            client[i].disconnect();
        }
    }
    
    // RemoteDelivery tests.
    
    InMemorySpoolRepository outgoingSpool;
    private MockServiceManager m_serviceManager;
    private AlterableDNSServer m_dnsServer;
    
    private Properties getStandardParameters() {
        Properties parameters = new Properties();
        parameters.put("delayTime", "500 msec, 500 msec, 500 msec"); // msec, sec, minute, hour
        parameters.put("maxRetries", "3");
        parameters.put("deliveryThreads", "1");
        parameters.put("debug", "true");
        parameters.put("sendpartial", "false");
        parameters.put("bounceProcessor", "bounce");
        parameters.put("outgoing", "mocked://outgoing/");
        return parameters;
    }
    

    /**
     * This has been created to test javamail 1.4 introduced bug.
     * http://issues.apache.org/jira/browse/JAMES-490
     */
    public void testDeliveryToSelfWithGatewayAndBind() throws Exception {
        finishSetUp(m_testConfiguration);
        outgoingSpool = new InMemorySpoolRepository();
        ((MockStore) m_serviceManager.lookup(Store.ROLE)).add("outgoing", outgoingSpool);
        
        RemoteDelivery rd = new RemoteDelivery();
        
        MockMailContext mmc = new MockMailContext();
        mmc.setAttribute(Constants.AVALON_COMPONENT_MANAGER,m_serviceManager);
        mmc.setAttribute(Constants.HELLO_NAME,"localhost");
        MockMailetConfig mci = new MockMailetConfig("Test",mmc,getStandardParameters());
        mci.setProperty("bind", "127.0.0.1");
        mci.setProperty("gateway","127.0.0.1");
        mci.setProperty("gatewayPort",""+m_smtpListenerPort);
        rd.init(mci);
        String sources = "Content-Type: text/plain;\r\nSubject: test\r\n\r\nBody";
        String sender = "test@localhost";
        String recipient = "test@localhost";
        MimeMessage mm = new MimeMessage(Session.getDefaultInstance(new Properties()),new ByteArrayInputStream(sources.getBytes()));
        MailImpl mail = new MailImpl("name",new MailAddress(sender),Arrays.asList(new MailAddress[] {new MailAddress(recipient)}),mm);
        
        rd.service(mail);
        
        while (outgoingSpool.size() > 0) {
            Thread.sleep(1000);
        }

        verifyLastMail(sender, recipient, null);
        
        assertEquals(((String) mm.getContent()).trim(),((String) ((MimeMessage) m_mailServer.getLastMail().getMessage()).getContent()).trim());
        
        mail.dispose();
    }

    
    /**
     * This is useful code to run tests on javamail bugs 
     * http://issues.apache.org/jira/browse/JAMES-52
     * 
     * This one passes with javamail 1.4.1EA
     * @throws Exception
     */
    public void test8bitmimeFromStream() throws Exception {
        finishSetUp(m_testConfiguration);
        outgoingSpool = new InMemorySpoolRepository();
        ((MockStore) m_serviceManager.lookup(Store.ROLE)).add("outgoing", outgoingSpool);
        
        RemoteDelivery rd = new RemoteDelivery();
        
        MockMailContext mmc = new MockMailContext();
        mmc.setAttribute(Constants.AVALON_COMPONENT_MANAGER,m_serviceManager);
        mmc.setAttribute(Constants.HELLO_NAME,"localhost");
        MockMailetConfig mci = new MockMailetConfig("Test",mmc,getStandardParameters());
        mci.setProperty("gateway","127.0.0.1");
        mci.setProperty("gatewayPort",""+m_smtpListenerPort);
        rd.init(mci);
        
        //String sources = "Content-Type: text/plain;\r\nContent-Transfer-Encoding: quoted-printable\r\nSubject: test\r\n\r\nBody=80\r\n";
        String sources = "Content-Type: text/plain; charset=iso-8859-15\r\nContent-Transfer-Encoding: quoted-printable\r\nSubject: test\r\n\r\nBody=80\r\n";
        //String sources = "Content-Type: text/plain; charset=iso-8859-15\r\nContent-Transfer-Encoding: 8bit\r\nSubject: test\r\n\r\nBody\u20AC\r\n";
        String sender = "test@localhost";
        String recipient = "test@localhost";
        MimeMessage mm = new MimeMessage(Session.getDefaultInstance(new Properties()),new ByteArrayInputStream(sources.getBytes()));
        MailImpl mail = new MailImpl("name",new MailAddress(sender),Arrays.asList(new MailAddress[] {new MailAddress(recipient)}),mm);
        
        rd.service(mail);
        
        while (outgoingSpool.size() > 0) {
            Thread.sleep(1000);
        }

        // verifyLastMail(sender, recipient, mm);
        verifyLastMail(sender, recipient, null);
        
        // THIS WOULD FAIL BECAUSE OF THE JAVAMAIL BUG
        assertEquals(mm.getContent(),m_mailServer.getLastMail().getMessage().getContent());
        
        mail.dispose();
    }
    
    /**
     * This is useful code to run tests on javamail bugs 
     * http://issues.apache.org/jira/browse/JAMES-52
     * 
     * This one passes with javamail 1.4.1EA
     * @throws Exception
     */
    public void test8bitmimeFromStreamWith8bitContent() throws Exception {
        finishSetUp(m_testConfiguration);
        outgoingSpool = new InMemorySpoolRepository();
        ((MockStore) m_serviceManager.lookup(Store.ROLE)).add("outgoing", outgoingSpool);
        
        RemoteDelivery rd = new RemoteDelivery();
        
        MockMailContext mmc = new MockMailContext();
        mmc.setAttribute(Constants.AVALON_COMPONENT_MANAGER,m_serviceManager);
        mmc.setAttribute(Constants.HELLO_NAME,"localhost");
        MockMailetConfig mci = new MockMailetConfig("Test",mmc,getStandardParameters());
        mci.setProperty("gateway","127.0.0.1");
        mci.setProperty("gatewayPort",""+m_smtpListenerPort);
        rd.init(mci);
        
        //String sources = "Content-Type: text/plain;\r\nContent-Transfer-Encoding: quoted-printable\r\nSubject: test\r\n\r\nBody=80\r\n";
        //String sources = "Content-Type: text/plain; charset=iso-8859-15\r\nContent-Transfer-Encoding: quoted-printable\r\nSubject: test\r\n\r\nBody=80\r\n";
        String sources = "Content-Type: text/plain; charset=iso-8859-15\r\nContent-Transfer-Encoding: 8bit\r\nSubject: test\r\n\r\nBody\u20AC\r\n";
        String sender = "test@localhost";
        String recipient = "test@localhost";
        MimeMessage mm = new MimeMessage(Session.getDefaultInstance(new Properties()),new ByteArrayInputStream(sources.getBytes()));
        MailImpl mail = new MailImpl("name",new MailAddress(sender),Arrays.asList(new MailAddress[] {new MailAddress(recipient)}),mm);
        
        rd.service(mail);
        
        while (outgoingSpool.size() > 0) {
            Thread.sleep(1000);
        }

        // verifyLastMail(sender, recipient, mm);
        verifyLastMail(sender, recipient, null);
        
        // THIS WOULD FAIL BECAUSE OF THE JAVAMAIL BUG
        assertEquals(mm.getContent(),m_mailServer.getLastMail().getMessage().getContent());
        
        mail.dispose();
    }
    
    /**
     * This is useful code to run tests on javamail bugs 
     * http://issues.apache.org/jira/browse/JAMES-52
     * 
     * This one passes with javamail 1.4.1EA
     * @throws Exception
     */
    public void test8bitmimeFromStreamWithoutContentTransferEncoding() throws Exception {
        finishSetUp(m_testConfiguration);
        outgoingSpool = new InMemorySpoolRepository();
        ((MockStore) m_serviceManager.lookup(Store.ROLE)).add("outgoing", outgoingSpool);
        
        RemoteDelivery rd = new RemoteDelivery();
        
        MockMailContext mmc = new MockMailContext();
        mmc.setAttribute(Constants.AVALON_COMPONENT_MANAGER,m_serviceManager);
        mmc.setAttribute(Constants.HELLO_NAME,"localhost");
        MockMailetConfig mci = new MockMailetConfig("Test",mmc,getStandardParameters());
        mci.setProperty("gateway","127.0.0.1");
        mci.setProperty("gatewayPort",""+m_smtpListenerPort);
        rd.init(mci);
        
        String sources = "Content-Type: text/plain;\r\nSubject: test\r\n\r\nBody\u03B2\r\n";
        //String sources = "Content-Type: text/plain; charset=iso-8859-15\r\nContent-Transfer-Encoding: quoted-printable\r\nSubject: test\r\n\r\nBody=80\r\n";
        //String sources = "Content-Type: text/plain; charset=iso-8859-15\r\nContent-Transfer-Encoding: 8bit\r\nSubject: test\r\n\r\nBody\u20AC\r\n";
        String sender = "test@localhost";
        String recipient = "test@localhost";
        MimeMessage mm = new MimeMessage(Session.getDefaultInstance(new Properties()),new ByteArrayInputStream(sources.getBytes()));
        MailImpl mail = new MailImpl("name",new MailAddress(sender),Arrays.asList(new MailAddress[] {new MailAddress(recipient)}),mm);
        
        rd.service(mail);
        
        while (outgoingSpool.size() > 0) {
            Thread.sleep(1000);
        }

        // verifyLastMail(sender, recipient, mm);
        verifyLastMail(sender, recipient, null);
        
        // THIS WOULD FAIL BECAUSE OF THE JAVAMAIL BUG
        assertEquals(mm.getContent(),m_mailServer.getLastMail().getMessage().getContent());
        
        mail.dispose();
    }
    
    /**
     * This is useful code to run tests on javamail bugs 
     * http://issues.apache.org/jira/browse/JAMES-52
     * 
     * This one passes with javamail 1.4.1EA
     * @throws Exception
     */
    public void test8bitmimeFromStreamWithoutContentTransferEncodingSentAs8bit() throws Exception {
        finishSetUp(m_testConfiguration);
        outgoingSpool = new InMemorySpoolRepository();
        ((MockStore) m_serviceManager.lookup(Store.ROLE)).add("outgoing", outgoingSpool);
        
        RemoteDelivery rd = new RemoteDelivery();
        
        MockMailContext mmc = new MockMailContext();
        mmc.setAttribute(Constants.AVALON_COMPONENT_MANAGER,m_serviceManager);
        mmc.setAttribute(Constants.HELLO_NAME,"localhost");
        MockMailetConfig mci = new MockMailetConfig("Test",mmc,getStandardParameters());
        mci.setProperty("gateway","127.0.0.1");
        mci.setProperty("gatewayPort",""+m_smtpListenerPort);
        rd.init(mci);
        
        String sources = "Content-Type: text/plain;\r\nSubject: test\r\n\r\nBody=32=48\r\n";
        //String sources = "Content-Type: text/plain; charset=iso-8859-15\r\nContent-Transfer-Encoding: quoted-printable\r\nSubject: test\r\n\r\nBody=80\r\n";
        //String sources = "Content-Type: text/plain; charset=iso-8859-15\r\nContent-Transfer-Encoding: 8bit\r\nSubject: test\r\n\r\nBody\u20AC\r\n";
        String sender = "test@localhost";
        String recipient = "test@localhost";
        MimeMessage mm = new MimeMessage(Session.getDefaultInstance(new Properties()),new ByteArrayInputStream(sources.getBytes()));
        MailImpl mail = new MailImpl("name",new MailAddress(sender),Arrays.asList(new MailAddress[] {new MailAddress(recipient)}),mm);
        
        rd.service(mail);
        
        while (outgoingSpool.size() > 0) {
            Thread.sleep(1000);
        }

        // verifyLastMail(sender, recipient, mm);
        verifyLastMail(sender, recipient, null);
        
        // THIS WOULD FAIL BECAUSE OF THE JAVAMAIL BUG
        assertEquals(mm.getContent(),m_mailServer.getLastMail().getMessage().getContent());
        
        mail.dispose();
    }
    
    /**
     * This is useful code to run tests on javamail bugs 
     * http://issues.apache.org/jira/browse/JAMES-52
     * 
     * This one passes with javamail 1.4.1EA
     * @throws Exception
     */
    public void test8bitmimeWith8bitmimeDisabledInServer() throws Exception {
        finishSetUp(m_testConfiguration);
        outgoingSpool = new InMemorySpoolRepository();
        ((MockStore) m_serviceManager.lookup(Store.ROLE)).add("outgoing", outgoingSpool);
        
        RemoteDelivery rd = new RemoteDelivery();
        
        MockMailContext mmc = new MockMailContext();
        mmc.setAttribute(Constants.AVALON_COMPONENT_MANAGER,m_serviceManager);
        mmc.setAttribute(Constants.HELLO_NAME,"localhost");
        MockMailetConfig mci = new MockMailetConfig("Test",mmc,getStandardParameters());
        mci.setProperty("gateway","127.0.0.1");
        mci.setProperty("gatewayPort",""+m_smtpListenerPort);
        mci.setProperty("mail.smtp.allow8bitmime", "false");
        rd.init(mci);
        
        //String sources = "Content-Type: text/plain;\r\nSubject: test\r\n\r\nBody=32=48\r\n";
        //String sources = "Content-Type: text/plain; charset=iso-8859-15\r\nContent-Transfer-Encoding: quoted-printable\r\nSubject: test\r\n\r\nBody=80\r\n";
        String sources = "Content-Type: text/plain; charset=iso-8859-15\r\nContent-Transfer-Encoding: 8bit\r\nSubject: test\r\n\r\nBody\u20AC\r\n";
        String sender = "test@localhost";
        String recipient = "test@localhost";
        MimeMessage mm = new MimeMessage(Session.getDefaultInstance(new Properties()),new ByteArrayInputStream(sources.getBytes()));
        MailImpl mail = new MailImpl("name",new MailAddress(sender),Arrays.asList(new MailAddress[] {new MailAddress(recipient)}),mm);
        
        rd.service(mail);
        
        while (outgoingSpool.size() > 0) {
            Thread.sleep(1000);
        }

        // verifyLastMail(sender, recipient, mm);
        verifyLastMail(sender, recipient, null);
        
        // THIS WOULD FAIL BECAUSE OF THE JAVAMAIL BUG
        assertEquals(mm.getContent(),m_mailServer.getLastMail().getMessage().getContent());
        
        mail.dispose();
    }
    
    // Check if auth users get not rejected cause rbl. See JAMES-566
    public void testDNSRBLNotRejectAuthUser() throws Exception {
        m_testConfiguration.setAuthorizedAddresses("192.168.0.1/32");
        m_testConfiguration.setAuthorizingAnnounce();
        m_testConfiguration.useRBL(true);
        finishSetUp(m_testConfiguration);

        m_dnsServer.setLocalhostByName(InetAddress.getByName("127.0.0.1"));

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo", InetAddress.getLocalHost().toString());
        String[] capabilityRes = smtpProtocol.getReplyStrings();

        List capabilitieslist = new ArrayList();
        for (int i = 1; i < capabilityRes.length; i++) {
            capabilitieslist.add(capabilityRes[i].substring(4));
        }

        assertTrue("anouncing auth required", capabilitieslist
                .contains("AUTH LOGIN PLAIN"));
        // is this required or just for compatibility? assertTrue("anouncing
        // auth required", capabilitieslist.contains("AUTH=LOGIN PLAIN"));

        String userName = "test_user_smtp";
        String sender = "test_user_smtp@localhost";

        smtpProtocol.setSender(sender);

        m_usersRepository.addUser(userName, "pwd");

        smtpProtocol.sendCommand("AUTH PLAIN");
        smtpProtocol.sendCommand(Base64.encodeAsString("\0" + userName
                + "\0pwd\0"));
        assertEquals("authenticated", 235, smtpProtocol.getReplyCode());

        smtpProtocol.addRecipient("mail@sample.com");
        assertEquals("authenticated.. not reject", 250, smtpProtocol
                .getReplyCode());

        smtpProtocol.sendShortMessageData("Subject: test\r\n\r\nTest body testDNSRBLNotRejectAuthUser\r\n");

        smtpProtocol.quit();

        // mail was propagated by SMTPServer
        assertNotNull("mail received by mail server", m_mailServer
                .getLastMail());
    }
    
   
    public void testDNSRBLRejectWorks() throws Exception {
        m_testConfiguration.setAuthorizedAddresses("192.168.0.1/32");
        m_testConfiguration.useRBL(true);
        finishSetUp(m_testConfiguration);

        m_dnsServer.setLocalhostByName(InetAddress.getByName("127.0.0.1"));

        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo", InetAddress.getLocalHost().toString());

        String sender = "test_user_smtp@localhost";

        smtpProtocol.setSender(sender);

        smtpProtocol.addRecipient("mail@sample.com");
        assertEquals("reject", 550, smtpProtocol
                .getReplyCode());

        smtpProtocol.sendShortMessageData("Subject: test\r\n\r\nTest body testDNSRBLRejectWorks\r\n");

        smtpProtocol.quit();

        // mail was rejected by SMTPServer
        assertNull("mail reject by mail server", m_mailServer
                .getLastMail());
    }
    
    
    public void testAddressBracketsEnforcementDisabled() throws Exception {
        m_testConfiguration.setAddressBracketsEnforcement(false);
        finishSetUp(m_testConfiguration);
        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo", InetAddress.getLocalHost().toString());
        
        smtpProtocol.sendCommand("mail from:", "test@localhost");
        assertEquals("accept", 250,smtpProtocol.getReplyCode());
        
        smtpProtocol.sendCommand("rcpt to:", "mail@sample.com");
        assertEquals("accept", 250,smtpProtocol.getReplyCode());

        smtpProtocol.quit();
        
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo", InetAddress.getLocalHost().toString());
        
        smtpProtocol.sendCommand("mail from:", "<test@localhost>");
        assertEquals("accept", 250,smtpProtocol.getReplyCode());
        
        smtpProtocol.sendCommand("rcpt to:", "<mail@sample.com>");
        assertEquals("accept", 250,smtpProtocol.getReplyCode());

        smtpProtocol.quit();
    }
    
    public void testAddressBracketsEnforcementEnabled() throws Exception {
        finishSetUp(m_testConfiguration);
        SMTPClient smtpProtocol = new SMTPClient();
        smtpProtocol.connect("127.0.0.1", m_smtpListenerPort);

        smtpProtocol.sendCommand("ehlo", InetAddress.getLocalHost().toString());
        
        smtpProtocol.sendCommand("mail from:", "test@localhost");
        assertEquals("reject", 501,smtpProtocol.getReplyCode());
        smtpProtocol.sendCommand("mail from:", "<test@localhost>");
        assertEquals("accept", 250,smtpProtocol.getReplyCode());
        
        smtpProtocol.sendCommand("rcpt to:", "mail@sample.com");
        assertEquals("reject", 501,smtpProtocol.getReplyCode());
        smtpProtocol.sendCommand("rcpt to:", "<mail@sample.com>");
        assertEquals("accept", 250,smtpProtocol.getReplyCode());

        smtpProtocol.quit();
    }
    
    // See http://www.ietf.org/rfc/rfc2920.txt  4: Examples
    public void testPipelining() throws Exception {
        StringBuffer buf = new StringBuffer();
        finishSetUp(m_testConfiguration);
        Socket client = new Socket("127.0.0.1",m_smtpListenerPort);
        
        buf.append("HELO TEST");
        buf.append("\r\n");
        buf.append("MAIL FROM: <test@localhost>");
        buf.append("\r\n");
        buf.append("RCPT TO: <test2@localhost>");
        buf.append("\r\n");
        buf.append("DATA");
        buf.append("\r\n");
        buf.append("Subject: test");
        buf.append("\r\n");;
        buf.append("\r\n");
        buf.append("content");
        buf.append("\r\n");
        buf.append(".");
        buf.append("\r\n");
        buf.append("quit");
        buf.append("\r\n");
        
        OutputStream out = client.getOutputStream();
        
        out.write(buf.toString().getBytes());
        out.flush();
      
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

        assertEquals("Connection made" , 220, Integer.parseInt(in.readLine().split(" ")[0]));
        assertEquals("HELO accepted" , 250, Integer.parseInt(in.readLine().split(" ")[0]));
        assertEquals("MAIL FROM accepted" , 250, Integer.parseInt(in.readLine().split(" ")[0]));
        assertEquals("RCPT TO accepted" , 250, Integer.parseInt(in.readLine().split(" ")[0]));
        assertEquals("DATA accepted" , 354, Integer.parseInt(in.readLine().split(" ")[0]));
        assertEquals("Message accepted" , 250, Integer.parseInt(in.readLine().split(" ")[0]));
        in.close();
        out.close();
        client.close();
    }
    
    // See http://www.ietf.org/rfc/rfc2920.txt  4: Examples
    public void testRejectAllRCPTPipelining() throws Exception {
        StringBuffer buf = new StringBuffer();
        m_testConfiguration.setAuthorizedAddresses("");
        finishSetUp(m_testConfiguration);
        Socket client = new Socket("127.0.0.1",m_smtpListenerPort);
        
        buf.append("HELO TEST");
        buf.append("\r\n");
        buf.append("MAIL FROM: <test@localhost>");
        buf.append("\r\n");
        buf.append("RCPT TO: <test@invalid>");
        buf.append("\r\n");
        buf.append("RCPT TO: <test2@invalid>");
        buf.append("\r\n");
        buf.append("DATA");
        buf.append("\r\n");
        buf.append("Subject: test");
        buf.append("\r\n");;
        buf.append("\r\n");
        buf.append("content");
        buf.append("\r\n");
        buf.append(".");
        buf.append("\r\n");
        buf.append("quit");
        buf.append("\r\n");
        
        OutputStream out = client.getOutputStream();
        
        out.write(buf.toString().getBytes());
        out.flush();
      
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

        assertEquals("Connection made" , 220, Integer.parseInt(in.readLine().split(" ")[0]));
        assertEquals("HELO accepted" , 250, Integer.parseInt(in.readLine().split(" ")[0]));
        assertEquals("MAIL FROM accepted" , 250, Integer.parseInt(in.readLine().split(" ")[0]));
        assertEquals("RCPT TO rejected" , 550, Integer.parseInt(in.readLine().split(" ")[0]));
        assertEquals("RCPT TO rejected" , 550, Integer.parseInt(in.readLine().split(" ")[0]));
        assertEquals("DATA not accepted" , 503, Integer.parseInt(in.readLine().split(" ")[0])); 
        in.close();
        out.close();
        client.close();
    }
    
    public void testRejectOneRCPTPipelining() throws Exception {
        StringBuffer buf = new StringBuffer();
        m_testConfiguration.setAuthorizedAddresses("");
        finishSetUp(m_testConfiguration);
        Socket client = new Socket("127.0.0.1",m_smtpListenerPort);
        
        buf.append("HELO TEST");
        buf.append("\r\n");
        buf.append("MAIL FROM: <test@localhost>");
        buf.append("\r\n");
        buf.append("RCPT TO: <test@invalid>");
        buf.append("\r\n");
        buf.append("RCPT TO: <test2@localhost>");
        buf.append("\r\n");
        buf.append("DATA");
        buf.append("\r\n");
        buf.append("Subject: test");
        buf.append("\r\n");;
        buf.append("\r\n");
        buf.append("content");
        buf.append("\r\n");
        buf.append(".");
        buf.append("\r\n");
        buf.append("quit");
        buf.append("\r\n");
        
        OutputStream out = client.getOutputStream();
        
        out.write(buf.toString().getBytes());
        out.flush();
      
        BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));

        assertEquals("Connection made" , 220, Integer.parseInt(in.readLine().split(" ")[0]));
        assertEquals("HELO accepted" , 250, Integer.parseInt(in.readLine().split(" ")[0]));
        assertEquals("MAIL FROM accepted" , 250, Integer.parseInt(in.readLine().split(" ")[0]));
        assertEquals("RCPT TO rejected" , 550, Integer.parseInt(in.readLine().split(" ")[0]));
        assertEquals("RCPT accepted" , 250, Integer.parseInt(in.readLine().split(" ")[0]));
        assertEquals("DATA accepted" , 354, Integer.parseInt(in.readLine().split(" ")[0])); 
        assertEquals("Message accepted" , 250, Integer.parseInt(in.readLine().split(" ")[0]));
        in.close();
        out.close();
        client.close();
    }
}
