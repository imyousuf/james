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
package org.apache.james.pop3server;

import org.apache.avalon.cornerstone.services.sockets.SocketManager;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.apache.james.core.MailImpl;
import org.apache.james.core.MimeMessageCopyOnWriteProxy;
import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.services.JamesConnectionManager;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.avalon.MockSocketManager;
import org.apache.james.test.mock.avalon.MockThreadManager;
import org.apache.james.test.mock.james.MockMailRepository;
import org.apache.james.test.mock.james.MockMailServer;
import org.apache.james.test.util.Util;
import org.apache.james.userrepository.MockUsersRepository;
import org.apache.james.util.connection.SimpleConnectionManager;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import java.io.Reader;
import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * Tests the org.apache.james.smtpserver.SMTPServer unit
 */
public class POP3ServerTest extends TestCase {
    private int m_pop3ListenerPort = Util.getNonPrivilegedPort();

    private MockMailServer m_mailServer;

    private POP3TestConfiguration m_testConfiguration;

    private POP3Server m_pop3Server;

    private MockUsersRepository m_usersRepository = new MockUsersRepository();
    private POP3Client m_pop3Protocol = null;

    public POP3ServerTest() {
        super("POP3ServerTest");
    }

    protected void setUp() throws Exception {
        m_pop3Server = new POP3Server();
        ContainerUtil.enableLogging(m_pop3Server, new MockLogger());
        ContainerUtil.service(m_pop3Server, setUpServiceManager());
        m_testConfiguration = new POP3TestConfiguration(m_pop3ListenerPort);
    }

    private void finishSetUp(POP3TestConfiguration testConfiguration)
            throws Exception {
        testConfiguration.init();
        ContainerUtil.configure(m_pop3Server, testConfiguration);
        ContainerUtil.initialize(m_pop3Server);
    }

    private MockServiceManager setUpServiceManager() {
        MockServiceManager serviceManager = new MockServiceManager();
        SimpleConnectionManager connectionManager = new SimpleConnectionManager();
        ContainerUtil.enableLogging(connectionManager, new MockLogger());
        serviceManager.put(JamesConnectionManager.ROLE, connectionManager);
        m_mailServer = new MockMailServer();
        serviceManager
                .put(MailServer.ROLE, m_mailServer);
        serviceManager.put(UsersRepository.ROLE,
                m_usersRepository);
        serviceManager.put(SocketManager.ROLE, new MockSocketManager(
                m_pop3ListenerPort));
        serviceManager.put(ThreadManager.ROLE, new MockThreadManager());
        return serviceManager;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        if (m_pop3Protocol != null) {
            m_pop3Protocol.sendCommand("quit");
            m_pop3Protocol.disconnect();
        }
        m_pop3Server.dispose();
    }

    public void testAuthenticationFail() throws Exception {
        finishSetUp(m_testConfiguration);
        
        m_pop3Protocol = new POP3Client();
        m_pop3Protocol.connect("127.0.0.1", m_pop3ListenerPort);

        m_usersRepository.addUser("known", "test2");

        m_pop3Protocol.login("known", "test");
        assertEquals(0, m_pop3Protocol.getState());
        assertTrue(m_pop3Protocol.getReplyString().startsWith("-ERR"));
    }

    public void testUnknownUser() throws Exception {
        finishSetUp(m_testConfiguration);

        m_pop3Protocol = new POP3Client();
        m_pop3Protocol.connect("127.0.0.1", m_pop3ListenerPort);

        m_pop3Protocol.login("unknown", "test");
        assertEquals(0, m_pop3Protocol.getState());
        assertTrue(m_pop3Protocol.getReplyString().startsWith("-ERR"));
    }

    public void testKnownUserEmptyInbox() throws Exception {
        finishSetUp(m_testConfiguration);

        m_pop3Protocol = new POP3Client();
        m_pop3Protocol.connect("127.0.0.1",m_pop3ListenerPort);

        m_usersRepository.addUser("foo", "bar");
        m_mailServer.setUserInbox("foo", new MockMailRepository());

        m_pop3Protocol.login("foo", "bar");
        System.err.println(m_pop3Protocol.getState());
        assertEquals(1, m_pop3Protocol.getState());

        POP3MessageInfo[] entries = m_pop3Protocol.listMessages();
        assertEquals(1, m_pop3Protocol.getState());

        assertNotNull(entries);
        assertEquals(entries.length, 0);
    }

    public void testNotAsciiCharsInPassword() throws Exception {
        finishSetUp(m_testConfiguration);

        m_pop3Protocol = new POP3Client();
        m_pop3Protocol.connect("127.0.0.1",m_pop3ListenerPort);

        String pass = "bar" + (new String(new char[] { 200, 210 })) + "foo";
        m_usersRepository.addUser("foo", pass);
        m_mailServer.setUserInbox("foo", new MockMailRepository());

        m_pop3Protocol.login("foo", pass);
        assertEquals(1, m_pop3Protocol.getState());
    }

