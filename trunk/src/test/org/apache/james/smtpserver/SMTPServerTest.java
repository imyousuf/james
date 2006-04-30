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
package org.apache.james.smtpserver;

import org.apache.avalon.cornerstone.services.sockets.SocketManager;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;
import org.apache.james.services.DNSServer;
import org.apache.james.services.JamesConnectionManager;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.avalon.MockSocketManager;
import org.apache.james.test.mock.avalon.MockThreadManager;
import org.apache.james.test.mock.james.MockMailServer;
import org.apache.james.test.mock.mailet.MockMailContext;
import org.apache.james.test.util.Util;
import org.apache.james.userrepository.MockUsersRepository;
import org.apache.james.util.Base64;
import org.apache.james.util.connection.SimpleConnectionManager;
import org.apache.mailet.MailAddress;
import org.columba.ristretto.composer.MimeTreeRenderer;
import org.columba.ristretto.io.CharSequenceSource;
import org.columba.ristretto.message.Address;
import org.columba.ristretto.message.Header;
import org.columba.ristretto.message.LocalMimePart;
import org.columba.ristretto.message.MimeHeader;
import org.columba.ristretto.message.MimeType;
import org.columba.ristretto.smtp.SMTPException;
import org.columba.ristretto.smtp.SMTPProtocol;
import org.columba.ristretto.smtp.SMTPResponse;

import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;
import javax.mail.util.SharedByteArrayInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

/**
 * Tests the org.apache.james.smtpserver.SMTPServer unit 
 */
public class SMTPServerTest extends TestCase {
    private int m_smtpListenerPort = Util.getNonPrivilegedPort();
    private MockMailServer m_mailServer;
    private SMTPTestConfiguration m_testConfiguration;
    private SMTPServer m_smtpServer;
    private MockUsersRepository m_usersRepository = new MockUsersRepository();

    public SMTPServerTest() {
        super("SMTPServerTest");
    }

    public void verifyLastMail(String sender, String recipient, MimeMessage msg) throws ParseException {
        Object[] mailData = m_mailServer.getLastMail();
        assertNotNull("mail received by mail server", mailData);

        if (sender == null && recipient == null && msg == null) fail("no verification can be done with all arguments null");

        if (sender != null) assertEquals("sender verfication", sender, ((MailAddress)mailData[0]).toString());
        if (recipient != null) assertTrue("recipient verfication", ((Collection) mailData[1]).contains(new MailAddress(recipient)));
        if (msg != null) assertEquals("message verification", msg, ((MimeMessage) mailData[2]));
    }
    
    protected void setUp() throws Exception {
        m_smtpServer = new SMTPServer();
        m_smtpServer.enableLogging(new MockLogger());

        m_smtpServer.service(setUpServiceManager());
        m_testConfiguration = new SMTPTestConfiguration(m_smtpListenerPort);
    }

    private void finishSetUp(SMTPTestConfiguration testConfiguration) throws Exception {
        testConfiguration.init();
        m_smtpServer.configure(testConfiguration);
        m_smtpServer.initialize();
        m_mailServer.setMaxMessageSizeBytes(m_testConfiguration.getMaxMessageSize());
    }

