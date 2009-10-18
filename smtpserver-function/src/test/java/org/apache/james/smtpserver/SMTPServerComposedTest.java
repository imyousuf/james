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

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.test.mock.avalon.MockLogger;

public class SMTPServerComposedTest extends SMTPServerTest {

    private SMTPServerComposed m_smtpServer;

    protected void setUp() throws Exception {
        m_smtpServer = new SMTPServerComposed();
        ContainerUtil.enableLogging(m_smtpServer,new MockLogger());
        m_serviceManager = setUpServiceManager();
        ContainerUtil.service(m_smtpServer, m_serviceManager);
        m_smtpServer.setLoader(m_serviceManager);
        m_testConfiguration = new SMTPTestConfiguration(m_smtpListenerPort);
    }

    protected void finishSetUp(SMTPTestConfiguration testConfiguration) throws Exception {
        testConfiguration.init();
        ContainerUtil.configure(m_smtpServer, testConfiguration);
        m_mailServer.setMaxMessageSizeBytes(m_testConfiguration.getMaxMessageSize()*1024);
        ContainerUtil.initialize(m_smtpServer);
    }
}
