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
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.avalon.MockSocketManager;
import org.apache.james.test.mock.avalon.MockThreadManager;
import org.apache.james.test.mock.james.MockMailServer;
import org.apache.james.test.mock.james.MockUsersRepository;
import org.apache.james.test.mock.james.MockUsersStore;
import org.apache.james.test.util.Util;
import org.apache.james.util.connection.SimpleConnectionManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

/**
 * Tests the org.apache.james.remotemanager.RemoteManager 
 */
public class RemoteManagerTest extends TestCase {

    protected int m_remoteManagerListenerPort = Util.getRandomNonPrivilegedPort();
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
        sendCommand(m_testConfiguration.getLoginName());
        sendCommand(m_testConfiguration.getLoginPassword());

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

        allAnswerLines.addAll(Arrays.asList(stringBuffer.toString().split("\n")));
        if ("".equals(getLastLine(allAnswerLines))) allAnswerLines.remove(allAnswerLines.size()-1);
        return allAnswerLines;
    }

    protected void sendCommand(String command) throws IOException {
        m_writer.write(command + "\n");
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
        serviceManager.put("org.apache.james.services.MailServer", mailServer);
        m_mockUsersRepository = mailServer.getUsersRepository();
        serviceManager.put("org.apache.james.services.UsersRepository", m_mockUsersRepository);
        serviceManager.put("org.apache.james.services.UsersStore", new MockUsersStore(m_mockUsersRepository));
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
}
