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
package org.apache.james.mailrepository;

import org.apache.avalon.cornerstone.services.datasources.DataSourceSelector;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.bridge.GuiceInjected;
import org.apache.james.services.FileSystem;
import org.apache.james.util.ConfigurationAdapter;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonMailStore implements GuiceInjected, Serviceable, Configurable, Initializable, LogEnabled, Store{

    private Store store;
    private FileSystem fs;
    private DataSourceSelector selector;
    private Log logger;
    private HierarchicalConfiguration config;
    
    public void service(ServiceManager manager) throws ServiceException {
        fs = (FileSystem) manager.lookup(FileSystem.ROLE);
        selector = (DataSourceSelector) manager.lookup(DataSourceSelector.ROLE);
    }

    public void configure(Configuration arg0) throws ConfigurationException {
        try {
            this.config = new ConfigurationAdapter(arg0);
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new ConfigurationException("Unable to convert config", e);
        }
    }

    public void initialize() throws Exception {
        store = Guice.createInjector(new Jsr250Module(), new AbstractModule() {
            
            @Override
            protected void configure() {
                bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
                bind(HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(config);
                bind(FileSystem.class).annotatedWith(Names.named("org.apache.james.services.FileSystem")).toInstance(fs);
                bind(DataSourceSelector.class).annotatedWith(Names.named("org.apache.avalon.cornerstone.services.datasources.DataSourceSelector")).toInstance(selector);
            }
        }).getInstance(GuiceMailStore.class);
    }

    public void enableLogging(Logger arg0) {
        this.logger = new AvalonLogger(arg0);
    }

    public Object select(Object arg0) throws ServiceException {
        return store.select(arg0);
    }

    public boolean isSelectable(Object arg0) {
        return store.isSelectable(arg0);
    }

    public void release(Object arg0) {
        store.release(arg0);
    }

}
