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
package org.apache.james.impl.vut;

import java.util.Collection;
import java.util.Map;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.james.api.vut.VirtualUserTableStore;
import org.apache.james.api.vut.management.VirtualUserTableManagementException;
import org.apache.james.api.vut.management.VirtualUserTableManagementMBean;
import org.apache.james.api.vut.management.VirtualUserTableManagementService;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonVirtualUserTableManagement implements Serviceable, Initializable, VirtualUserTableManagementService, VirtualUserTableManagementMBean{

    private VirtualUserTableManagement vutManage;
    private org.apache.james.api.vut.management.VirtualUserTableManagement vManage;
    private VirtualUserTableStore store;
    
    public void service(ServiceManager manager) throws ServiceException {
        store = (VirtualUserTableStore) manager.lookup(VirtualUserTableStore.ROLE);
        vManage= (org.apache.james.api.vut.management.VirtualUserTableManagement) manager.lookup(org.apache.james.api.vut.management.VirtualUserTableManagement.ROLE);
    }

    public void initialize() throws Exception {
        vutManage = Guice.createInjector(new Jsr250Module(), new VirtualUserTableManagementModule()).getInstance(VirtualUserTableManagement.class);
    }

    public boolean addAddressMapping(String virtualUserTable, String user, String domain, String address) throws VirtualUserTableManagementException {
        return vutManage.addAddressMapping(virtualUserTable, user, domain, address);
    }

    public boolean addAliasDomainMapping(String virtualUserTable, String aliasDomain, String realDomain) throws VirtualUserTableManagementException {
        return vutManage.addAliasDomainMapping(virtualUserTable, aliasDomain, realDomain);
    }

    public boolean addErrorMapping(String virtualUserTable, String user, String domain, String error) throws VirtualUserTableManagementException {
        return vutManage.addErrorMapping(virtualUserTable, user, domain, error);
    }

    public boolean addMapping(String virtualUserTable, String user, String domain, String mapping) throws VirtualUserTableManagementException {
        return vutManage.addMapping(virtualUserTable, user, domain, mapping);
    }

    public boolean addRegexMapping(String virtualUserTable, String user, String domain, String regex) throws VirtualUserTableManagementException {
        return vutManage.addRegexMapping(virtualUserTable, user, domain, regex);
    }

    public Map<String,Collection<String>> getAllMappings(String virtualUserTable) throws VirtualUserTableManagementException {
        return vutManage.getAllMappings(virtualUserTable);
    }

    public Collection<String> getUserDomainMappings(String virtualUserTable, String user, String domain) throws VirtualUserTableManagementException {
        return vutManage.getUserDomainMappings(virtualUserTable, user, domain);
    }

    public boolean removeAddressMapping(String virtualUserTable, String user, String domain, String address) throws VirtualUserTableManagementException {
        return vutManage.removeAddressMapping(virtualUserTable, user, domain, address);
    }

    public boolean removeAliasDomainMapping(String virtualUserTable, String aliasDomain, String realDomain) throws VirtualUserTableManagementException {
        return vutManage.removeAliasDomainMapping(virtualUserTable, aliasDomain, realDomain);
    }

    public boolean removeErrorMapping(String virtualUserTable, String user, String domain, String error) throws VirtualUserTableManagementException {
        return vutManage.removeErrorMapping(virtualUserTable, user, domain, error);
    }

    public boolean removeMapping(String virtualUserTable, String user, String domain, String mapping) throws VirtualUserTableManagementException {
        return vutManage.removeMapping(virtualUserTable, user, domain, mapping);
    }

    public boolean removeRegexMapping(String virtualUserTable, String user, String domain, String regex) throws VirtualUserTableManagementException {
        return vutManage.removeRegexMapping(virtualUserTable, user, domain, regex);
    }

    private class VirtualUserTableManagementModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(VirtualUserTableStore.class).annotatedWith(Names.named("org.apache.james.api.vut.VirtualUserTableStore")).toInstance(store);
            bind(org.apache.james.api.vut.management.VirtualUserTableManagement.class).annotatedWith(Names.named("org.apache.james.api.vut.management.VirtualUserTableManagement")).toInstance(vManage);
        }
        
    }
}
