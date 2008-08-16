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
import org.apache.avalon.framework.service.ServiceException;
import org.apache.james.api.dnsservice.AbstractDNSServer;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.domainlist.DomainList;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.user.UsersStore;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailServer;
import org.apache.james.services.MailServerTestAllImplementations;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockServiceManager;
import org.apache.james.test.mock.avalon.MockStore;
import org.apache.james.test.mock.james.InMemorySpoolRepository;
import org.apache.james.test.mock.james.MockUsersStore;
import org.apache.james.userrepository.MockUsersRepository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

public class JamesTest extends MailServerTestAllImplementations {
    
    private File tempContextFile = null;
    private InMemorySpoolRepository mockMailRepository;

    public MailServer createMailServer() throws ServiceException {
        James james = new James();
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
    
    

    protected void tearDown() throws Exception {
        if (tempContextFile != null) {
            tempContextFile.delete();
        }
        if (mockMailRepository != null) {
            ContainerUtil.dispose(mockMailRepository);
        }
        super.tearDown();
    }



    private MockServiceManager setUpServiceManager() {
        MockServiceManager serviceManager = new MockServiceManager();
        MockUsersRepository mockUsersRepository = new MockUsersRepository();
        serviceManager.put(UsersRepository.ROLE, mockUsersRepository);
        serviceManager.put(UsersStore.ROLE, new MockUsersStore(mockUsersRepository));
        serviceManager.put(FileSystem.ROLE, new FileSystem() {

            public File getBasedir() throws FileNotFoundException {
                return new File(".");
            }

            public InputStream getResource(String url) throws IOException {
                return new FileInputStream(getFile(url)); 
            }

            public File getFile(String fileURL) throws FileNotFoundException {
                return new File("./conf/");
            }
            
        });
        serviceManager.put(DomainList.ROLE, new DomainList() {

            public List getDomains() {
                ArrayList d = new ArrayList();
                d.add("localhost");
                return d;
            }

            public boolean containsDomain(String domain) {
                return getDomains().contains(domain);
            }

        public void setAutoDetect(boolean autodetect) {}

        public void setAutoDetectIP(boolean autodetectIP) {}            
        });
        MockStore mockStore = new MockStore();
        mockMailRepository = new InMemorySpoolRepository();
        mockStore.add(EXISTING_USER_NAME, mockMailRepository);
        serviceManager.put(Store.ROLE, mockStore);
        serviceManager.put(DNSService.ROLE, setUpDNSServer());
        return serviceManager;
    }
    
    private DNSService setUpDNSServer() {
        DNSService dns = new AbstractDNSServer() {
            public String getHostName(InetAddress addr) {
                return "localhost";
            }

            public InetAddress getLocalHost() throws UnknownHostException {
                throw new UnknownHostException("Unknown");
            }
        };
        return dns;
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
