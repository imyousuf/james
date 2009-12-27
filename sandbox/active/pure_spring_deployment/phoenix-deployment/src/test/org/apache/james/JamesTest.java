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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.api.dnsservice.AbstractDNSServer;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.domainlist.DomainList;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailServer;
import org.apache.james.services.MailServerTestAllImplementations;
import org.apache.james.test.mock.avalon.MockStore;
import org.apache.james.test.mock.james.InMemorySpoolRepository;
import org.apache.james.test.mock.james.MockUsersStore;
import org.apache.james.userrepository.MockUsersRepository;
import org.apache.james.util.ConfigurationAdapter;
import org.apache.mailet.MailetContext;

public class JamesTest extends MailServerTestAllImplementations {
    
    protected File tempContextFile = null;
    protected InMemorySpoolRepository mockMailRepository;
    protected MockUsersRepository mockUsersRepository;
    protected MockUsersStore usersStore;
    protected FileSystem fs;
    protected DomainList domList;
    protected MockStore mockStore;
    protected DNSService dns;
    
    public MailServer createMailServer() throws Exception {
        James james = new James();
        james.setDNSService(dns);
        james.setDomainList(domList);
        james.setFileSystem(fs);
        james.setSpoolRepository(mockMailRepository);
        james.setStore(mockStore);
        james.setUsersRepository(mockUsersRepository);
        james.setLog(new SimpleLog("JamesLog"));
        try {
            JamesTestConfiguration conf = new JamesTestConfiguration();
            conf.init();
            james.configure(new ConfigurationAdapter(conf));
            james.init();
        } catch (Exception e) {
            e.printStackTrace();
            fail("James.initialize() failed");
        }
        return james;
    }
    
    protected void setUp() throws Exception{
        super.setUp();
        mockUsersRepository = new MockUsersRepository();
        mockUsersRepository.setLog(new SimpleLog("MockLog"));
        mockUsersRepository.configure(new DefaultConfigurationBuilder());
        
        usersStore = new MockUsersStore(mockUsersRepository);
        
        fs = new FileSystem() {

            public File getBasedir() throws FileNotFoundException {
                return new File(".");
            }

            public InputStream getResource(String url) throws IOException {
                return new FileInputStream(getFile(url)); 
            }

            public File getFile(String fileURL) throws FileNotFoundException {
                return new File("./conf/");
            }
            
        };
        
        domList = new DomainList() {

            public List<String> getDomains() {
                ArrayList<String> d = new ArrayList<String>();
                d.add("localhost");
                return d;
            }

            public boolean containsDomain(String domain) {
                return getDomains().contains(domain);
            }

        public void setAutoDetect(boolean autodetect) {}

        public void setAutoDetectIP(boolean autodetectIP) {}            
        };
        
        mockStore = new MockStore();
        
        mockMailRepository = new InMemorySpoolRepository();
        mockStore.add(EXISTING_USER_NAME, mockMailRepository);
        
        dns = new AbstractDNSServer() {
            public String getHostName(InetAddress addr) {
                return "localhost";
            }

            public InetAddress getLocalHost() throws UnknownHostException {
                throw new UnknownHostException("Unknown");
            }
        };
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

    public boolean allowsPasswordlessUser() {
        return false;
    }

    public boolean canTestUserExists() {
        return true;
    }

    public boolean isUserExisting(MailServer mailServerImpl, String username) {
        return ((MailetContext)mailServerImpl).isLocalUser(username);
    }
}
