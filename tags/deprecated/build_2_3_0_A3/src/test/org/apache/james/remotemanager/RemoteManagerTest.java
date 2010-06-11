/***********************************************************************
 * Copyright (c) 2000-2005 The Apache Software Foundation.             *
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


package org.apache.james.remotemanager;

import junit.framework.TestCase;
import org.apache.avalon.cornerstone.services.sockets.SocketManager;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;
import org.apache.commons.net.telnet.TelnetClient;
import org.apache.james.services.JamesConnectionManager;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.avalon.MockSocketManager;
import org.apache.james.test.mock.avalon.MockThreadManager;
import org.apache.james.test.mock.james.MockMailServer;
import org.apache.james.test.mock.james.MockUsersStore;
import org.apache.james.test.util.Util;
import org.apache.james.userrepository.MockUsersRepository;
import org.apache.james.util.connection.SimpleConnectionManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 * Tests the org.apache.james.remotemanager.RemoteManager
 * TODO: impl missing command tests for: 
 *       USER 
 *       SHUTDOWN (hard to test, because it does shutdown the whole (testing) JVM 
 */

public class RemoteManagerTest extends TestCase {
    
    public static final String LINE_SEPARATOR = System.getProperties().getProperty("line.separator"); 

    protected int m_remoteManagerListenerPort = Util.getNonPrivilegedPort();
    protected RemoteManager m_remoteManager;
    protected RemoteManagerTestConfiguration m_testConfiguration;
    protected String m_host = "127.0.0.1";
    protected BufferedReader m_reader;
    protected OutputStreamWriter m_writer;
    protected TelnetClient m_telnetClient;
    private MockUsersRepository m_mockUsersRepository;

    protected void setUp() throws Exception {
        m_remoteManager = new RemoteManager();
        m_remoteManager.enableLogging(new MockLogger());

        m_remoteManager.service(setUpServiceManager());
        m_testConfiguration = new RemoteManagerTestConfiguration(m_remoteManagerListenerPort);
    }

