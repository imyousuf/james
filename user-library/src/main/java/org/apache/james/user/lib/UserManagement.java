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




package org.apache.james.user.lib;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Resource;
import javax.management.NotCompliantMBeanException;
import javax.management.StandardMBean;

import org.apache.james.user.api.JamesUser;
import org.apache.james.user.api.User;
import org.apache.james.user.api.UserManagementMBean;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersStore;

public class UserManagement extends StandardMBean implements UserManagementMBean {
    
    /**
     * The administered UsersRepository
     */
    private UsersRepository localUsers;
    private UsersStore usersStore;

    @Resource(name="localusersrepository")
    public void setUsersRepository(UsersRepository localUsers) {
        this.localUsers = localUsers;
    }
    
    @Resource(name="usersstore")
    public void setUsersStore(UsersStore usersStore) {
        this.usersStore = usersStore;
    }

    
    public UserManagement() throws NotCompliantMBeanException {
        super(UserManagementMBean.class);
    }


    private JamesUser getJamesUser(String userName, String repositoryName) {
        User baseuser = getUserRepository(repositoryName).getUserByName(userName);
        if (baseuser == null) throw new IllegalArgumentException("user not found: " + userName);
        if (! (baseuser instanceof JamesUser ) ) throw new IllegalArgumentException("user is not of type JamesUser: " + userName);

        return (JamesUser) baseuser;
    }

    private UsersRepository getUserRepository(String repositoryName) {
        if (repositoryName == null) return localUsers; // return default

        if (usersStore == null) throw new IllegalArgumentException("cannot access user repository named " + repositoryName);

        UsersRepository repository = usersStore.getRepository(repositoryName);
        if (repository == null) throw new IllegalArgumentException("user repository does not exist: " + repositoryName);
        
        return repository;
    }

    /**
     * @see org.apache.james.user.api.UserManagementMBean#addUser(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean addUser(String userName, String password, String repositoryName) {
        return getUserRepository(repositoryName).addUser(userName, password);
    }

    /**
     * @see org.apache.james.user.api.UserManagementMBean#deleteUser(java.lang.String, java.lang.String)
     */
    public boolean deleteUser(String userName, String repositoryName) {
        UsersRepository users = getUserRepository(repositoryName);
        if (!users.contains(userName)) return false;
        users.removeUser(userName);
        return true;
    }

    /**
     * @see org.apache.james.user.api.UserManagementMBean#verifyExists(java.lang.String, java.lang.String)
     */
    public boolean verifyExists(String userName, String repositoryName) {
        UsersRepository users = getUserRepository(repositoryName);
        return users.contains(userName);
    }

    /**
     * @see org.apache.james.user.api.UserManagementMBean#countUsers(java.lang.String)
     */
    public long countUsers(String repositoryName) {
        UsersRepository users = getUserRepository(repositoryName);
        return users.countUsers();
    }

    /**
     * @see org.apache.james.user.api.UserManagementMBean#listAllUsers(java.lang.String)
     */
    public String[] listAllUsers(String repositoryName) {
        List<String> userNames = new ArrayList<String>();
        UsersRepository users = getUserRepository(repositoryName);
        for (Iterator<String> it = users.list(); it.hasNext();) {
            userNames.add(it.next());
        }
        return (String[])userNames.toArray(new String[]{});
    }

    /**
     * @see org.apache.james.user.api.UserManagementMBean#setPassword(java.lang.String, java.lang.String, java.lang.String)
     */
    public boolean setPassword(String userName, String password, String repositoryName) {
        UsersRepository users = getUserRepository(repositoryName);
        User user = users.getUserByName(userName);
        if (user == null) throw new IllegalArgumentException("user not found: " + userName);
        return user.setPassword(password);
    }

    /**
     * @see org.apache.james.user.api.UserManagementMBean#unsetAlias(java.lang.String, java.lang.String)
     */
    public boolean unsetAlias(String userName, String repositoryName) {
        JamesUser user = getJamesUser(userName, null);
        if (!user.getAliasing()) return false;
        
        user.setAliasing(false);
        getUserRepository(repositoryName).updateUser(user);
        return true;
    }

    /**
     * @see org.apache.james.user.api.UserManagementMBean#getAlias(java.lang.String, java.lang.String)
     */
    public String getAlias(String userName, String repositoryName) {
        JamesUser user = getJamesUser(userName, null);
        if (!user.getAliasing()) return null;
        return user.getAlias();
    }

    /**
     * @see org.apache.james.user.api.UserManagementMBean#unsetForwardAddress(java.lang.String, java.lang.String)
     */
    public boolean unsetForwardAddress(String userName, String repositoryName) {
        JamesUser user = getJamesUser(userName, null);

        if (!user.getForwarding()) return false;
        
        user.setForwarding(false);
        getUserRepository(repositoryName).updateUser(user);
        return true;
    }

    /**
     * @see org.apache.james.user.api.UserManagementMBean#getForwardAddress(java.lang.String, java.lang.String)
     */
    public String getForwardAddress(String userName, String repositoryName) {
        JamesUser user = getJamesUser(userName, null);
        if (!user.getForwarding()) return null;
        return user.getForwardingDestination().toString();
    }

    /**
     * @see org.apache.james.user.api.UserManagementMBean#getUserRepositoryNames()
     */
    public List<String> getUserRepositoryNames() {
        List<String> result = new ArrayList<String>();
        if (usersStore == null) return result;
        
        Iterator<String> repositoryNames = usersStore.getRepositoryNames();
        while (repositoryNames.hasNext()) {
            String name = repositoryNames.next();
            result.add(name);
        }
        return result;
    }

}
