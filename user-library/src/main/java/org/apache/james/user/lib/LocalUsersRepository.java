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

import org.apache.james.user.api.User;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersStore;

import java.util.Iterator;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

/**
 * Provide access to the default "LocalUsers" UsersRepository.
 */
public class LocalUsersRepository implements UsersRepository {

    private UsersStore usersStore;
    protected UsersRepository users;

    @Resource(name="users-store")
    public void setUsersStore(UsersStore usersStore) {
        this.usersStore = usersStore;
    }

    
    @PostConstruct
    public void init() throws Exception {
        users = usersStore.getRepository("LocalUsers");
        if (users == null) {
            throw new Exception("The user repository could not be found.");
        }
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#addUser(org.apache.james.user.api.User)
     */
    public boolean addUser(User user) {
        return users.addUser(user);
    }
    
    /**
     * @see org.apache.james.user.api.UsersRepository#addUser(java.lang.String, java.lang.Object)
     */
    public void addUser(String name, Object attributes) {
        users.addUser(name,attributes);
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#addUser(java.lang.String, java.lang.String)
     */
    public boolean addUser(String username, String password) {
        return users.addUser(username, password);
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#getUserByName(java.lang.String)
     */
    public User getUserByName(String name) {
        return users.getUserByName(name);
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#getUserByNameCaseInsensitive(java.lang.String)
     */
    public User getUserByNameCaseInsensitive(String name) {
        return users.getUserByNameCaseInsensitive(name);
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#getRealName(java.lang.String)
     */
    public String getRealName(String name) {
        return users.getRealName(name);
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#updateUser(org.apache.james.user.api.User)
     */
    public boolean updateUser(User user) {
        return users.updateUser(user);
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#removeUser(java.lang.String)
     */
    public void removeUser(String name) {
        users.removeUser(name);
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#contains(java.lang.String)
     */
    public boolean contains(String name) {
        return users.contains(name);
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#containsCaseInsensitive(java.lang.String)
     */
    public boolean containsCaseInsensitive(String name) {
        return users.containsCaseInsensitive(name);
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#test(java.lang.String, java.lang.String)
     */
    public boolean test(String name, String password) {
        return users.test(name,password);
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#countUsers()
     */
    public int countUsers() {
        return users.countUsers();
    }

    /**
     * @see org.apache.james.user.api.UsersRepository#list()
     */
    public Iterator<String> list() {
        return users.list();
    }

}
