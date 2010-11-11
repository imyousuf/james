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
import org.apache.james.remotemanager.netty.RemoteManager;

public class RemoteManagerTest extends AbstractRemoteManagerTest{
    private RemoteManager remotemanager;

    @Override
    protected void initRemoteManager(RemoteManagerTestConfiguration testConfiguration) throws Exception {
        remotemanager.configure(testConfiguration);
        remotemanager.init();
    }

    @Override
    protected void setUpRemoteManager() throws Exception {
        
        remotemanager = new RemoteManager();
        remotemanager.setDNSService(dnsservice);
        remotemanager.setFileSystem(filesystem);
        remotemanager.setProtocolHandlerChain(chain);
        SimpleLog log = new SimpleLog("Mock");
        log.setLevel(SimpleLog.LOG_LEVEL_DEBUG);
        remotemanager.setLog(log);
        remotemanager.setMailServer(mailServer);
               
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        remotemanager.destroy();
    }

}
