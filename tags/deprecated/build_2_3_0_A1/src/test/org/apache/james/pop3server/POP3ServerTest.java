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
package org.apache.james.pop3server;

import org.apache.avalon.cornerstone.services.sockets.SocketManager;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;
import org.apache.james.core.MailImpl;
import org.apache.james.core.MimeMessageInputStreamSource;
import org.apache.james.core.MimeMessageWrapper;
import org.apache.james.services.JamesConnectionManager;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.avalon.MockSocketManager;
import org.apache.james.test.mock.avalon.MockThreadManager;
import org.apache.james.test.mock.james.MockMailRepository;
import org.apache.james.test.mock.james.MockMailServer;
import org.apache.james.test.mock.james.MockUsersRepository;
import org.apache.james.test.util.Util;
import org.apache.james.util.connection.SimpleConnectionManager;
import org.apache.mailet.MailAddress;
import org.columba.ristretto.io.Source;
import org.columba.ristretto.pop3.POP3Exception;
import org.columba.ristretto.pop3.POP3Protocol;
import org.columba.ristretto.pop3.POP3Response;
import org.columba.ristretto.pop3.ScanListEntry;

import com.sun.mail.util.SharedByteArrayInputStream;

import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * Tests the org.apache.james.smtpserver.SMTPServer unit
 */
public class POP3ServerTest extends TestCase {
    private int m_pop3ListenerPort = Util.getRandomNonPrivilegedPort();

    private MockMailServer m_mailServer;

    private POP3TestConfiguration m_testConfiguration;

    private POP3Server m_pop3Server;

    private MockUsersRepository m_usersRepository = new MockUsersRepository();

    public POP3ServerTest() {
        super("POP3ServerTest");
    }

    protected void setUp() throws Exception {
        m_pop3Server = new POP3Server();
        m_pop3Server.enableLogging(new MockLogger());

        m_pop3Server.service(setUpServiceManager());
        m_testConfiguration = new POP3TestConfiguration(m_pop3ListenerPort);
    }

    private void finishSetUp(POP3TestConfiguration testConfiguration)
            throws Exception {
        testConfiguration.init();
        m_pop3Server.configure(testConfiguration);
        m_pop3Server.initialize();
    }

    private MockServiceManager setUpServiceManager() {
        MockServiceManager serviceManager = new MockServiceManager();
        SimpleConnectionManager connectionManager = new SimpleConnectionManager();
        connectionManager.enableLogging(new MockLogger());
        serviceManager.put(JamesConnectionManager.ROLE, connectionManager);
        m_mailServer = new MockMailServer();
        serviceManager
                .put("org.apache.james.services.MailServer", m_mailServer);
        serviceManager.put("org.apache.james.services.UsersRepository",
                m_usersRepository);
        serviceManager.put(SocketManager.ROLE, new MockSocketManager(
                m_pop3ListenerPort));
        serviceManager.put(ThreadManager.ROLE, new MockThreadManager());
        return serviceManager;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        m_pop3Server.dispose();
    }

    public void testAuthenticationFail() throws Exception, POP3Exception {
        finishSetUp(m_testConfiguration);

        POP3Protocol pop3Protocol = new POP3Protocol("127.0.0.1",
                m_pop3ListenerPort);
        pop3Protocol.openPort();

        m_usersRepository.addUser("known", "test2");

        int res = 0;
        try {
            pop3Protocol.userPass("known", "test".toCharArray());
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        }

        assertEquals(-1, res);

        pop3Protocol.quit();
    }

    public void testUnknownUser() throws Exception, POP3Exception {
        finishSetUp(m_testConfiguration);

        POP3Protocol pop3Protocol = new POP3Protocol("127.0.0.1",
                m_pop3ListenerPort);
        pop3Protocol.openPort();

        int res = 0;
        try {
            pop3Protocol.userPass("unknown", "test".toCharArray());
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        }

        assertEquals(-1, res);

        pop3Protocol.quit();
    }

    public void testKnownUserEmptyInbox() throws Exception, POP3Exception {
        finishSetUp(m_testConfiguration);

        POP3Protocol pop3Protocol = new POP3Protocol("127.0.0.1",
                m_pop3ListenerPort);
        pop3Protocol.openPort();

        m_usersRepository.addUser("foo", "bar");

        int res = 0;
        try {
            pop3Protocol.userPass("foo", "bar".toCharArray());
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        }

        assertEquals(0, res);

        res = 0;
        ScanListEntry[] entries = null;
        try {
            entries = pop3Protocol.list();
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        }

        assertNotNull(entries);
        assertEquals(entries.length, 0);
        assertEquals(res, 0);

        pop3Protocol.quit();
    }

