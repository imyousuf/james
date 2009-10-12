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

package org.apache.james.userrepository;

import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.avalon.framework.service.DefaultServiceManager;
import org.apache.james.api.user.JamesUser;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.vut.VirtualUserTable;
import org.apache.james.mailrepository.filepair.File_Persistent_Object_Repository;
import org.apache.james.services.FileSystem;
import org.apache.james.test.mock.avalon.MockLogger;
import org.apache.james.test.mock.avalon.MockStore;
import org.apache.mailet.MailAddress;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.Collection;
import java.util.Iterator;

/**
 * Test basic behaviours of UsersFileRepository
 */
public class UsersFileRepositoryTest extends MockUsersRepositoryTest {

    /**
     * Create the repository to be tested.
     * 
     * @return the user repository
     * @throws Exception 
     */
    protected UsersRepository getUsersRepository() throws Exception {
        UsersFileRepository res = new UsersFileRepository();
        DefaultServiceManager serviceManager = new DefaultServiceManager();
        serviceManager.put(FileSystem.ROLE, new FileSystem() {

            public File getBasedir() throws FileNotFoundException {
                return new File(".");
            }

            public InputStream getResource(String url) throws IOException {
                return new FileInputStream(getFile(url)); 
            }

            public File getFile(String fileURL) throws FileNotFoundException {
                throw new UnsupportedOperationException();
            }
            
        });
        MockStore mockStore = new MockStore();
        File_Persistent_Object_Repository file_Persistent_Object_Repository = new File_Persistent_Object_Repository();
        file_Persistent_Object_Repository.service(serviceManager);
        file_Persistent_Object_Repository.enableLogging(new MockLogger());
        DefaultConfiguration defaultConfiguration22 = new DefaultConfiguration("conf");
        defaultConfiguration22.setAttribute("destinationURL", "file://target/var/users");
        file_Persistent_Object_Repository.configure(defaultConfiguration22);
        file_Persistent_Object_Repository.initialize();
        mockStore.add("OBJECT.users", file_Persistent_Object_Repository);
        res.setStore(mockStore);
        DefaultConfiguration configuration = new DefaultConfiguration("test");
        DefaultConfiguration destinationConf = new DefaultConfiguration("destination");
        destinationConf.setAttribute("URL", "file://target/var/users");
        configuration.addChild(destinationConf);
        res.enableLogging(new ConsoleLogger());
        res.configure(configuration );
        res.initialize();
        return res;
    }

    protected void disposeUsersRepository() {
        Iterator i = this.usersRepository.list();
        while (i.hasNext()) {
            this.usersRepository.removeUser((String) i.next());
        }
        ContainerUtil.dispose(this.usersRepository);
    }
    
    public void testVirtualUserTableImpl() throws Exception {
        String username = "test";
        String password = "pass";
        String alias = "alias";
        String domain = "localhost";
        String forward = "forward@somewhere";
        
        UsersFileRepository repos = (UsersFileRepository) getUsersRepository();
        repos.setEnableAliases(true);
        repos.setEnableForwarding(true);
        repos.addUser(username,password);
        
        JamesUser user = (JamesUser)repos.getUserByName(username);
        user.setAlias(alias);
        repos.updateUser(user);
        
        Collection map = ((VirtualUserTable) repos).getMappings(username, domain);
        assertNull("No mapping", map);
        
        user.setAliasing(true);
        repos.updateUser(user);
        map = ((VirtualUserTable) repos).getMappings(username, domain);
        assertEquals("One mapping", 1, map.size());
        assertEquals("Alias found", map.iterator().next().toString(), alias + "@" + domain);
        
        
        user.setForwardingDestination(new MailAddress(forward));
        repos.updateUser(user);
        map = ((VirtualUserTable) repos).getMappings(username, domain);
        assertTrue("One mapping", map.size() == 1);
        assertEquals("Alias found", map.iterator().next().toString(), alias + "@" + domain);
        
        
        user.setForwarding(true);
        repos.updateUser(user);
        map = ((VirtualUserTable) repos).getMappings(username, domain);
        Iterator mappings = map.iterator();
        assertTrue("Two mapping",map.size() == 2);
        assertEquals("Alias found", mappings.next().toString(), alias + "@" + domain);
        assertEquals("Forward found", mappings.next().toString(), forward);
    }


}
