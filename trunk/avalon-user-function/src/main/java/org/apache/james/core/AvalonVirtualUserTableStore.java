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

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.vut.VirtualUserTable;
import org.apache.james.api.vut.VirtualUserTableStore;
import org.apache.james.services.FileSystem;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonVirtualUserTableStore extends AbstractAvalonStore implements VirtualUserTableStore{
    private DataSourceSelector selector;
    private FileSystem fs;
    private VirtualUserTableStore vStore;
    private DNSService dns;
   
    
    public void service(ServiceManager manager) throws ServiceException {
        selector = (DataSourceSelector) manager.lookup(DataSourceSelector.ROLE);
        fs = (FileSystem) manager.lookup(FileSystem.ROLE);
        dns = (DNSService) manager.lookup(DNSService.ROLE);
    }

    public void initialize() throws Exception {
        vStore = Guice.createInjector(new Jsr250Module(), new AvalonVirtualUserTableStoreModule()).getInstance(GuiceVirtualUserTableStore.class);
    }

    public VirtualUserTable getTable(String name) {
        return vStore.getTable(name);
    }
    
    public class AvalonVirtualUserTableStoreModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(configuration);
            bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
            bind(DataSourceSelector.class).annotatedWith(Names.named("org.apache.avalon.cornerstone.services.datasources.DataSourceSelector")).toInstance(selector);
            bind(FileSystem.class).annotatedWith(Names.named("org.apache.james.services.FileSystem")).toInstance(fs);
            bind(DNSService.class).annotatedWith(Names.named("org.apache.james.api.dnsservice.DNSService")).toInstance(dns);
        }
        
    }
    
}
