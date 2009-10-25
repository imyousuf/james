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
import org.apache.james.api.vut.ErrorMappingException;
import org.apache.james.api.vut.VirtualUserTableStore;
import org.apache.james.api.vut.management.InvalidMappingException;
import org.apache.james.bridge.GuiceInjected;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonDefaultVirtualUserTable implements GuiceInjected, Serviceable, Initializable, org.apache.james.api.vut.management.VirtualUserTableManagement{

    private VirtualUserTableStore store;
    private DefaultVirtualUserTable vut;
  

    public void service(ServiceManager manager) throws ServiceException {
        store = (VirtualUserTableStore) manager.lookup(VirtualUserTableStore.ROLE);
    }

    public void initialize() throws Exception {
       vut = Guice.createInjector(new Jsr250Module(), new DefaultVirtualUserTableModule()).getInstance(DefaultVirtualUserTable.class);
    }

    public boolean addAddressMapping(String user, String domain, String address) throws InvalidMappingException {
        return vut.addAddressMapping(user, domain, address);
    }

    public boolean addAliasDomainMapping(String aliasDomain, String realDomain) throws InvalidMappingException {
        return vut.addAliasDomainMapping(aliasDomain, realDomain);
    }

    public boolean addErrorMapping(String user, String domain, String error) throws InvalidMappingException {
        return vut.addErrorMapping(user, domain, error);
    }

    public boolean addMapping(String user, String domain, String mapping) throws InvalidMappingException {
        return vut.addMapping(user, domain, mapping);
    }

    public boolean addRegexMapping(String user, String domain, String regex) throws InvalidMappingException {
        return vut.addRegexMapping(user, domain, regex);
    }

    public Map<String, Collection<String>> getAllMappings() {
        return vut.getAllMappings();
    }

    public Collection<String> getUserDomainMappings(String user, String domain) throws InvalidMappingException {
        return vut.getUserDomainMappings(user, domain);
    }

    public boolean removeAddressMapping(String user, String domain, String address) throws InvalidMappingException {
        return vut.removeAddressMapping(user, domain, address);
    }

    public boolean removeAliasDomainMapping(String aliasDomain, String realDomain) throws InvalidMappingException {
        return vut.removeAliasDomainMapping(aliasDomain, realDomain);
    }

    public boolean removeErrorMapping(String user, String domain, String error) throws InvalidMappingException {
        return vut.removeErrorMapping(user, domain, error);
    }

    public boolean removeMapping(String user, String domain, String mapping) throws InvalidMappingException {
        return vut.removeMapping(user, domain, mapping);
    }

    public boolean removeRegexMapping(String user, String domain, String regex) throws InvalidMappingException {
        return vut.removeRegexMapping(user, domain, regex);
    }

    public Collection<String> getMappings(String user, String domain) throws ErrorMappingException {
        return vut.getMappings(user, domain);
    }
    
    private class DefaultVirtualUserTableModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(VirtualUserTableStore.class).annotatedWith(Names.named("org.apache.james.api.vut.VirtualUserTableStore")).toInstance(store);
        }
        
    }

}
