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

import java.util.List;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.user.UsersStore;
import org.apache.james.api.user.management.UserManagementException;
import org.apache.james.api.user.management.UserManagementMBean;
import org.apache.james.smtpserver.mina.GuiceInjected;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonUserManagement implements UserManagementMBean, GuiceInjected, Serviceable, Initializable {

    private UserManagementMBean bean;
    private UsersStore store;
    private UsersRepository repos;
    
    public boolean addUser(String userName, String password, String repositoryName) throws UserManagementException {
        return bean.addUser(userName, password, repositoryName);
    }

    public long countUsers(String repositoryName) throws UserManagementException {
        return bean.countUsers(repositoryName);
    }

    public boolean deleteUser(String userName, String repositoryName) throws UserManagementException {
        return bean.deleteUser(userName, repositoryName);
    }

    public String getAlias(String userName, String repositoryName) throws UserManagementException {
        return bean.getAlias(userName, repositoryName);
    }

    public String getForwardAddress(String userName, String repositoryName) throws UserManagementException {
        return bean.getForwardAddress(userName, repositoryName);
    }

    public List<String> getUserRepositoryNames() {
        return bean.getUserRepositoryNames();
    }

    public String[] listAllUsers(String repositoryName) throws UserManagementException {
        return bean.listAllUsers(repositoryName);
    }

    public boolean setAlias(String userName, String aliasUserName, String repositoryName) throws UserManagementException {
        return bean.setAlias(userName, aliasUserName, repositoryName);
    }

    public boolean setForwardAddress(String userName, String forwardEmailAddress, String repositoryName) throws UserManagementException {
        return bean.setForwardAddress(userName, forwardEmailAddress, repositoryName);
    }

    public boolean setPassword(String userName, String password, String repositoryName) throws UserManagementException {
        return bean.setPassword(userName, password, repositoryName);
    }

    public boolean unsetAlias(String userName, String repositoryName) throws UserManagementException {
        return bean.unsetAlias(userName, repositoryName);
    }

    public boolean unsetForwardAddress(String userName, String repositoryName) throws UserManagementException {
        return bean.unsetForwardAddress(userName, repositoryName);
    }

    public boolean verifyExists(String userName, String repositoryName) throws UserManagementException {
        return bean.verifyExists(userName, repositoryName);
    }

    public void service(ServiceManager arg0) throws ServiceException {
        store = (UsersStore) arg0.lookup(UsersStore.ROLE);
        repos = (UsersRepository) arg0.lookup(UsersRepository.ROLE);
    }

    public void initialize() throws Exception {
        bean = Guice.createInjector(new Jsr250Module(), new UserManagementModule()).getInstance(UserManagement.class);
    }
    
    private class UserManagementModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(UsersStore.class).annotatedWith(Names.named("org.apache.james.api.user.UsersStore")).toInstance(store);
            bind(UsersRepository.class).annotatedWith(Names.named("org.apache.james.api.user.UsersRepository")).toInstance(repos);
        }
        
    }

}
