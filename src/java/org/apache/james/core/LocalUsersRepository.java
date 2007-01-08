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

package org.apache.james.core;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;

import java.util.Iterator;

public class LocalUsersRepository implements UsersRepository, Serviceable, Initializable {

    private UsersStore usersStore;
    private UsersRepository users;

    public void service(ServiceManager serviceManager) throws ServiceException {
        usersStore =
           (UsersStore) serviceManager.lookup(UsersStore.ROLE);
    }

    public void initialize() throws Exception {
        users = usersStore.getRepository("LocalUsers");
        if (users == null) {
            throw new ServiceException("","The user repository could not be found.");
        }
    }

    public boolean addUser(User user) {
        return users.addUser(user);
    }

    public void addUser(String name, Object attributes) {
        users.addUser(name,attributes);
    }

    public boolean addUser(String username, String password) {
        return users.addUser(username, password);
    }

    public User getUserByName(String name) {
        return users.getUserByName(name);
    }

    public User getUserByNameCaseInsensitive(String name) {
        return users.getUserByNameCaseInsensitive(name);
    }

    public String getRealName(String name) {
        return users.getRealName(name);
    }

    public boolean updateUser(User user) {
        return users.updateUser(user);
    }

    public void removeUser(String name) {
        users.removeUser(name);
    }

    public boolean contains(String name) {
        return users.contains(name);
    }

    public boolean containsCaseInsensitive(String name) {
        return users.containsCaseInsensitive(name);
    }

    public boolean test(String name, String password) {
        return users.test(name,password);
    }

    public int countUsers() {
        return users.countUsers();
    }

    public Iterator list() {
        return users.list();
    }

}
