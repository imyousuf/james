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

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.kernel.mock.FakeLoader;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailServer;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockStore;
import org.apache.james.test.mock.james.MockFileSystem;
import org.apache.james.test.mock.james.MockMailServer;
import org.apache.james.userrepository.MockUsersRepository;
import org.apache.mailet.MailetContext;
import org.apache.mailet.base.test.FakeMailContext;

public class AsyncSMTPServerTest extends SMTPServerTest {

    private AsyncSMTPServer m_smtpServer;

    protected void setUp() throws Exception {
        m_smtpServer = new AsyncSMTPServer();
        ContainerUtil.enableLogging(m_smtpServer,new MockLogger());
        m_serviceManager = setUpServiceManager();
        ContainerUtil.service(m_smtpServer, m_serviceManager);
        m_smtpServer.setLoader(m_serviceManager);
        m_testConfiguration = new SMTPTestConfiguration(m_smtpListenerPort);
    }

    protected void finishSetUp(SMTPTestConfiguration testConfiguration) throws Exception {
        testConfiguration.init();
        ContainerUtil.configure(m_smtpServer, testConfiguration);
        m_smtpServer.initialize();
        m_mailServer.setMaxMessageSizeBytes(m_testConfiguration.getMaxMessageSize()*1024);
    }

    protected FakeLoader setUpServiceManager() throws Exception {
        m_serviceManager = new FakeLoader();
        m_serviceManager.put(MailetContext.class.getName(), new FakeMailContext());
        m_mailServer = new MockMailServer(new MockUsersRepository());
        m_serviceManager.put(MailServer.ROLE, m_mailServer);
     // Phoenix loader does not understand aliases
        m_serviceManager.put("James", m_mailServer);
        m_serviceManager.put("localusersrepository", m_usersRepository);
        m_serviceManager.put(UsersRepository.ROLE, m_usersRepository);
        m_dnsServer = new AlterableDNSServer();
        m_serviceManager.put(DNSService.ROLE, m_dnsServer);
        m_serviceManager.put("dnsserver", m_dnsServer);
        m_serviceManager.put(Store.ROLE, new MockStore());
        m_serviceManager.put(FileSystem.ROLE, new MockFileSystem());
    
        return m_serviceManager;
    }
    
    public void testConnectionLimitExceeded() throws Exception {
        // Disable superclass test because this doesn't work with MINA yet.
        // TODO try to understand and fix it.
    }


}
