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

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.socket.AvalonProtocolServer;
import org.apache.james.util.ConfigurationAdapter;

public class SMTPServerComposedTest extends SMTPServerTest {

    /*
    private SMTPServerComposed server;
    private AvalonProtocolServer pserver;
    protected void setUp() throws Exception {      
        server = new SMTPServerComposed();
        server.setAvalonProtocolServer(m_smtpServer);
        //ContainerUtil.enableLogging(m_smtpServer,new MockLogger());
        m_serviceManager = setUpServiceManager();
        pserver = new AvalonProtocolServer();
        pserver.setConnectionManager(connectionManager);
        pserver.setDNSService(m_dnsServer);
        pserver.setFileSystem(fileSystem);
        pserver.setLog(new SimpleLog("Log"));
        pserver.setProtocolHandlerFactory(server);
        pserver.setSocketManager(socketManager);
        pserver.setThreadManager(threadManager);
        //ContainerUtil.service(m_smtpServer, m_serviceManager);
        server.setLoader(m_serviceManager);
        server.setDNSService(m_dnsServer);
        server.setLog(new SimpleLog("MockLog"));
        server.setAvalonProtocolServer(pserver);
        m_testConfiguration = new SMTPTestConfiguration(m_smtpListenerPort);
    }

    protected void finishSetUp(SMTPTestConfiguration testConfiguration) throws Exception {
       testConfiguration.init();
        pserver.setConfiguration(new ConfigurationAdapter(testConfiguration));
        server.setConfiguration(new ConfigurationAdapter(testConfiguration));
        m_mailServer.setMaxMessageSizeBytes(m_testConfiguration.getMaxMessageSize()*1024);
        pserver.init();

        server.doInit();

    }
    */
}
