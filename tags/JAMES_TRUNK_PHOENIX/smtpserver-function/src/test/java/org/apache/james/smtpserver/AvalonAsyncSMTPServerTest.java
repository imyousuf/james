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
import org.apache.james.api.kernel.mock.FakeLoader;
import org.apache.james.smtpserver.mina.AvalonAsyncSMTPServer;
import org.apache.james.test.mock.avalon.MockLogger;

public class AvalonAsyncSMTPServerTest extends AsyncSMTPServerTest{

    private AvalonAsyncSMTPServer smtpserver; 
    
    @Override
    protected void finishSetUp(SMTPTestConfiguration testConfiguration)
            throws Exception {
        testConfiguration.init();
        ContainerUtil.configure(smtpserver, testConfiguration);
        ContainerUtil.initialize(smtpserver);
        m_mailServer.setMaxMessageSizeBytes(m_testConfiguration.getMaxMessageSize()*1024);

    }

    @Override
    protected void setUp() throws Exception {
        FakeLoader loader = setUpServiceManager();
        smtpserver = new AvalonAsyncSMTPServer();
        ContainerUtil.enableLogging(smtpserver, new MockLogger());
        ContainerUtil.service(smtpserver, loader);
        m_testConfiguration = new SMTPTestConfiguration(m_smtpListenerPort);

    }
    
    
}
