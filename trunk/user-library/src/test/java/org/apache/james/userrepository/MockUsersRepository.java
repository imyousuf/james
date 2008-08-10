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

import org.apache.james.api.user.User;
import org.apache.james.security.DigestUtil;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class MockUsersRepository extends AbstractUsersRepository {

    private final HashMap m_users = new HashMap();

    /**
     * force the repository to hold implementations of JamesUser interface, instead of User
     * JamesUser is _not_ required as of the UsersRepository interface, so the necessarity forcing it
     * is due to code using UsersRepository while at the same time expecting it to hold JamesUsers
     * (like in RemoteManagerHandler) 
     */
    private boolean m_forceUseJamesUser = false;

    public void setForceUseJamesUser() {
        m_forceUseJamesUser = true;
    }

    public boolean addUser(User user) {

        if (m_forceUseJamesUser && user instanceof DefaultUser ) {
            DefaultUser aUser = (DefaultUser)user;
            user = new DefaultJamesUser(aUser.getUserName(),
                                             aUser.getHashedPassword(),
                                             aUser.getHashAlgorithm());
        }

        String key = user.getUserName();
        if (m_users.containsKey(key)) return false;
        m_users.put(key, user);
        return true;
    }

    public void addUser(String name, Object attributes) {
        if (!(attributes instanceof String)) {
            throw new IllegalArgumentException();
        }
        try {
            String passwordHash = DigestUtil.digestString(((String) attributes), "SHA");

            User user;

            if (m_forceUseJamesUser) {
                user = new DefaultJamesUser(name, passwordHash, "SHA");
            } else {
                user = new DefaultUser(name, passwordHash, "SHA");
            }
           
            addUser(user);
        } catch (Exception e) {
            e.printStackTrace();  // encoding failed
        }
    }

    public boolean addUser(String username, String password) {
        if (m_users.containsKey(username)) return false;
        try {
            String passwordHash = DigestUtil.digestString((password), "SHA");

            User user;

            if (m_forceUseJamesUser) {
                user = new DefaultJamesUser(username, passwordHash, "SHA");
            } else {
                user = new DefaultUser(username, passwordHash, "SHA");
            }
           
            return addUser(user);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();  // encoding failed
        }
        return false;
    }

    public Object getAttributes(String name) {
        return null;  // trivial implementation
    }

    public User getUserByName(String name) {
        if (ignoreCase) {
            return getUserByNameCaseInsensitive(name);
        } else {
            return (User) m_users.get(name);
        }
    }

    public User getUserByNameCaseInsensitive(String name) {
        return (User) m_users.get(name.toLowerCase(Locale.US));
    }

    public String getRealName(String name) {
        if (ignoreCase) {
            return m_users.get(name.toLowerCase(Locale.US)) != null ? ((User) m_users.get(name.toLowerCase(Locale.US))).getUserName() : null;
        } else {
            return m_users.get(name) != null ? name : null;
        }
    }

    public boolean updateUser(User user) {
        if (m_users.containsKey(user.getUserName())) {
            m_users.put(user.getUserName(), user);
            return true;
        } else return false;  // trivial implementation
    }

    public void removeUser(String name) {
        m_users.remove(name);
    }

    public boolean contains(String name) {
        if (ignoreCase) {
            return containsCaseInsensitive(name);
        } else {
            return m_users.containsKey(name);
        }
    }

    public boolean containsCaseInsensitive(String name) {
        throw new UnsupportedOperationException("mock");
    }

    public boolean test(String name, Object attributes) {
        throw new UnsupportedOperationException("mock");
    }

    public boolean test(String name, String password) {
        User user = getUserByName(name);
        if (user == null) return false;
        return user.verifyPassword(password);
    }

    public int countUsers() {
        return m_users.size();
    }

    protected List listUserNames() {
        Iterator users = m_users.values().iterator();
        List userNames = new LinkedList();
        while ( users.hasNext() ) {
            User user = (User)users.next();
            userNames.add(user.getUserName());
        }

        return userNames;
    }
    public Iterator list() {
        return listUserNames().iterator(); 
    }

    protected void doAddUser(User user) {
        // unused
    }

    protected void doUpdateUser(User user) {
        // unused
    }
}
