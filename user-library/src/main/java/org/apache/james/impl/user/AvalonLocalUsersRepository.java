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
package org.apache.james.impl.user;

import java.util.Iterator;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.api.user.User;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.user.UsersStore;
import org.apache.james.bridge.GuiceInjected;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonLocalUsersRepository implements GuiceInjected, Initializable,Serviceable,UsersRepository{

    private UsersStore usersStore;
    private LocalUsersRepository repos;
    
    public void initialize() throws Exception {
        repos = Guice.createInjector(new Jsr250Module(), new LocalUsersRepositoryModule()).getInstance(LocalUsersRepository.class);
    }

    public void service(ServiceManager manager) throws ServiceException {
        usersStore = (UsersStore) manager.lookup(UsersStore.ROLE);
    }
    
    private class LocalUsersRepositoryModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(UsersStore.class).annotatedWith(Names.named("org.apache.james.api.user.UsersStore")).toInstance(usersStore);
        }
        
    }

    public boolean addUser(User user) {
        return repos.addUser(user);
    }

    public void addUser(String name, Object attributes) {
        repos.addUser(name, attributes);
    }

    public boolean addUser(String username, String password) {
        return repos.addUser(username, password);
    }

    public boolean contains(String name) {
        return repos.contains(name);
    }

    public boolean containsCaseInsensitive(String name) {
        return repos.containsCaseInsensitive(name);
    }

    public int countUsers() {
        return repos.countUsers();
    }

    public String getRealName(String name) {
        return repos.getRealName(name);
    }

    public User getUserByName(String name) {
        return repos.getUserByName(name);
    }

    public User getUserByNameCaseInsensitive(String name) {
        return repos.getUserByNameCaseInsensitive(name);
    }

    public Iterator<String> list() {
        return repos.list();
    }

    public void removeUser(String name) {
        repos.removeUser(name);
    }

    public boolean test(String name, String password) {
        return repos.test(name, password);
    }

    public boolean updateUser(User user) {
        return repos.updateUser(user);
    }
    

}
