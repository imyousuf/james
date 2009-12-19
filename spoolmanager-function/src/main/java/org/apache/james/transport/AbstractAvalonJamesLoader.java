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


package org.apache.james.transport;

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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.user.UsersStore;
import org.apache.james.api.vut.VirtualUserTable;
import org.apache.james.api.vut.VirtualUserTableStore;
import org.apache.james.bridge.GuiceInjected;
import org.apache.james.services.MailServer;
import org.apache.james.util.ConfigurationAdapter;
import org.apache.jsieve.mailet.Poster;
import org.apache.mailet.MailetContext;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Names;

public abstract class AbstractAvalonJamesLoader implements Configurable, Serviceable, Initializable, LogEnabled, GuiceInjected {

    

    private ConfigurationAdapter config;
    private AvalonLogger logger;
    private MailetContext context;
    private Poster poster;
    private DNSService dns;
    private MailServer mailserver;
    private UsersRepository userRepos;
    private DataSourceSelector dselector;
    private VirtualUserTableStore vutStore;
    private Store store;
    private VirtualUserTable vut;
    private UsersStore usersStore;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration config) throws ConfigurationException {
        try {
            this.config = new ConfigurationAdapter(config);
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new ConfigurationException("Unable to convert configuration", e);
        }
    }

    
    public void service(ServiceManager manager) throws ServiceException {       
        context = (MailetContext) manager.lookup("org.apache.mailet.MailetContext");
        poster = (Poster) manager.lookup("org.apache.jsieve.mailet.Poster");
        dns = (DNSService) manager.lookup(DNSService.ROLE);
        mailserver = (MailServer) manager.lookup(MailServer.ROLE);
        userRepos = (UsersRepository) manager.lookup(UsersRepository.ROLE);
        dselector = (DataSourceSelector) manager.lookup(DataSourceSelector.ROLE);
        vutStore = (VirtualUserTableStore) manager.lookup(VirtualUserTableStore.ROLE);
        store = (Store) manager.lookup(Store.ROLE);
        vut = (VirtualUserTable) manager.lookup(VirtualUserTable.ROLE);
        usersStore = (UsersStore) manager.lookup(UsersStore.ROLE);
          
    }


    /**
     * @see org.apache.avalon.framework.logger.LogEnabled#enableLogging(org.apache.avalon.framework.logger.Logger)
     */
    public void enableLogging(Logger logger) {
        this.logger = new AvalonLogger(logger);
    }


    protected class Module extends AbstractModule {

        @Override
        protected void configure() {
            bind(DNSService.class).annotatedWith(Names.named("dnsserver")).toInstance(dns);
            bind(org.apache.commons.configuration.HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(config);
            bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
            bind(MailetContext.class).annotatedWith(Names.named("James")).toInstance(context);
            bind(Poster.class).annotatedWith(Names.named("org.apache.jsieve.mailet.Poster")).toInstance(poster);
            bind(MailServer.class).annotatedWith(Names.named("James")).toInstance(mailserver);
            bind(UsersRepository.class).annotatedWith(Names.named("localusersrepository")).toInstance(userRepos);
            bind(DataSourceSelector.class).annotatedWith(Names.named("database-connections")).toInstance(dselector);
            bind(VirtualUserTable.class).annotatedWith(Names.named("defaultvirtualusertable")).toInstance(vut);
            bind(VirtualUserTableStore.class).annotatedWith(Names.named("virtualusertable-store")).toInstance(vutStore);
            bind(Store.class).annotatedWith(Names.named("mailstore")).toInstance(store);
            bind(UsersStore.class).annotatedWith(Names.named("users-store")).toInstance(usersStore);
            bind(LoaderService.class).annotatedWith(Names.named("org.apache.james.LoaderService")).toProvider(new Provider<LoaderService>() {

                public LoaderService get() {
                    return new MyLoaderService();
                }
                
                // Mimic the loaderservice
                class MyLoaderService implements LoaderService {
                    Injector injector = Guice.createInjector(new Module(), new Jsr250Module());

                    public <T> T load(Class<T> type) {
                        return injector.getInstance(type);
                    }
                    
                }
            });
        }
        
    }
}
