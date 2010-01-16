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

import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.remotemanager.mina.AsyncRemoteManager;
import org.apache.james.util.ConfigurationAdapter;

public class AsyncRemoteManagerTest extends RemoteManagerTest{


    private AsyncRemoteManager remotemanager;

    protected void setUp() throws Exception {
        setUpServiceManager();

        remotemanager = new AsyncRemoteManager();
        remotemanager.setDNSService(dnsservice);
        remotemanager.setFileSystem(filesystem);
        remotemanager.setLoader(serviceManager);
        SimpleLog log = new SimpleLog("Mock");
        log.setLevel(SimpleLog.LOG_LEVEL_DEBUG);
        remotemanager.setLog(log);
        remotemanager.setMailServer(mailServer);
        m_testConfiguration = new RemoteManagerTestConfiguration(m_remoteManagerListenerPort);
    }

    protected void finishSetUp(RemoteManagerTestConfiguration testConfiguration) throws Exception {
        testConfiguration.init();
        remotemanager.configure(new ConfigurationAdapter(testConfiguration));
        remotemanager.init();
    }

}
