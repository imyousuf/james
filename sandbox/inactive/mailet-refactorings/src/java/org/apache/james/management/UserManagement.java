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




package org.apache.james.management;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.JamesUser;
import org.apache.james.services.UsersStore;
import org.apache.mailet.AliasedUser;
import org.apache.mailet.ForwardingUser;
import org.apache.mailet.MailAddress;
import org.apache.mailet.User;
import org.apache.mailet.UsersRepository;

import javax.mail.internet.ParseException;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public class UserManagement implements UserManagementMBean, Serviceable {

    String ROLE = "org.apache.james.management.UserManagement";
    
    /**
     * The administered UsersRepository
     */
    private UsersRepository localUsers;
    private UsersStore usersStore;

    public void setLocalUsers(UsersRepository localUsers) {
        this.localUsers = localUsers;
    }

    public void setUsersStore(UsersStore usersStore) {
        this.usersStore = usersStore;
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service( final ServiceManager componentManager )
        throws ServiceException {
        localUsers = (UsersRepository) componentManager.lookup(UsersRepository.ROLE);
        if (localUsers == null) {
            throw new ServiceException("","The local user repository could not be found.");
        }
        setLocalUsers(localUsers);
        usersStore = (UsersStore)componentManager.lookup( UsersStore.ROLE );
        if (usersStore == null) {
            throw new ServiceException("","The user store containing the user repositories could not be found.");
        }
        setUsersStore(usersStore);
    }

    private JamesUser getJamesUser(String userName, String repositoryName) throws UserManagementException {
        User baseuser = getUserRepository(repositoryName).getUserByName(userName);
        if (baseuser == null) throw new UserManagementException("user not found: " + userName);
        if (! (baseuser instanceof JamesUser ) ) throw new UserManagementException("user is not of type JamesUser: " + userName);

        return (JamesUser) baseuser;
    }

    private UsersRepository getUserRepository(String repositoryName) throws UserManagementException {
        if (repositoryName == null) return localUsers; // return default

        if (usersStore == null) throw new UserManagementException("cannot access user repository named " + repositoryName);

        UsersRepository repository = usersStore.getRepository(repositoryName);
        if (repository == null) throw new UserManagementException("user repository does not exist: " + repositoryName);
        
        return repository;
    }

    public boolean addUser(String userName, String password, String repositoryName) throws UserManagementException {
        return getUserRepository(repositoryName).addUser(userName, password);
    }

    public boolean deleteUser(String userName, String repositoryName) throws UserManagementException {
        UsersRepository users = getUserRepository(repositoryName);
        if (!users.contains(userName)) return false;
        users.removeUser(userName);
        return true;
    }

    public boolean verifyExists(String userName, String repositoryName) throws UserManagementException {
        UsersRepository users = getUserRepository(repositoryName);
        return users.contains(userName);
    }

    public long countUsers(String repositoryName) throws UserManagementException {
        UsersRepository users = getUserRepository(repositoryName);
        return users.countUsers();
    }

    public String[] listAllUsers(String repositoryName) throws UserManagementException {
        List userNames = new ArrayList();
        UsersRepository users = getUserRepository(repositoryName);
        for (Iterator it = users.list(); it.hasNext();) {
            userNames.add(it.next());
        }
        return (String[])userNames.toArray(new String[]{});
    }

    public boolean setPassword(String userName, String password, String repositoryName) throws UserManagementException {
        UsersRepository users = getUserRepository(repositoryName);
        User user = users.getUserByName(userName);
        if (user == null) throw new UserManagementException("user not found: " + userName);
        return user.setPassword(password);
    }

    public boolean setAlias(String userName, String aliasUserName, String repositoryName) throws UserManagementException {
        JamesUser user = getJamesUser(userName, null);
        ForwardingUser aliasUser = getJamesUser(aliasUserName, null);
        if (aliasUser == null) return false;

        boolean success = user.setAlias(aliasUserName);
        user.setAliasing(true);
        getUserRepository(repositoryName).updateUser(user);
        return success;
    }

    public boolean unsetAlias(String userName, String repositoryName) throws UserManagementException {
        JamesUser user = getJamesUser(userName, null);
        if (!user.getAliasing()) return false;
        
        user.setAliasing(false);
        getUserRepository(repositoryName).updateUser(user);
        return true;
    }

    public String getAlias(String userName, String repositoryName) throws UserManagementException {
        AliasedUser user = getJamesUser(userName, null);
        if (!user.getAliasing()) return null;
        return user.getAlias();
    }

    public boolean setForwardAddress(String userName, String forwardEmailAddress, String repositoryName) throws UserManagementException {
        MailAddress forwardAddress;
        try {
             forwardAddress = new MailAddress(forwardEmailAddress);
        } catch(ParseException pe) {
            throw new UserManagementException(pe);
        }

        JamesUser user = getJamesUser(userName, null);
        boolean success = user.setForwardingDestination(forwardAddress);
        if (!success) return false;
        
        user.setForwarding(true);
        getUserRepository(repositoryName).updateUser(user);
        return true;
    }

    public boolean unsetForwardAddress(String userName, String repositoryName) throws UserManagementException {
        JamesUser user = getJamesUser(userName, null);

        if (!user.getForwarding()) return false;
        
        user.setForwarding(false);
        getUserRepository(repositoryName).updateUser(user);
        return true;
    }

    public String getForwardAddress(String userName, String repositoryName) throws UserManagementException {
        ForwardingUser user = getJamesUser(userName, null);
        if (!user.getForwarding()) return null;
        return user.getForwardingDestination().toString();
    }

    public List getUserRepositoryNames() {
        List result = new ArrayList();
        if (usersStore == null) return result;
        
        Iterator repositoryNames = usersStore.getRepositoryNames();
        while (repositoryNames.hasNext()) {
            String name = (String) repositoryNames.next();
            result.add(name);
        }
        return result;
    }

}
