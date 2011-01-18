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

package org.apache.james.user.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.logging.impl.SimpleLog;
import org.apache.james.filesystem.api.FileSystem;
import org.apache.james.lifecycle.api.LifecycleUtil;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.user.api.model.JamesUser;
import org.apache.james.user.file.UsersFileRepository;
import org.apache.james.user.lib.AbstractUsersRepositoryTest;
import org.apache.james.vut.api.VirtualUserTable;
import org.apache.mailet.MailAddress;

/**
 * Test basic behaviours of UsersFileRepository
 */
public class UsersFileRepositoryTest extends AbstractUsersRepositoryTest {

    /**
     * Create the repository to be tested.
     * 
     * @return the user repository
     * @throws Exception 
     */
    protected UsersRepository getUsersRepository() throws Exception {
        FileSystem fs = new FileSystem() {

            public File getBasedir() throws FileNotFoundException {
                return new File(".");
            }

            public InputStream getResource(String url) throws IOException {
                return new FileInputStream(getFile(url)); 
            }

            public File getFile(String fileURL) throws FileNotFoundException {
                return new File(fileURL.substring(FileSystem.FILE_PROTOCOL.length()));
            }
            
        };
        
        DefaultConfigurationBuilder configuration = new DefaultConfigurationBuilder("test");
        configuration.addProperty("destination.[@URL]", "file://target/var/users");
        
        UsersFileRepository res = new UsersFileRepository();

        

        res.setFileSystem(fs);
        res.setLog(new SimpleLog("MockLog"));
        res.configure(configuration);
        res.init();
        return res;
    }

    protected void disposeUsersRepository() throws UsersRepositoryException {
        if (this.usersRepository != null) {
            Iterator<String> i = this.usersRepository.list();
            while (i.hasNext()) {
                this.usersRepository.removeUser((String) i.next());
            }
            LifecycleUtil.dispose(this.usersRepository);
        }
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
        
        Collection<String> map = ((VirtualUserTable) repos).getMappings(username, domain);
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
        Iterator<String> mappings = map.iterator();
        assertTrue("Two mapping",map.size() == 2);
        assertEquals("Alias found", mappings.next().toString(), alias + "@" + domain);
        assertEquals("Forward found", mappings.next().toString(), forward);
    }

}