    private MockServiceManager setUpServiceManager() throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        SimpleConnectionManager connectionManager = new SimpleConnectionManager();
        connectionManager.enableLogging(new MockLogger());
        serviceManager.put(JamesConnectionManager.ROLE, connectionManager);
        serviceManager.put("org.apache.mailet.MailetContext", new MockMailContext());
        m_mailServer = new MockMailServer();
        serviceManager.put(MailServer.ROLE, m_mailServer);
        serviceManager.put(UsersRepository.ROLE, m_usersRepository);
        serviceManager.put(SocketManager.ROLE, new MockSocketManager(m_smtpListenerPort));
        serviceManager.put(ThreadManager.ROLE, new MockThreadManager());
        // Mock DNS Server
        DNSServer dns = new DNSServer() {

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
            
        };
        serviceManager.put(DNSServer.ROLE, dns);
        return serviceManager;
    }

    private LocalMimePart createMail() {
        MimeHeader mimeHeader = new MimeHeader(new Header());
        mimeHeader.set("Mime-Version", "1.0");
        LocalMimePart mail = new LocalMimePart(mimeHeader);
        MimeHeader header = mail.getHeader();
        header.setMimeType(new MimeType("text", "plain"));

        mail.setBody(new CharSequenceSource("James Unit Test Body"));
        return mail;
    }

    public void testSimpleMailSendWithEHLO() throws Exception, SMTPException {
        finishSetUp(m_testConfiguration);

        SMTPProtocol smtpProtocol = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol.openPort();

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String[] capabilityStrings = smtpProtocol.ehlo(InetAddress.getLocalHost());
        assertEquals("capabilities", 2, capabilityStrings.length);
        List capabilitieslist = Arrays.asList(capabilityStrings);
        assertTrue("capabilities present PIPELINING", capabilitieslist.contains("PIPELINING"));
        assertTrue("capabilities present ENHANCEDSTATUSCODES", capabilitieslist.contains("ENHANCEDSTATUSCODES"));
        //assertTrue("capabilities present 8BITMIME", capabilitieslist.contains("8BITMIME"));

        smtpProtocol.mail(new Address("mail@localhost"));
        smtpProtocol.rcpt(new Address("mail@localhost"));

        smtpProtocol.data(MimeTreeRenderer.getInstance().renderMimePart(createMail()));

        smtpProtocol.quit();

        // mail was propagated by SMTPServer
        assertNotNull("mail received by mail server", m_mailServer.getLastMail());
    }

    public void testEmptyMessage() throws Exception {
        InputStream mSource = new SharedByteArrayInputStream(("").getBytes());
        finishSetUp(m_testConfiguration);

        SMTPProtocol smtpProtocol = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol.openPort();

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol.helo(InetAddress.getLocalHost());
        
        smtpProtocol.mail(new Address("mail@localhost"));
        
        smtpProtocol.rcpt(new Address("mail@localhost"));

        smtpProtocol.data(mSource);

        smtpProtocol.quit();

        // mail was propagated by SMTPServer
        assertNotNull("mail received by mail server", m_mailServer.getLastMail());

        // added to check a NPE in the test (JAMES-474) due to MockMailServer
        // not cloning the message (added a MimeMessageCopyOnWriteProxy there)
        System.gc();

        int size = ((MimeMessage) m_mailServer.getLastMail()[2]).getSize();

        assertEquals(size, 2);
    }

    public void testSimpleMailSendWithHELO() throws Exception, SMTPException {
        finishSetUp(m_testConfiguration);

        SMTPProtocol smtpProtocol = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol.openPort();

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol.helo(InetAddress.getLocalHost());

        smtpProtocol.mail(new Address("mail@localhost"));
        smtpProtocol.rcpt(new Address("mail@localhost"));

        smtpProtocol.data(MimeTreeRenderer.getInstance().renderMimePart(createMail()));

        smtpProtocol.quit();

        // mail was propagated by SMTPServer
        assertNotNull("mail received by mail server", m_mailServer.getLastMail());
    }

    public void testTwoSimultaneousMails() throws Exception, SMTPException {
        finishSetUp(m_testConfiguration);

        SMTPProtocol smtpProtocol1 = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        SMTPProtocol smtpProtocol2 = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();
        smtpProtocol2.openPort();

        assertEquals("first connection taken", 1, smtpProtocol1.getState());
        assertEquals("second connection taken", 1, smtpProtocol2.getState());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost());
        smtpProtocol2.helo(InetAddress.getLocalHost());

        String sender1 = "mail_sender1@localhost";
        String recipient1 = "mail_recipient1@localhost";
        smtpProtocol1.mail(new Address(sender1));
        smtpProtocol1.rcpt(new Address(recipient1));

        String sender2 = "mail_sender2@localhost";
        String recipient2 = "mail_recipient2@localhost";
        smtpProtocol2.mail(new Address(sender2));
        smtpProtocol2.rcpt(new Address(recipient2));

        smtpProtocol1.data(MimeTreeRenderer.getInstance().renderMimePart(createMail()));
        verifyLastMail(sender1, recipient1, null);
            
        smtpProtocol2.data(MimeTreeRenderer.getInstance().renderMimePart(createMail()));
        verifyLastMail(sender2, recipient2, null);

        smtpProtocol1.quit();
        smtpProtocol2.quit();
    }

    public void testTwoMailsInSequence() throws Exception, SMTPException {
        finishSetUp(m_testConfiguration);

        SMTPProtocol smtpProtocol1 = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();

        assertEquals("first connection taken", 1, smtpProtocol1.getState());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost());

        String sender1 = "mail_sender1@localhost";
        String recipient1 = "mail_recipient1@localhost";
        smtpProtocol1.mail(new Address(sender1));
        smtpProtocol1.rcpt(new Address(recipient1));

        smtpProtocol1.data(MimeTreeRenderer.getInstance().renderMimePart(createMail()));
        verifyLastMail(sender1, recipient1, null);
            
        String sender2 = "mail_sender2@localhost";
        String recipient2 = "mail_recipient2@localhost";
        smtpProtocol1.mail(new Address(sender2));
        smtpProtocol1.rcpt(new Address(recipient2));

        smtpProtocol1.data(MimeTreeRenderer.getInstance().renderMimePart(createMail()));
        verifyLastMail(sender2, recipient2, null);

        smtpProtocol1.quit();
    }
    
    public void testHeloResolv() throws Exception, SMTPException {
        m_testConfiguration.setHeloResolv();
        m_testConfiguration.setAuthorizedAddresses("192.168.0.1");
        finishSetUp(m_testConfiguration);


        MySMTPProtocol smtpProtocol1 = new MySMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();

        assertEquals("first connection taken", 1, smtpProtocol1.getState());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String[] helo1 = new String[] { "abgsfe3rsf.de"};
        String[] helo2 = new String[] { "james.apache.org" };
        
        smtpProtocol1.sendCommand("helo",helo1);
        SMTPResponse response = smtpProtocol1.getResponse();
        // this should give a 501 code cause the helo could not resolved
        assertEquals("expected error: helo could not resolved", 501, response.getCode());
            
        smtpProtocol1.sendCommand("helo", helo2);
        SMTPResponse response2 = smtpProtocol1.getResponse();
        // helo is resolvable. so this should give a 250 code
        assertEquals("Helo accepted", 250, response2.getCode());

        smtpProtocol1.quit();
    }
    
    public void testHeloResolvDefault() throws Exception, SMTPException {
        finishSetUp(m_testConfiguration);

        MySMTPProtocol smtpProtocol1 = new MySMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();
        
        smtpProtocol1.sendCommand("helo",new String[]{"abgsfe3rsf.de"});
        SMTPResponse response = smtpProtocol1.getResponse();
        // helo should not be checked. so this should give a 250 code
        assertEquals("Helo accepted", 250, response.getCode());

        smtpProtocol1.quit();
    }
    
    public void testSenderDomainResolv() throws Exception, SMTPException {
        m_testConfiguration.setSenderDomainResolv();
        m_testConfiguration.setAuthorizedAddresses("192.168.0.1/32");
        finishSetUp(m_testConfiguration);

        SMTPProtocol smtpProtocol1 = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();

        
        assertEquals("first connection taken", 1, smtpProtocol1.getState());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost());

        String sender1 = "mail_sender1@xfwrqqfgfe.de";
        String sender2 = "mail_sender2@james.apache.org";
        
        try {
            smtpProtocol1.mail(new Address(sender1));
            fail("sender should not accept");
        } catch (SMTPException e) {
            assertEquals("expected 501 error", 501, e.getCode());
        }
    
        smtpProtocol1.mail(new Address(sender2));

        smtpProtocol1.quit();
        
    }
 
    public void testSenderDomainResolvDefault() throws Exception, SMTPException {
        finishSetUp(m_testConfiguration);

        SMTPProtocol smtpProtocol1 = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();
        
        smtpProtocol1.helo(InetAddress.getLocalHost());
        
        String sender1 = "mail_sender1@xfwrqqfgfe.de";
        
        smtpProtocol1.mail(new Address(sender1));

        smtpProtocol1.quit();
    }
    
    public void testSenderDomainResolvRelayClientDefault() throws Exception, SMTPException {
        m_testConfiguration.setSenderDomainResolv();
        finishSetUp(m_testConfiguration);

        SMTPProtocol smtpProtocol1 = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();

        
        assertEquals("first connection taken", 1, smtpProtocol1.getState());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost());

        String sender1 = "mail_sender1@xfwrqqfgfe.de";
        
        // Both mail shold
        smtpProtocol1.mail(new Address(sender1));

        smtpProtocol1.quit();
        
    }
    
    public void testSenderDomainResolvRelayClient() throws Exception, SMTPException {
        m_testConfiguration.setSenderDomainResolv();
        m_testConfiguration.setCheckAuthClients(true);
        finishSetUp(m_testConfiguration);

        SMTPProtocol smtpProtocol1 = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();

        
        assertEquals("first connection taken", 1, smtpProtocol1.getState());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost());

        String sender1 = "mail_sender1@xfwrqqfgfe.de";
        String sender2 = "mail_sender2@james.apache.org";
        
        try {
            smtpProtocol1.mail(new Address(sender1));
            fail("sender should not accept");
        } catch (SMTPException e) {
            assertEquals("expected 501 error", 501, e.getCode());
        }
    
        smtpProtocol1.mail(new Address(sender2));

        smtpProtocol1.quit();
        
    }
    
    public void testMaxRcpt() throws Exception, SMTPException {
        m_testConfiguration.setMaxRcpt(1);
        finishSetUp(m_testConfiguration);


        SMTPProtocol smtpProtocol1 = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();

        assertEquals("first connection taken", 1, smtpProtocol1.getState());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        smtpProtocol1.helo(InetAddress.getLocalHost());

        String sender1 = "mail_sender1@james.apache.org";
        String rcpt1 = "test@localhost";
        String rcpt2 = "test2@localhost";
    
        smtpProtocol1.mail(new Address(sender1));
        smtpProtocol1.rcpt(new Address(rcpt1));

        try {
            smtpProtocol1.rcpt(new Address(rcpt2));
            fail("rcpt should not accepted");
        } catch (SMTPException e) {
            assertEquals("expected 452 error", 452, e.getCode());
        }
        
        smtpProtocol1.data(MimeTreeRenderer.getInstance().renderMimePart(createMail()));
        
        // After the data is send the rcpt count is set back to 0.. So a new mail with rcpt should be accepted
        
        smtpProtocol1.mail(new Address(sender1));
 
        smtpProtocol1.rcpt(new Address(rcpt1));
        
        smtpProtocol1.data(MimeTreeRenderer.getInstance().renderMimePart(createMail()));
        
        smtpProtocol1.quit();
        
    }

    public void testMaxRcptDefault() throws Exception, SMTPException {
        finishSetUp(m_testConfiguration);

        SMTPProtocol smtpProtocol1 = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();
        
        smtpProtocol1.helo(InetAddress.getLocalHost());
        
        String sender1 = "mail_sender1@james.apache.org";
        String rcpt1 = "test@localhost";
        
        smtpProtocol1.mail(new Address(sender1));
        
        smtpProtocol1.rcpt(new Address(rcpt1));
        
        smtpProtocol1.data(MimeTreeRenderer.getInstance().renderMimePart(createMail()));
        
        smtpProtocol1.quit();
    }
  
    public void testEhloResolv() throws Exception, SMTPException {
        m_testConfiguration.setEhloResolv();
        m_testConfiguration.setAuthorizedAddresses("192.168.0.1");
        finishSetUp(m_testConfiguration);


        MySMTPProtocol smtpProtocol1 = new MySMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();

        assertEquals("first connection taken", 1, smtpProtocol1.getState());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String[] ehlo1 = new String[] { "abgsfe3rsf.de"};
        String[] ehlo2 = new String[] { "james.apache.org" };
        
        smtpProtocol1.sendCommand("ehlo", ehlo1);
        SMTPResponse response = smtpProtocol1.getResponse();
        // this should give a 501 code cause the ehlo could not resolved
        assertEquals("expected error: ehlo could not resolved", 501, response.getCode());
            
        smtpProtocol1.sendCommand("ehlo", ehlo2);
        SMTPResponse response2 = smtpProtocol1.getResponse();
        // ehlo is resolvable. so this should give a 250 code
        assertEquals("ehlo accepted", 250, response2.getCode());

        smtpProtocol1.quit();
    }
    
    public void testEhloResolvDefault() throws Exception, SMTPException {
        finishSetUp(m_testConfiguration);

        MySMTPProtocol smtpProtocol1 = new MySMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();
        
        smtpProtocol1.sendCommand("ehlo",new String[]{"abgsfe3rsf.de"});
        SMTPResponse response = smtpProtocol1.getResponse();
        // ehlo should not be checked. so this should give a 250 code
        assertEquals("ehlo accepted", 250, response.getCode());

        smtpProtocol1.quit();
    }
    
    public void testEhloResolvIgnoreClientDisabled() throws Exception, SMTPException {
        m_testConfiguration.setEhloResolv();
        m_testConfiguration.setCheckAuthNetworks(true);
        finishSetUp(m_testConfiguration);


        MySMTPProtocol smtpProtocol1 = new MySMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();

        assertEquals("first connection taken", 1, smtpProtocol1.getState());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String[] ehlo1 = new String[] { "abgsfe3rsf.de"};
        String[] ehlo2 = new String[] { "james.apache.org" };
        
        smtpProtocol1.sendCommand("ehlo", ehlo1);
        SMTPResponse response = smtpProtocol1.getResponse();
        // this should give a 501 code cause the ehlo could not resolved
        assertEquals("expected error: ehlo could not resolved", 501, response.getCode());
            
        smtpProtocol1.sendCommand("ehlo", ehlo2);
        SMTPResponse response2 = smtpProtocol1.getResponse();
        // ehlo is resolvable. so this should give a 250 code
        assertEquals("ehlo accepted", 250, response2.getCode());

        smtpProtocol1.quit();
    }
    
    public void testHeloEnforcement() throws Exception, SMTPException {
        finishSetUp(m_testConfiguration);

        SMTPProtocol smtpProtocol1 = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();

        assertEquals("first connection taken", 1, smtpProtocol1.getState());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String sender1 = "mail_sender1@localhost";
        try {
            smtpProtocol1.mail(new Address(sender1));
            fail("helo not enforced");
        } catch (SMTPException e) {
            assertEquals("expected 503 error", 503, e.getCode());
        }
        
        smtpProtocol1.helo(InetAddress.getLocalHost());
        
        smtpProtocol1.mail(new Address(sender1));
        
        smtpProtocol1.quit();
    }
    
    public void testHeloEnforcementDisabled() throws Exception, SMTPException {
        m_testConfiguration.setHeloEhloEnforcement(false);
        finishSetUp(m_testConfiguration);

        SMTPProtocol smtpProtocol1 = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();

        assertEquals("first connection taken", 1, smtpProtocol1.getState());

        // no message there, yet
        assertNull("no mail received by mail server", m_mailServer.getLastMail());

        String sender1 = "mail_sender1@localhost";
        
        smtpProtocol1.mail(new Address(sender1));
        
        smtpProtocol1.quit();
    }

    public void testAuth() throws Exception, SMTPException {
        m_testConfiguration.setAuthorizedAddresses("128.0.0.1/8");
        m_testConfiguration.setAuthorizingAnnounce();
        finishSetUp(m_testConfiguration);

        MySMTPProtocol smtpProtocol = new MySMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol.openPort();

        String[] capabilityStrings = smtpProtocol.ehlo(InetAddress.getLocalHost());
        List capabilitieslist = Arrays.asList(capabilityStrings);
        assertTrue("anouncing auth required", capabilitieslist.contains("AUTH LOGIN PLAIN"));
        // is this required or just for compatibility? assertTrue("anouncing auth required", capabilitieslist.contains("AUTH=LOGIN PLAIN"));

        String userName = "test_user_smtp";
        String noexistUserName = "noexist_test_user_smtp";
        String sender ="test_user_smtp@localhost";
        smtpProtocol.sendCommand("AUTH FOO", null);
        SMTPResponse response = smtpProtocol.getResponse();
        assertEquals("expected error: unrecognized authentication type", 504, response.getCode());

        smtpProtocol.mail(new Address(sender));

        try {
            smtpProtocol.rcpt(new Address("mail@sample.com"));
            fail("no auth required");
        } catch (SMTPException e) {
            assertEquals("expected 530 error", 530, e.getCode());
        }

        assertFalse("user not existing", m_usersRepository.contains(noexistUserName));
        try {
            smtpProtocol.auth("PLAIN", noexistUserName, "pwd".toCharArray());
            fail("auth succeeded for non-existing user");
        } catch (SMTPException e) {
            assertEquals("expected error", 535, e.getCode());
        }

        m_usersRepository.addUser(userName, "pwd");
        try {
            smtpProtocol.auth("PLAIN", userName, "wrongpwd".toCharArray());
            fail("auth succeeded with wrong password");
        } catch (SMTPException e) {
            assertEquals("expected error", 535, e.getCode());
        }

        try {
            smtpProtocol.auth("PLAIN", userName, "pwd".toCharArray());
        } catch (SMTPException e) {
            e.printStackTrace(); 
            fail("authentication failed");
        }

        smtpProtocol.sendCommand("AUTH PLAIN ", new String[]{Base64.encodeAsString("\0" + userName + "\0pwd")});
        response = smtpProtocol.getResponse();
        assertEquals("expected error: User has previously authenticated.", 503, response.getCode());

        smtpProtocol.rcpt(new Address("mail@sample.com"));
        smtpProtocol.data(MimeTreeRenderer.getInstance().renderMimePart(createMail()));

        smtpProtocol.quit();

        // mail was propagated by SMTPServer
        assertNotNull("mail received by mail server", m_mailServer.getLastMail());
    }

    public void testAuthWithEmptySender() throws Exception {
        m_testConfiguration.setAuthorizedAddresses("128.0.0.1/8");
        m_testConfiguration.setAuthorizingAnnounce();
        finishSetUp(m_testConfiguration);

        MySMTPProtocol smtpProtocol = new MySMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol.openPort();

        smtpProtocol.ehlo(InetAddress.getLocalHost());

        String userName = "test_user_smtp";
        m_usersRepository.addUser(userName, "pwd");

        smtpProtocol.mail(new Address(""));

        try {
            smtpProtocol.auth("PLAIN", userName, "pwd".toCharArray());
        } catch (SMTPException e) {
            e.printStackTrace(); 
            fail("authentication failed");
        }

        try {
            smtpProtocol.rcpt(new Address("mail@sample.com"));
            fail("smtpserver allowed an empty sender for an authenticated user");
        } catch (SMTPException e) {
            assertEquals("expected error", 503, e.getCode());
        }
        
        smtpProtocol.quit();
    }

    public void testNoRecepientSpecified() throws Exception, SMTPException {
        finishSetUp(m_testConfiguration);

        MySMTPProtocol smtpProtocol = new MySMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol.openPort();

        smtpProtocol.ehlo(InetAddress.getLocalHost());

        smtpProtocol.mail(new Address("mail@sample.com"));

        // left out for test smtpProtocol.rcpt(new Address("mail@localhost"));

        try {
            smtpProtocol.data(MimeTreeRenderer.getInstance().renderMimePart(createMail()));
            fail("sending succeeded without recepient");
        } catch (Exception e) {
            // test succeeded
        }

        smtpProtocol.quit();

        // mail was propagated by SMTPServer
        assertNull("no mail received by mail server", m_mailServer.getLastMail());
    }

    public void testMultipleMailsAndRset() throws Exception, SMTPException {
        finishSetUp(m_testConfiguration);

        MySMTPProtocol smtpProtocol = new MySMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol.openPort();

        smtpProtocol.ehlo(InetAddress.getLocalHost());

        smtpProtocol.mail(new Address("mail@sample.com"));
        
        smtpProtocol.reset();
        
        smtpProtocol.mail(new Address("mail@sample.com"));

        smtpProtocol.quit();

        // mail was propagated by SMTPServer
        assertNull("no mail received by mail server", m_mailServer.getLastMail());
    }

    public void testRelayingDenied() throws Exception, SMTPException {
        m_testConfiguration.setAuthorizedAddresses("128.0.0.1/8");
        finishSetUp(m_testConfiguration);

        SMTPProtocol smtpProtocol = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol.openPort();

        smtpProtocol.ehlo(InetAddress.getLocalHost());

        smtpProtocol.mail(new Address("mail@sample.com"));
        try {
            smtpProtocol.rcpt(new Address("maila@sample.com"));
            fail("relaying allowed");
        } catch (SMTPException e) {
            assertEquals("expected 550 error", 550, e.getCode());
        }
    }

    public void testHandleAnnouncedMessageSizeLimitExceeded() throws Exception, SMTPException {
        m_testConfiguration.setMaxMessageSize(1); // set message limit to 1kb
        finishSetUp(m_testConfiguration);

        MySMTPProtocol smtpProtocol = new MySMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol.openPort();

        smtpProtocol.ehlo(InetAddress.getLocalHost());

        smtpProtocol.sendCommand("MAIL FROM:<mail@localhost> SIZE=1025", null);
        SMTPResponse response = smtpProtocol.getResponse();
        assertEquals("expected error: max msg size exceeded", 552, response.getCode());

        try {
            smtpProtocol.rcpt(new Address("mail@localhost"));
        } catch (SMTPException e) {
            assertEquals("expected error", 552, response.getCode());
        }
    }

    public void testHandleMessageSizeLimitExceeded() throws Exception, SMTPException {
        m_testConfiguration.setMaxMessageSize(1); // set message limit to 1kb 
        finishSetUp(m_testConfiguration);

        MySMTPProtocol smtpProtocol = new MySMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol.openPort();

        smtpProtocol.ehlo(InetAddress.getLocalHost());

        smtpProtocol.mail(new Address("mail@localhost"));
        smtpProtocol.rcpt(new Address("mail@localhost"));

        MimeHeader mimeHeader = new MimeHeader(new Header());
        mimeHeader.set("Mime-Version", "1.0");
        LocalMimePart mail = new LocalMimePart(mimeHeader);
        MimeHeader header = mail.getHeader();
        header.setMimeType(new MimeType("text", "plain"));

        // create Body with more than 1kb
        StringBuffer body = new StringBuffer();
        body.append("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        body.append("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        body.append("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        body.append("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        body.append("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        body.append("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        body.append("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        body.append("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        body.append("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        body.append("1234567810123456782012345678301234567840123456785012345678601234567870123456788012345678901234567100");
        body.append("1234567810123456782012345"); // 1025 chars

        mail.setBody(new CharSequenceSource(body.toString()));

        try {
            smtpProtocol.data(MimeTreeRenderer.getInstance().renderMimePart(mail));
            fail("message size exceeded not recognized");
        } catch (SMTPException e) {
            assertEquals("expected 552 error", 552, e.getCode());
        }

    }

    public void testConnectionLimitExceeded() throws Exception, SMTPException {
        m_testConfiguration.setConnectionLimit(1); // allow no more than one connection at a time 
        finishSetUp(m_testConfiguration);

        SMTPProtocol smtpProtocol1 = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        SMTPProtocol smtpProtocol2 = new SMTPProtocol("127.0.0.1", m_smtpListenerPort);
        smtpProtocol1.openPort();
        assertEquals("first connection taken", 1, smtpProtocol1.getState());

        smtpProtocol2.openPort();
        assertEquals("second connection not taken", SMTPProtocol.NOT_CONNECTED, smtpProtocol2.getState());
    }
    
    
}

class MySMTPProtocol extends SMTPProtocol
{

    public MySMTPProtocol(String s, int i) {
        super(s, i);
    }

    public MySMTPProtocol(String s) {
        super(s);
    }

    public void sendCommand(String string, String[] strings) throws IOException {
        super.sendCommand(string, strings);     
    }

    public SMTPResponse getResponse() throws IOException, SMTPException {
        return super.readSingleLineResponse();
    }
}
