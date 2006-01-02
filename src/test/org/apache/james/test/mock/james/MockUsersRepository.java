/***********************************************************************
 * Copyright (c) 2000-2005 The Apache Software Foundation.             *
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
package org.apache.james.test.mock.james;

import org.apache.james.security.DigestUtil;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;
import org.apache.james.userrepository.DefaultUser;

import java.util.HashMap;
import java.util.Iterator;

public class MockUsersRepository implements UsersRepository {

    private final HashMap m_users = new HashMap();

    public boolean addUser(User user) {
        String key = user.getUserName();
        if (m_users.containsKey(key)) return false;
        m_users.put(key, user);
        return true; 
    }

    public void addUser(String name, Object attributes) {
        try {
            addUser(new DefaultUser(name, DigestUtil.digestString(((String)attributes), "SHA"), "SHA"));
        } catch (Exception e) {
            e.printStackTrace();  // encoding failed
        }
    }

    public Object getAttributes(String name) {
        return null;  // trivial implementation
    }

    public User getUserByName(String name) {
        return (User) m_users.get(name);
    }

    public User getUserByNameCaseInsensitive(String name) {
        return null;  // trivial implementation
    }

    public String getRealName(String name) {
        return ((User) m_users.get(name)).getUserName();
    }

    public boolean updateUser(User user) {
        return false;  // trivial implementation
    }

    public void removeUser(String name) {
        // trivial implementation
    }

    public boolean contains(String name) {
        return m_users.containsKey(name);
    }

    public boolean containsCaseInsensitive(String name) {
        return false;  // trivial implementation
    }

    public boolean test(String name, Object attributes) {
        return false;  // trivial implementation
    }

    public boolean test(String name, String password) {
        User user = getUserByName(name);
        if (user == null) return false;
        return user.verifyPassword(password);
    }

    public int countUsers() {
        return m_users.size();
    }

    public Iterator list() {
        return m_users.values().iterator();
    }
}
