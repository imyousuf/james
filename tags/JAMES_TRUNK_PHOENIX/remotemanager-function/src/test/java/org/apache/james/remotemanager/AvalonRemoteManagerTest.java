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

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.test.mock.avalon.MockLogger;

public class AvalonRemoteManagerTest extends RemoteManagerTest{

    private AvalonRemoteManager server;
    @Override
    protected void setUp() throws Exception {
        server = new AvalonRemoteManager();
        setUpServiceManager();
        ContainerUtil.enableLogging(server, new MockLogger());
        ContainerUtil.service(server, serviceManager);
        m_testConfiguration = new RemoteManagerTestConfiguration(m_remoteManagerListenerPort);
    }

    @Override
    protected void finishSetUp(RemoteManagerTestConfiguration testConfiguration)
            throws Exception {
        testConfiguration.init();
        ContainerUtil.configure(server, testConfiguration);
        ContainerUtil.initialize(server);
    }
}
