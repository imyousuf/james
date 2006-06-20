/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.             *
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


package org.apache.james.management;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.User;
import org.apache.james.services.JamesUser;
import org.apache.mailet.MailAddress;

import javax.mail.internet.ParseException;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.List;

public class UserManagement implements UserManagementMBean, Serviceable {

    /**
     * The administered UsersRepository
     */
    private UsersRepository users;

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service( final ServiceManager componentManager )
        throws ServiceException {
        users = (UsersRepository) componentManager.lookup(UsersRepository.ROLE);
        if (users == null) {
            throw new ServiceException("","The user repository could not be found.");
        }
    }

    private JamesUser getJamesUser(String userName) throws UserManagementException {
        User baseuser = users.getUserByName(userName);
        if (baseuser == null) throw new UserManagementException("user not found: " + userName);
        if (! (baseuser instanceof JamesUser ) ) throw new UserManagementException("user is not of type JamesUser: " + userName);

        return (JamesUser) baseuser;
    }

    public boolean addUser(String userName, String password) {
        return users.addUser(userName, password);
    }

    public boolean deleteUser(String userName) {
        if (!users.contains(userName)) return false;
        users.removeUser(userName);
        return true;
    }

    public boolean verifyExists(String userName) {
        return users.contains(userName);
    }

    public long countUsers() {
        return users.countUsers();
    }

    public String[] listAllUsers() {
        List userNames = new ArrayList();
        for (Iterator it = users.list(); it.hasNext();) {
            userNames.add(it.next());
        }
        return (String[])userNames.toArray(new String[]{});
    }

    public boolean setPassword(String userName, String password) throws UserManagementException {
        User user = users.getUserByName(userName);
        if (user == null) throw new UserManagementException("user not found: " + userName);
        return user.setPassword(password);
    }

    public boolean setAlias(String userName, String aliasUserName) throws UserManagementException {
        JamesUser user = getJamesUser(userName);
        JamesUser aliasUser = getJamesUser(aliasUserName);
        if (aliasUser == null) return false;

        boolean success = user.setAlias(aliasUserName);
        user.setAliasing(true);
        users.updateUser(user);
        return success;
    }

    public boolean unsetAlias(String userName) throws UserManagementException {
        JamesUser user = getJamesUser(userName);
        if (!user.getAliasing()) return false;
        
        user.setAliasing(false);
        users.updateUser(user);
        return true;
    }

    public String getAlias(String userName) throws UserManagementException {
        JamesUser user = getJamesUser(userName);
        if (!user.getAliasing()) return null;
        return user.getAlias();
    }

    public boolean setForwardAddress(String userName, String forwardEmailAddress) throws UserManagementException {
        MailAddress forwardAddress;
        try {
             forwardAddress = new MailAddress(forwardEmailAddress);
        } catch(ParseException pe) {
            throw new UserManagementException(pe);
        }

        JamesUser user = getJamesUser(userName);
        boolean success = user.setForwardingDestination(forwardAddress);
        if (!success) return false;
        
        user.setForwarding(true);
        users.updateUser(user);
        return true;
    }

    public boolean unsetForwardAddress(String userName) throws UserManagementException {
        JamesUser user = getJamesUser(userName);

        if (!user.getForwarding()) return false;
        
        user.setForwarding(false);
        users.updateUser(user);
        return true;
    }

    public String getForwardAddress(String userName) throws UserManagementException {
        JamesUser user = getJamesUser(userName);
        if (!user.getForwarding()) return null;
        return user.getForwardingDestination().toString();
    }


}
