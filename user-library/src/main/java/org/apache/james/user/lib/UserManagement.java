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

import org.apache.james.user.api.UserManagementMBean;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.model.JamesUser;
import org.apache.james.user.api.model.User;

public class UserManagement extends StandardMBean implements UserManagementMBean {
    
    /**
     * The administered UsersRepository
     */
    private UsersRepository localUsers;

    @Resource(name="localusersrepository")
    public void setUsersRepository(UsersRepository localUsers) {
        this.localUsers = localUsers;
    }
    
    
    public UserManagement() throws NotCompliantMBeanException {
        super(UserManagementMBean.class);
    }


    private JamesUser getJamesUser(String userName) {
        User baseuser = localUsers.getUserByName(userName);
        if (baseuser == null) throw new IllegalArgumentException("user not found: " + userName);
        if (! (baseuser instanceof JamesUser ) ) throw new IllegalArgumentException("user is not of type JamesUser: " + userName);

        return (JamesUser) baseuser;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.user.api.UserManagementMBean#addUser(java.lang.String, java.lang.String)
     */
    public boolean addUser(String userName, String password) {
        return localUsers.addUser(userName, password);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.user.api.UserManagementMBean#deleteUser(java.lang.String)
     */
    public boolean deleteUser(String userName) {
        if (!localUsers.contains(userName)) return false;
        localUsers.removeUser(userName);
        return true;
    }

    /*
     * /(non-Javadoc)
     * @see org.apache.james.user.api.UserManagementMBean#verifyExists(java.lang.String)
     */
    public boolean verifyExists(String userName) {
        return localUsers.contains(userName);
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.user.api.UserManagementMBean#countUsers()
     */
    public long countUsers() {
        return localUsers.countUsers();
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.user.api.UserManagementMBean#listAllUsers()
     */
    public String[] listAllUsers() {
        List<String> userNames = new ArrayList<String>();
        for (Iterator<String> it = localUsers.list(); it.hasNext();) {
            userNames.add(it.next());
        }
        return (String[])userNames.toArray(new String[]{});
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.user.api.UserManagementMBean#setPassword(java.lang.String, java.lang.String)
     */
    public boolean setPassword(String userName, String password) {
        User user = localUsers.getUserByName(userName);
        if (user == null) throw new IllegalArgumentException("user not found: " + userName);
        return user.setPassword(password);
    }


    /*
     * 
     * (non-Javadoc)
     * @see org.apache.james.user.api.UserManagementMBean#unsetAlias(java.lang.String)
     */
    public boolean unsetAlias(String userName) {
        JamesUser user = getJamesUser(userName);
        if (!user.getAliasing()) return false;
        
        user.setAliasing(false);
        localUsers.updateUser(user);
        return true;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.user.api.UserManagementMBean#getAlias(java.lang.String)
     */
    public String getAlias(String userName) {
        JamesUser user = getJamesUser(userName);
        if (!user.getAliasing()) return null;
        return user.getAlias();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.user.api.UserManagementMBean#unsetForwardAddress(java.lang.String)
     */
    public boolean unsetForwardAddress(String userName) {
        JamesUser user = getJamesUser(userName);

        if (!user.getForwarding()) return false;
        
        user.setForwarding(false);
        localUsers.updateUser(user);
        return true;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.user.api.UserManagementMBean#getForwardAddress(java.lang.String)
     */
    public String getForwardAddress(String userName) {
        JamesUser user = getJamesUser(userName);
        if (!user.getForwarding()) return null;
        return user.getForwardingDestination().toString();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.user.api.UserManagementMBean#getVirtualHostingEnabled()
     */
    public boolean getVirtualHostingEnabled() {
        return localUsers.supportVirtualHosting();
    }

}
