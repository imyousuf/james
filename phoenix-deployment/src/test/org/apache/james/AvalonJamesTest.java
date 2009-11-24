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

package org.apache.james;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.domainlist.DomainList;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.user.UsersStore;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailServer;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.userrepository.MockUsersRepository;

public class AvalonJamesTest extends JamesTest{

    public MailServer createMailServer() throws Exception {
        AvalonJames james = new AvalonJames();
        james.service(setUpServiceManager());
        MockLogger mockLogger = new MockLogger();
        mockLogger.disableDebug();
        ContainerUtil.enableLogging(james, mockLogger);
        try {
            JamesTestConfiguration conf = new JamesTestConfiguration();
            conf.init();
            ContainerUtil.configure(james, conf);
            ContainerUtil.initialize(james);
        } catch (Exception e) {
            e.printStackTrace();
            fail("James.initialize() failed");
        }
        return james;
    }
    

    private MockServiceManager setUpServiceManager() throws Exception {
        MockServiceManager serviceManager = new MockServiceManager();
        MockUsersRepository mockUsersRepository = new MockUsersRepository();
       
        
        serviceManager.put(UsersRepository.ROLE, mockUsersRepository);
        serviceManager.put(UsersStore.ROLE, usersStore);
        serviceManager.put(FileSystem.ROLE, fs);
        serviceManager.put(DomainList.ROLE, domList);

        serviceManager.put(Store.ROLE, mockStore);
        serviceManager.put(DNSService.ROLE, dns);
        return serviceManager;
    }
}
