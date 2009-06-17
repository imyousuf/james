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



package org.apache.james.remotemanager;

import org.apache.avalon.cornerstone.services.sockets.SocketManager;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.service.ServiceException;
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
import org.apache.james.util.InternetPrintWriter;
import org.apache.james.util.connection.SimpleConnectionManager;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.TestCase;

/**
 * Tests the org.apache.james.remotemanager.RemoteManager
 * TODO: impl missing command tests for: 
 *       USER 
 *       SHUTDOWN (hard to test, because it does shutdown the whole (testing) JVM 
 */

public class RemoteManagerTest extends TestCase {
    
    protected int m_remoteManagerListenerPort = Util.getNonPrivilegedPort();
    protected RemoteManager m_remoteManager;
    protected RemoteManagerTestConfiguration m_testConfiguration;
    protected String m_host = "127.0.0.1";
    protected BufferedReader m_reader;
    protected InternetPrintWriter m_writer;
    protected TelnetClient m_telnetClient;
    private MockUsersRepository m_mockUsersRepository;
    private MockMailServer mailServer;

    protected void setUp() throws Exception {
        m_remoteManager = new RemoteManager();
        ContainerUtil.enableLogging(m_remoteManager, new MockLogger());
        ContainerUtil.service(m_remoteManager, setUpServiceManager());
        m_testConfiguration = new RemoteManagerTestConfiguration(m_remoteManagerListenerPort);
    }

    protected void tearDown() throws Exception {
        ContainerUtil.dispose(mailServer);
        super.tearDown();
    }

    protected void finishSetUp(RemoteManagerTestConfiguration testConfiguration) {
        testConfiguration.init();
        try {
            ContainerUtil.configure(m_remoteManager, testConfiguration);
            ContainerUtil.initialize(m_remoteManager);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void login() throws IOException {
        login(m_testConfiguration.getLoginName(), m_testConfiguration.getLoginPassword());
    }

    protected void login(String name, String password) throws IOException {
        sendCommand(name);
        List answers = readAnswer();
        String last = getLastLine(answers);
        assertTrue("Last line does not start with Password: "+last,last.startsWith("Password:"));
        sendCommand(password);
        answers = readAnswer();
        last = getLastLine(answers);
        assertTrue("Last line does not start with Welcome: "+last,last.startsWith("Welcome"));
    }

    protected String getLastLine(List list) {
        if (list == null || list.isEmpty()) return null;
        return (String)list.get(list.size()-1);
    }

    protected List readAnswer() {
        return readAnswer(1);
    }
    
    protected List readAnswer(int numLines) {
        List allAnswerLines = new ArrayList();
        try {
            if (numLines > 0) {
                for (int i = 0; i < numLines; i++) {
                    allAnswerLines.add(m_reader.readLine());
                }
            } else {
                String line = m_reader.readLine();
                allAnswerLines.add(line);
                
                while (m_reader.ready()) {
                    allAnswerLines.add(m_reader.readLine());
                }
            }
            return allAnswerLines;
        } catch (IOException e) {
            return null;
        }
    }

    protected void sendCommand(String command) throws IOException {
        m_writer.println(command);
        m_writer.flush();
    }

    protected void connect() throws IOException {
        m_telnetClient = new TelnetClient();
        m_telnetClient.connect(m_host, m_remoteManagerListenerPort);

        m_reader = new BufferedReader(new InputStreamReader(new BufferedInputStream(m_telnetClient.getInputStream(), 1024), "ASCII"));
        m_writer = new InternetPrintWriter(new BufferedOutputStream(m_telnetClient.getOutputStream(), 1024), true);
        
        readAnswer(3);
    }

    private MockServiceManager setUpServiceManager() throws ServiceException {
        MockServiceManager serviceManager = new MockServiceManager();
        SimpleConnectionManager connectionManager = new SimpleConnectionManager();
        ContainerUtil.enableLogging(connectionManager, new MockLogger());
        serviceManager.put(JamesConnectionManager.ROLE, connectionManager);
        mailServer = new MockMailServer();
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
        List answers = readAnswer();
        sendCommand(m_testConfiguration.getLoginPassword());

        // we should receive the fail message and a new Login id.
        answers = readAnswer(2);
        String last = getLastLine(answers);
        assertTrue("Last line does not start with 'Login id:' but with '"+last+"'",last.startsWith("Login id:")); // login failed, getting new login prompt
    }

    public void testWrongLoginPassword() throws IOException {
        finishSetUp(m_testConfiguration);
        connect();

        sendCommand(m_testConfiguration.getLoginName());
        List answers = readAnswer();
        sendCommand("getmethru");

        answers = readAnswer(2);
        String last = getLastLine(answers);
        assertTrue("Line does not start with 'Login id:' but with '"+last+"'", last.startsWith("Login id:")); // login failed, getting new login prompt
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
        int helpLines = 16;
    
        finishSetUp(m_testConfiguration);
        connect();
        login();

        sendCommand("help");
        delay();
        assertTrue("command line is effective", readAnswer().size() > 0);
        readAnswer(helpLines);
        
        sendCommand("quit");
        delay();
        assertTrue("",readAnswer(1).contains("Bye"));

        sendCommand("help");
        delay();
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
            readAnswer(1);
        }

        delay();

        sendCommand("listusers");
        List list = readAnswer(5);

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

    private void delay() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            ; // ignore
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

        String lastLine;
        
        sendCommand("adduser testPwdUser pwd1");
        lastLine = getLastLine(readAnswer());
        assertTrue(lastLine.endsWith(" added"));

        assertTrue("initial password", m_mockUsersRepository.test("testPwdUser", "pwd1"));
        
        sendCommand("setpassword testPwdUser     ");
        lastLine = getLastLine(readAnswer());
        assertTrue("password changed to empty: "+lastLine, m_mockUsersRepository.test("testPwdUser", "pwd1"));

        // change pwd
        sendCommand("setpassword testPwdUser pwd2");
        lastLine = getLastLine(readAnswer());
        assertTrue("password not changed to pwd2: "+lastLine, m_mockUsersRepository.test("testPwdUser", "pwd2"));
        
        // assure case sensitivity
        sendCommand("setpassword testPwdUser pWD2");
        lastLine = getLastLine(readAnswer());
        assertFalse("password not changed to pWD2: "+lastLine, m_mockUsersRepository.test("testPwdUser", "pwd2"));
        assertTrue("password not changed to pWD2: "+lastLine, m_mockUsersRepository.test("testPwdUser", "pWD2"));
        
    }
    
}