    protected void finishSetUp(RemoteManagerTestConfiguration testConfiguration) {
        testConfiguration.init();
        try {
            m_remoteManager.configure(testConfiguration);
            m_remoteManager.initialize();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void login() throws IOException {
        login(m_testConfiguration.getLoginName(), m_testConfiguration.getLoginPassword());
    }

    protected void login(String name, String password) throws IOException {
        sendCommand(name);
        sendCommand(password);

        List answers = readAnswer();
        String last = getLastLine(answers);
        assertTrue(last.startsWith("Welcome"));
    }

    protected String getLastLine(List list) {
        if (list == null || list.isEmpty()) return null;
        return (String)list.get(list.size()-1);
    }

    protected List readAnswer() {
        try {
            while (!m_reader.ready()) { ; }
        } catch (IOException e) {
            return null;
        }

        StringBuffer stringBuffer = new StringBuffer();
        char[] charBuffer = new char[100];
        List allAnswerLines = new ArrayList();
        try {
            int readCount;
            while ((m_reader.ready() && (readCount = m_reader.read(charBuffer)) > 0)) {
                stringBuffer.append(charBuffer, 0, readCount);
            }
        } catch (IOException e) {
            fail("reading remote manager answer failed");
        }

        allAnswerLines.addAll(Arrays.asList(stringBuffer.toString().split(LINE_SEPARATOR)));
        if ("".equals(getLastLine(allAnswerLines))) allAnswerLines.remove(allAnswerLines.size()-1);
        return allAnswerLines;
    }

    protected void sendCommand(String command) throws IOException {
        m_writer.write(command + LINE_SEPARATOR);
        m_writer.flush();
    }

    protected void connect() throws IOException {
        m_telnetClient = new TelnetClient();
        m_telnetClient.connect(m_host, m_remoteManagerListenerPort);

        m_reader = new BufferedReader(new InputStreamReader(m_telnetClient.getInputStream()));
        m_writer = new OutputStreamWriter(m_telnetClient.getOutputStream());
    }

    private MockServiceManager setUpServiceManager() {
        MockServiceManager serviceManager = new MockServiceManager();
        SimpleConnectionManager connectionManager = new SimpleConnectionManager();
        connectionManager.enableLogging(new MockLogger());
        serviceManager.put(JamesConnectionManager.ROLE, connectionManager);
        MockMailServer mailServer = new MockMailServer();
        serviceManager.put(MailServer.ROLE, mailServer);
        m_mockUsersRepository = mailServer.getUsersRepository();
        serviceManager.put(UsersRepository.ROLE, m_mockUsersRepository);
        serviceManager.put(UsersStore.ROLE, new MockUsersStore(m_mockUsersRepository));
        serviceManager.put(SocketManager.ROLE, new MockSocketManager(m_remoteManagerListenerPort));
        serviceManager.put(ThreadManager.ROLE, new MockThreadManager());
        return serviceManager;
    }

    public void testLogin() throws IOException {
        finishSetUp(m_testConfiguration);
        connect();

        login();
    }

    public void testWrongLoginUser() throws IOException {
        finishSetUp(m_testConfiguration);
        connect();

        sendCommand("sindbad");
        sendCommand(m_testConfiguration.getLoginPassword());

        List answers = readAnswer();
        String last = getLastLine(answers);
        assertTrue(last.startsWith("Login id:")); // login failed, getting new login prompt
    }

    public void testWrongLoginPassword() throws IOException {
        finishSetUp(m_testConfiguration);
        connect();

        sendCommand(m_testConfiguration.getLoginName());
        sendCommand("getmethru");

        List answers = readAnswer();
        String last = getLastLine(answers);
        assertTrue(last.startsWith("Login id:")); // login failed, getting new login prompt
    }

    public void testUserCount() throws IOException {
        finishSetUp(m_testConfiguration);
        connect();
        login();

        sendCommand("countusers");
        assertTrue(getLastLine(readAnswer()).endsWith(" 0")); // no user yet

        sendCommand("adduser testCount1 testCount");
        assertTrue(getLastLine(readAnswer()).endsWith(" added")); // success

        sendCommand("countusers");
        assertTrue(getLastLine(readAnswer()).endsWith(" 1")); // 1 total

        sendCommand("adduser testCount2 testCount");
        assertTrue(getLastLine(readAnswer()).endsWith(" added")); // success

        sendCommand("countusers");
        assertTrue(getLastLine(readAnswer()).endsWith(" 2")); // 2 total

        m_mockUsersRepository.removeUser("testCount1");

        sendCommand("countusers");
        assertTrue(getLastLine(readAnswer()).endsWith(" 1")); // 1 total
    }

    public void testAddUserAndVerify() throws IOException {
        finishSetUp(m_testConfiguration);
        connect();
        login();

        sendCommand("adduser testAdd test");
        assertTrue(getLastLine(readAnswer()).endsWith(" added")); // success

        sendCommand("verify testNotAdded");
        assertTrue(getLastLine(readAnswer()).endsWith(" does not exist"));

        sendCommand("verify testAdd");
        assertTrue(getLastLine(readAnswer()).endsWith(" exists"));

        sendCommand("deluser testAdd");
        readAnswer();

        sendCommand("verify testAdd");
        assertTrue(getLastLine(readAnswer()).endsWith(" does not exist"));
    }

    public void testDelUser() throws IOException {
        finishSetUp(m_testConfiguration);
        connect();
        login();

        sendCommand("adduser testDel test");
        assertTrue(getLastLine(readAnswer()).endsWith(" added")); // success

        sendCommand("deluser testNotDeletable");
        assertTrue(getLastLine(readAnswer()).endsWith(" doesn't exist"));

        sendCommand("verify testDel");
        assertTrue(getLastLine(readAnswer()).endsWith(" exists"));

        sendCommand("deluser testDel");
        assertTrue(getLastLine(readAnswer()).endsWith(" deleted"));

        sendCommand("verify testDel");
        assertTrue(getLastLine(readAnswer()).endsWith(" does not exist"));
    }

    public void testQuit() throws IOException {
        finishSetUp(m_testConfiguration);
        connect();
        login();

        sendCommand("help");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            ; // ignore
        }
        assertTrue("command line is effective", readAnswer().size() > 0);

        sendCommand("quit");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            ; // ignore
        }
        readAnswer();

        sendCommand("help");
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            ; // ignore
        }
        assertNull("connection is closed", m_reader.readLine());
    }   

    public void testListUsers() throws IOException {
        finishSetUp(m_testConfiguration);
        connect();
        login();

        String[] users = new String[] {"ccc", "aaa", "dddd", "bbbbb"};

        for (int i = 0; i < users.length; i++) {
            String user = users[i];
            sendCommand("adduser " + user + " test");
        }
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            ; // ignore
        }
        readAnswer();

        sendCommand("listusers");
        List list = readAnswer();

        assertEquals("user count line", "Existing accounts " + users.length, list.get(0));
        
        List readUserNames = new ArrayList();
        for (Iterator iterator = list.iterator(); iterator.hasNext();) {
            String answerLine = (String) iterator.next();
            if (!answerLine.startsWith("user: ")) continue;
            readUserNames.add(answerLine.substring(6));
        }
        assertEquals("user count", users.length, readUserNames.size());

        for (int i = 0; i < users.length; i++) {
            String user = users[i];
            assertTrue("name found", readUserNames.contains(user));
        }
    }

    public void testCommandCaseInsensitive() throws IOException {
        finishSetUp(m_testConfiguration);
        connect();
        login();

        sendCommand("adduser testDel test");
        assertTrue(getLastLine(readAnswer()).endsWith(" added")); // success

        sendCommand("verify testDel");
        assertTrue(getLastLine(readAnswer()).endsWith(" exists"));

        sendCommand("VERIFY testDel");
        assertTrue(getLastLine(readAnswer()).endsWith(" exists"));

        sendCommand("vErIfY testDel");
        assertTrue(getLastLine(readAnswer()).endsWith(" exists"));
    }

    public void testParameterCaseSensitive() throws IOException {
        finishSetUp(m_testConfiguration);
        connect();
        login();

        sendCommand("adduser testDel test");
        assertTrue(getLastLine(readAnswer()).endsWith(" added")); // success

        sendCommand("verify testDel");
        assertTrue(getLastLine(readAnswer()).endsWith(" exists"));

        sendCommand("verify TESTDEL");
        assertTrue(getLastLine(readAnswer()).endsWith(" does not exist"));

        sendCommand("verify testdel");
        assertTrue(getLastLine(readAnswer()).endsWith(" does not exist"));
    }

    public void testAlias() throws IOException {
        m_mockUsersRepository.setForceUseJamesUser();
        finishSetUp(m_testConfiguration);
        connect();
        login();
        
        // do some tests when parameter users don't exist
        sendCommand("setalias testNonExist1 testNonExist2");
        assertTrue(getLastLine(readAnswer()).equals("No such user testNonExist1"));

        sendCommand("adduser testAlias1 test");
        assertTrue(getLastLine(readAnswer()).endsWith(" added"));

        sendCommand("showalias testAlias1");
        assertTrue(getLastLine(readAnswer()).equals("User testAlias1 does not currently have an alias"));

        sendCommand("setalias testAlias1 testNonExist2");
        assertTrue(getLastLine(readAnswer()).equals("Alias unknown to server - create that user first."));

        sendCommand("setalias testNonExist1 testAlias");
        assertTrue(getLastLine(readAnswer()).equals("No such user testNonExist1"));

        sendCommand("adduser testAlias2 test");
        assertTrue(getLastLine(readAnswer()).endsWith(" added"));

        // regular alias
        sendCommand("setalias testAlias1 testAlias2");
        assertTrue(getLastLine(readAnswer()).equals("Alias for testAlias1 set to:testAlias2"));

        //TODO: is this correct? even primitive circular aliasing allowed!
        sendCommand("setalias testAlias2 testAlias1");
        assertTrue(getLastLine(readAnswer()).equals("Alias for testAlias2 set to:testAlias1"));

        // did first one persist?
        sendCommand("showalias testAlias1");
        assertTrue(getLastLine(readAnswer()).equals("Current alias for testAlias1 is: testAlias2"));

        //TODO: is this correct? setting self as alias!
        sendCommand("setalias testAlias1 testAlias1");
        assertTrue(getLastLine(readAnswer()).equals("Alias for testAlias1 set to:testAlias1"));

        sendCommand("adduser testAlias3 test");
        assertTrue(getLastLine(readAnswer()).endsWith(" added"));

        // re-set, simply overwrites
        sendCommand("setalias testAlias1 testAlias3");
        assertTrue(getLastLine(readAnswer()).equals("Alias for testAlias1 set to:testAlias3"));

        // check overwrite
        sendCommand("showalias testAlias1");
        assertTrue(getLastLine(readAnswer()).equals("Current alias for testAlias1 is: testAlias3"));

        // retreat
        sendCommand("unsetalias testAlias1");
        assertTrue(getLastLine(readAnswer()).equals("Alias for testAlias1 unset"));

        // check removed alias
        sendCommand("showalias testAlias1");
        assertTrue(getLastLine(readAnswer()).equals("User testAlias1 does not currently have an alias"));

    }

    public void testForward() throws IOException {
        m_mockUsersRepository.setForceUseJamesUser();
        finishSetUp(m_testConfiguration);
        connect();
        login();

        // do some tests when parameter users don't exist
        sendCommand("setforwarding testNonExist1 testForward1@locahost");
        assertTrue(getLastLine(readAnswer()).equals("No such user testNonExist1"));
        
        sendCommand("adduser testForwardUser test");
        assertTrue(getLastLine(readAnswer()).endsWith(" added"));

        sendCommand("showforwarding testForwardUser");
        assertTrue(getLastLine(readAnswer()).equals("User testForwardUser is not currently being forwarded"));

        sendCommand("setforwarding testForwardUser testForward1@locahost");
        assertTrue(getLastLine(readAnswer()).equals("Forwarding destination for testForwardUser set to:testForward1@locahost"));
        
        // did it persist?
        sendCommand("showforwarding testForwardUser");
        assertTrue(getLastLine(readAnswer()).equals("Current forwarding destination for testForwardUser is: testForward1@locahost"));

        // re-set, simply overwrites
        sendCommand("setforwarding testForwardUser testForward2@locahost");
        assertTrue(getLastLine(readAnswer()).equals("Forwarding destination for testForwardUser set to:testForward2@locahost"));

        // check overwrite
        sendCommand("showforwarding testForwardUser");
        assertTrue(getLastLine(readAnswer()).equals("Current forwarding destination for testForwardUser is: testForward2@locahost"));

        // retreat
        sendCommand("unsetforwarding testForwardUser");
        assertTrue(getLastLine(readAnswer()).equals("Forward for testForwardUser unset"));

        // check removed forward
        sendCommand("showforwarding testForwardUser");
        assertTrue(getLastLine(readAnswer()).equals("User testForwardUser is not currently being forwarded"));

    }

    public void testSetPassword() throws IOException {
        finishSetUp(m_testConfiguration);
        connect();
        login();

        sendCommand("adduser testPwdUser pwd1");
        assertTrue(getLastLine(readAnswer()).endsWith(" added"));

        assertTrue("initial password", m_mockUsersRepository.test("testPwdUser", "pwd1"));
        
         sendCommand("setpassword testPwdUser     ");
        assertTrue("password changed to empty", m_mockUsersRepository.test("testPwdUser", "pwd1"));
        readAnswer(); // ignore

        // change pwd
        sendCommand("setpassword testPwdUser pwd2");
        assertTrue("password not changed to pwd2", m_mockUsersRepository.test("testPwdUser", "pwd2"));
        readAnswer(); // ignore
        
        // assure case sensitivity
        sendCommand("setpassword testPwdUser pWD2");
        assertFalse("password not changed to pWD2", m_mockUsersRepository.test("testPwdUser", "pwd2"));
        assertTrue("password not changed to pWD2", m_mockUsersRepository.test("testPwdUser", "pWD2"));
        readAnswer(); // ignore
        
    }
}