    public void testNotAsciiCharsInPassword() throws Exception, POP3Exception {
        finishSetUp(m_testConfiguration);

        POP3Protocol pop3Protocol = new POP3Protocol("127.0.0.1",
                m_pop3ListenerPort);
        pop3Protocol.openPort();

        String pass = "bar" + (new String(new char[] { 200, 210 })) + "foo";
        m_usersRepository.addUser("foo", pass);

        int res = 0;
        try {
            pop3Protocol.userPass("foo", pass.toCharArray());
        } catch (POP3Exception e) {
            res = e.getResponse() != null ? e.getResponse().getType() : -1;
        }

        assertEquals(0, res);

        pop3Protocol.quit();
    }

    public void testKnownUserInboxWithMessages() throws Exception,
            POP3Exception {
        finishSetUp(m_testConfiguration);

        POP3Protocol pop3Protocol = new POP3Protocol("127.0.0.1",
                m_pop3ListenerPort);
        pop3Protocol.openPort();

        m_usersRepository.addUser("foo2", "bar2");
        MockMailRepository mailRep = new MockMailRepository();

        setupTestMails(mailRep);

        m_mailServer.setUserInbox("foo2", mailRep);

        int res = 0;
        try {
            pop3Protocol.userPass("foo2", "bar2".toCharArray());
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        }

        assertEquals(0, res);

        res = 0;
        ScanListEntry[] entries = null;
        try {
            entries = pop3Protocol.list();
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        }

        assertNotNull(entries);
        assertEquals(2, entries.length);
        assertEquals(res, 0);

        Source i = null;
        try {
            i = pop3Protocol.top(entries[0].getIndex(), 0);
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        }

        assertNotNull(i);
        i.close();

        InputStream i2 = null;
        try {
            i2 = pop3Protocol.retr(entries[0].getIndex());
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        }

        assertNotNull(i2);
        
        i2.close();

        boolean deleted = false;
        try {
            deleted = pop3Protocol.dele(entries[0].getIndex());
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        }

        assertTrue(deleted);
        assertEquals(res, 0);

        pop3Protocol.quit();

        pop3Protocol.openPort();

        res = 0;
        try {
            pop3Protocol.userPass("foo2", "bar2".toCharArray());
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        }

        assertEquals(0, res);

        res = 0;
        entries = null;
        try {
            entries = pop3Protocol.list();
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        } finally {
            assertNotNull(entries);
            assertEquals(1, entries.length);
            assertEquals(res, 0);
        }

        i = null;
        try {
            i = pop3Protocol.top(entries[0].getIndex(), 0);
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        } finally {
            assertNotNull(i);
            i.close();
        }
        pop3Protocol.quit();
    }

    private void setupTestMails(MockMailRepository mailRep) throws MessagingException {
        ArrayList recipients = new ArrayList();
        recipients.add(new MailAddress("recipient@test.com"));
        MimeMessage mw = new MimeMessageWrapper(
                new MimeMessageInputStreamSource(
                        "test",
                        new SharedByteArrayInputStream(
                                ("Return-path: return@test.com\r\n"+
                                 "Content-Transfer-Encoding: plain\r\n"+
                                 "Subject: test\r\n\r\n"+
                                 "Body Text\r\n").getBytes())));
        mailRep.store(new MailImpl("name", new MailAddress("from@test.com"),
                recipients, mw));
        MimeMessage mw2 = new MimeMessageWrapper(
                new MimeMessageInputStreamSource(
                        "test2",
                        new SharedByteArrayInputStream(
                                ("").getBytes())));
        mailRep.store(new MailImpl("name2", new MailAddress("from@test.com"),
                recipients, mw2));
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

        // open two connections
        POP3Protocol pop3Protocol1 = new POP3Protocol("127.0.0.1",
                m_pop3ListenerPort);
        POP3Protocol pop3Protocol2 = new POP3Protocol("127.0.0.1",
                m_pop3ListenerPort);
        pop3Protocol1.openPort();
        pop3Protocol2.openPort();

        assertEquals("first connection taken", 1, pop3Protocol1.getState());
        assertEquals("second connection taken", 1, pop3Protocol2.getState());

        // open two accounts
        int res = 0;
        try {
            pop3Protocol1.userPass("foo1", "bar1".toCharArray());
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        }

        try {
            pop3Protocol2.userPass("foo2", "bar2".toCharArray());
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        }

        ScanListEntry[] entries = null;
        try {
            entries = pop3Protocol1.list();
            assertEquals("foo1 has mails", 2, entries.length);
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        }

        try {
            entries = pop3Protocol2.list();
            assertEquals("foo2 has no mails", 0, entries.length);
        } catch (POP3Exception e) {
            res = e.getResponse().getType();
        }

        // put both to rest
        pop3Protocol1.quit();
        pop3Protocol2.quit();
    }

}

class MyPOP3Protocol extends POP3Protocol {

    public MyPOP3Protocol(String s, int i) {
        super(s, i);
    }

    public MyPOP3Protocol(String s) {
        super(s);
    }

    public void sendCommand(String string, String[] strings) throws IOException {
        super.sendCommand(string, strings);
    }

    public POP3Response getResponse() throws IOException, POP3Exception {
        return super.readSingleLineResponse();
    }
}