    public void testKnownUserInboxWithMessages() throws Exception {
        finishSetUp(m_testConfiguration);

        m_pop3Protocol = new POP3Client();
        m_pop3Protocol.connect("127.0.0.1",m_pop3ListenerPort);

        m_usersRepository.addUser("foo2", "bar2");
        MockMailRepository mailRep = new MockMailRepository();

        setupTestMails(mailRep);

        m_mailServer.setUserInbox("foo2", mailRep);

        m_pop3Protocol.login("foo2", "bar2");
        assertEquals(1, m_pop3Protocol.getState());

        POP3MessageInfo[] entries = m_pop3Protocol.listMessages();

        assertNotNull(entries);
        assertEquals(2, entries.length);
        assertEquals(1, m_pop3Protocol.getState());

        Reader r = m_pop3Protocol.retrieveMessageTop(entries[0].number, 0);
        assertNotNull(r);
        r.close();

        Reader r2 = m_pop3Protocol.retrieveMessage(entries[0].number);
        assertNotNull(r2);
        r2.close();

        boolean deleted = m_pop3Protocol.deleteMessage(entries[0].number);

        assertTrue(deleted);
        assertEquals(1, m_pop3Protocol.getState());

        m_pop3Protocol.sendCommand("quit");
        m_pop3Protocol.disconnect();

        m_pop3Protocol.connect("127.0.0.1",m_pop3ListenerPort);

        m_pop3Protocol.login("foo2", "bar2");
        assertEquals(1, m_pop3Protocol.getState());

        entries = null;

        POP3MessageInfo stats = m_pop3Protocol.status();
        assertEquals(92, stats.size);
        assertEquals(1, stats.number);

        entries = m_pop3Protocol.listMessages();

        assertNotNull(entries);
        assertEquals(1, entries.length);
        assertEquals(1, m_pop3Protocol.getState());

        Reader r3 = m_pop3Protocol.retrieveMessageTop(entries[0].number, 0);
        assertNotNull(r3);
        r3.close();
    }

    private void setupTestMails(MockMailRepository mailRep) throws MessagingException {
        ArrayList recipients = new ArrayList();
        recipients.add(new MailAddress("recipient@test.com"));
        MimeMessage mw = new MimeMessageCopyOnWriteProxy(
                new MimeMessageInputStreamSource(
                        "test",
                        new SharedByteArrayInputStream(
                                ("Return-path: return@test.com\r\n"+
                                 "Content-Transfer-Encoding: plain\r\n"+
                                 "Subject: test\r\n\r\n"+
                                 "Body Text\r\n").getBytes())));
        MailImpl m = new MailImpl("name", new MailAddress("from@test.com"),
                recipients, mw);
        mailRep.store(m);
        MimeMessage mw2 = new MimeMessageCopyOnWriteProxy(
                new MimeMessageInputStreamSource(
                        "test2",
                        new SharedByteArrayInputStream(
                                ("").getBytes())));
        MailImpl mailimpl2 = new MailImpl("name2", new MailAddress("from@test.com"),
                        recipients, mw2);
        mailRep.store(mailimpl2);
        m.dispose();
        mailimpl2.dispose();
    }

    public void testTwoSimultaneousMails() throws Exception {
        finishSetUp(m_testConfiguration);

        // make two user/repositories, open both
        m_usersRepository.addUser("foo1", "bar1");
        MockMailRepository mailRep1 = new MockMailRepository();
        setupTestMails(mailRep1);
        m_mailServer.setUserInbox("foo1", mailRep1);

        m_usersRepository.addUser("foo2", "bar2");
        MockMailRepository mailRep2 = new MockMailRepository();
        //do not setupTestMails, this is done later
        m_mailServer.setUserInbox("foo2", mailRep2);

        POP3Client pop3Protocol2 = null;
        try {
            // open two connections
            m_pop3Protocol = new POP3Client();
            m_pop3Protocol.connect("127.0.0.1", m_pop3ListenerPort);
            pop3Protocol2 = new POP3Client();
            pop3Protocol2.connect("127.0.0.1", m_pop3ListenerPort);

            assertEquals("first connection taken", 0, m_pop3Protocol.getState());
            assertEquals("second connection taken", 0, pop3Protocol2.getState());

            // open two accounts
            m_pop3Protocol.login("foo1", "bar1");

            pop3Protocol2.login("foo2", "bar2");

            POP3MessageInfo[] entries = m_pop3Protocol.listMessages();
            assertEquals("foo1 has mails", 2, entries.length);

            entries = pop3Protocol2.listMessages();
            assertEquals("foo2 has no mails", 0, entries.length);

        } finally {
            // put both to rest, field var is handled by tearDown()
            if (pop3Protocol2 != null) {
                pop3Protocol2.sendCommand("quit");
                pop3Protocol2.disconnect();
            }
        }
    }

}
