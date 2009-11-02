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
package org.apache.james.remotemanager;

import org.apache.avalon.cornerstone.services.sockets.SocketManager;
import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;
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
import org.apache.james.api.user.UsersStore;
import org.apache.james.api.vut.management.VirtualUserTableManagementService;
import org.apache.james.bridge.GuiceInjected;
import org.apache.james.management.BayesianAnalyzerManagementService;
import org.apache.james.management.DomainListManagementService;
import org.apache.james.management.ProcessorManagementService;
import org.apache.james.management.SpoolManagementService;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailServer;
import org.apache.james.socket.JamesConnectionManager;
import org.apache.james.socket.api.ProtocolHandlerFactory;
import org.apache.james.util.ConfigurationAdapter;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.name.Names;

public class AvalonRemoteManager implements GuiceInjected, Initializable, Serviceable, Configurable, LogEnabled, RemoteManagerMBean {
    
    private FileSystem filesystem;
    private MailServer mailserver;
    private DNSService dns;
    private Log logger;
    private org.apache.commons.configuration.HierarchicalConfiguration config;
    private Injector injector;
    private JamesConnectionManager connectionManager;
    private SocketManager socketManager;
    private RemoteManager server = new RemoteManager();
    private ThreadManager threadManager;
    private SpoolManagementService spoolService;
    private BayesianAnalyzerManagementService bayesianServer;
    private VirtualUserTableManagementService vutService;
    private DomainListManagementService domainService;
    private UsersStore usersStore;
    private ProcessorManagementService processorService;
    private Store store;
    
    public String getNetworkInterface() {
        return server.getNetworkInterface();
    }

    public int getPort() {
        return server.getPort();
    }

    public String getSocketType() {
        return server.getSocketType();
    }

    public boolean isEnabled() {
        return server.isEnabled();
    }

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

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager manager) throws ServiceException {
        dns = (DNSService) manager.lookup(DNSService.ROLE);
        mailserver = (MailServer) manager.lookup(MailServer.ROLE);
        filesystem = (FileSystem) manager.lookup(FileSystem.ROLE);
        socketManager = (SocketManager) manager.lookup(SocketManager.ROLE);
        connectionManager = (JamesConnectionManager) manager.lookup(JamesConnectionManager.ROLE);     
        threadManager = (ThreadManager) manager.lookup(ThreadManager.ROLE);
        spoolService = (SpoolManagementService) manager.lookup(SpoolManagementService.ROLE);
        bayesianServer = (BayesianAnalyzerManagementService) manager.lookup(BayesianAnalyzerManagementService.ROLE);
        vutService = (VirtualUserTableManagementService) manager.lookup(VirtualUserTableManagementService.ROLE);
        domainService = (DomainListManagementService) manager.lookup(DomainListManagementService.ROLE);
        usersStore = (UsersStore) manager.lookup(UsersStore.ROLE);
        processorService = (ProcessorManagementService) manager.lookup(ProcessorManagementService.ROLE);
        store = (Store) manager.lookup(Store.ROLE);
        // thats needed because of used excalibur socket components
        server.service(manager);
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        injector = Guice.createInjector(new RemoteManagerModule(), new Jsr250Module());
        injector.injectMembers(server);
    }
                 
    /**
     * @see org.apache.avalon.framework.logger.LogEnabled#enableLogging(org.apache.avalon.framework.logger.Logger)
     */
    public void enableLogging(Logger logger) {
        this.logger = new AvalonLogger(logger);
    }

    private final class RemoteManagerModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(DNSService.class).annotatedWith(Names.named("org.apache.james.api.dnsservice.DNSService")).toInstance(dns);
            bind(MailServer.class).annotatedWith(Names.named("org.apache.james.services.MailServer")).toInstance(mailserver);
            bind(org.apache.commons.configuration.HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(config);
            bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
            bind(FileSystem.class).annotatedWith(Names.named("org.apache.james.services.FileSystem")).toInstance(filesystem);
            bind(ProtocolHandlerFactory.class).annotatedWith(Names.named("org.apache.james.socket.api.ProtocolHandlerFactory")).toProvider(new Provider<ProtocolHandlerFactory>() {

                public ProtocolHandlerFactory get() {
                    return server;
                }
                
            });
            bind(SocketManager.class).annotatedWith(Names.named("org.apache.avalon.cornerstone.services.sockets.SocketManager")).toInstance(socketManager);
            bind(JamesConnectionManager.class).annotatedWith(Names.named("org.apache.james.socket.JamesConnectionManager")).toInstance(connectionManager);
            bind(ThreadManager.class).annotatedWith(Names.named("org.apache.avalon.cornerstone.services.threads.ThreadManager")).toInstance(threadManager);
            bind(SpoolManagementService.class).annotatedWith(Names.named("org.apache.james.management.SpoolManagementService")).toInstance(spoolService);
            bind(BayesianAnalyzerManagementService.class).annotatedWith(Names.named("org.apache.james.management.BayesianAnalyzerManagementService")).toInstance(bayesianServer);
            bind(UsersStore.class).annotatedWith(Names.named("org.apache.james.api.user.UsersStore")).toInstance(usersStore);
            bind(ProcessorManagementService.class).annotatedWith(Names.named("org.apache.james.management.ProcessorManagementService")).toInstance(processorService);
            bind(VirtualUserTableManagementService.class).annotatedWith(Names.named("org.apache.james.api.vut.management.VirtualUserTableManagementService")).toInstance(vutService);
            bind(DomainListManagementService.class).annotatedWith(Names.named("org.apache.james.management.DomainListManagementService")).toInstance(domainService);
            bind(Store.class).annotatedWith(Names.named("org.apache.avalon.cornerstone.services.store.Store")).toInstance(store);
            // we bind the LoaderService to an Provider to get sure everything is there when the SMTPLoaderService get created.
            bind(LoaderService.class).annotatedWith(Names.named("org.apache.james.LoaderService")).toProvider(new Provider<LoaderService>() {

                public LoaderService get() {
                    return new RemoteManagerLoaderService();
                }
                
                // Mimic the loaderservice
                class RemoteManagerLoaderService implements LoaderService {
                    Injector injector = Guice.createInjector(new RemoteManagerModule(), new Jsr250Module());

                    public <T> T load(Class<T> type) {
                        return injector.getInstance(type);
                    }
                    
                }
                
            });

        }
    }
}