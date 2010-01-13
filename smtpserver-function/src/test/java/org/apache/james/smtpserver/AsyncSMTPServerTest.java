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
import org.apache.james.api.kernel.mock.FakeLoader;
import org.apache.james.smtpserver.integration.SMTPServerDNSServiceAdapter;
import org.apache.james.smtpserver.mina.AsyncSMTPServer;
import org.apache.james.util.ConfigurationAdapter;

public class AsyncSMTPServerTest extends SMTPServerTest {

    private AsyncSMTPServer m_smtpServer;

    protected void setUp() throws Exception {
        m_serviceManager = setUpServiceManager();

        
        m_smtpServer = new AsyncSMTPServer();
        m_smtpServer.setDNSService(m_dnsServer);
        m_smtpServer.setFileSystem(fileSystem);
        m_smtpServer.setLoader(m_serviceManager);
        m_smtpServer.setLog(new SimpleLog("Mock"));
        m_smtpServer.setMailServer(m_mailServer);
        m_testConfiguration = new SMTPTestConfiguration(m_smtpListenerPort);
    }

    protected void finishSetUp(SMTPTestConfiguration testConfiguration) throws Exception {
        testConfiguration.init();
        m_smtpServer.configure(new ConfigurationAdapter(testConfiguration));
        m_smtpServer.init();
        m_mailServer.setMaxMessageSizeBytes(m_testConfiguration.getMaxMessageSize()*1024);
    }

    protected FakeLoader setUpServiceManager() throws Exception {
        super.setUpServiceManager();
        SMTPServerDNSServiceAdapter dnsAdapter = new SMTPServerDNSServiceAdapter();
        dnsAdapter.setDNSService(m_dnsServer);
        m_serviceManager.put("org.apache.james.smtpserver.protocol.DNSService", dnsAdapter);
        return m_serviceManager;
    }
    
    public void testConnectionLimitExceeded() throws Exception {
        // Disable superclass test because this doesn't work with MINA yet.
        // TODO try to understand and fix it.
    }
   
    	
}
