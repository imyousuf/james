/***********************************************************************
 * Copyright (c) 2000-2006 The Apache Software Foundation.             *
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


package org.apache.james;

import org.apache.james.services.MailServer;
import org.apache.james.services.MailServerTestAllImplementations;
import org.apache.james.test.mock.avalon.MockContext;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.avalon.MockStore;
import org.apache.james.test.mock.james.MockUsersRepository;
import org.apache.james.test.mock.james.MockUsersStore;
import org.apache.james.test.mock.james.MockMailRepository;

import java.io.File;

public class JamesTest extends MailServerTestAllImplementations {

    public MailServer createMailServer() {
        James james = new James();
        james.service(setUpServiceManager());
        MockLogger mockLogger = new MockLogger();
        mockLogger.disableDebug();
        james.enableLogging(mockLogger);
        try {
            JamesTestConfiguration conf = new JamesTestConfiguration();
            conf.init();
            james.configure(conf);
            james.contextualize(new MockContext(File.createTempFile("james_test", "tmp")));
            james.initialize();
        } catch (Exception e) {
            e.printStackTrace();
            fail("James.initialize() failed");
        }
        return james;
    }

    private MockServiceManager setUpServiceManager() {
        MockServiceManager serviceManager = new MockServiceManager();
        MockUsersRepository mockUsersRepository = new MockUsersRepository();
        serviceManager.put("org.apache.james.services.UsersRepository", mockUsersRepository);
        serviceManager.put("org.apache.james.services.UsersStore", new MockUsersStore(mockUsersRepository));
        MockStore mockStore = new MockStore();
        mockStore.add(EXISTING_USER_NAME, new MockMailRepository());
        serviceManager.put("org.apache.avalon.cornerstone.services.store.Store", mockStore);
        return serviceManager;
    }

    public boolean allowsPasswordlessUser() {
        return false;
    }

    public boolean canTestUserExists() {
        return true;
    }

    public boolean isUserExisting(MailServer mailServerImpl, String username) {
        return ((James)mailServerImpl).isLocalUser(username);
    }
}
