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

package org.apache.james.imapserver.mock;

import java.util.Arrays;
import java.util.Iterator;

import org.apache.james.imapserver.TestConstants;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;

public class MockUsersRepository implements UsersRepository, TestConstants
{

    public boolean addUser(User user)
    {
        throw new RuntimeException("not implemented");
    }

    public void addUser(String name, Object attributes)
    {
        throw new RuntimeException("not implemented");
    }

    public boolean addUser(String username, String password)
    {
        throw new RuntimeException("not implemented");
    }

    public User getUserByName(String name)
    {
        if (USER_NAME.equals(name)) {
            return new MockUser();
        }
        return null;
    }

    public User getUserByNameCaseInsensitive(String name)
    {
        if (USER_NAME.equalsIgnoreCase(name)) {
            return new MockUser();
        }
        return null;
    }

    public String getRealName(String name)
    {
        if (USER_NAME.equalsIgnoreCase(name)) {
            return USER_REALNAME;
        } else {
            return null;
        }
    }

    public boolean updateUser(User user)
    {
        throw new RuntimeException("not implemented");

    }

    public void removeUser(String name)
    {
        throw new RuntimeException("not implemented");

    }

    public boolean contains(String name)
    {
        if (USER_NAME.equals(name)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean containsCaseInsensitive(String name)
    {
        if (USER_NAME.equalsIgnoreCase(name)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean test(String name, String password)
    {
        User user=getUserByName(name);
        if (user!=null) {
            return user.verifyPassword(password);
        }
        return false;
    }

    public int countUsers()
    {
        return 1;
    }

    public Iterator list()
    {
        return Arrays.asList(new String[] { USER_NAME }).iterator();
    }

}
