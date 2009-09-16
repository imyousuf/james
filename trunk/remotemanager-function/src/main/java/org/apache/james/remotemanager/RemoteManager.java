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

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.user.UsersStore;
import org.apache.james.api.vut.management.VirtualUserTableManagementService;
import org.apache.james.management.BayesianAnalyzerManagementService;
import org.apache.james.management.DomainListManagementService;
import org.apache.james.management.ProcessorManagementService;
import org.apache.james.management.SpoolManagementService;
import org.apache.james.services.MailServer;
import org.apache.james.socket.AbstractProtocolServer;
import org.apache.james.socket.ProtocolHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

/**
 * Provides a really rude network interface to administer James.
 * Allow to add accounts.
 * TODO: -improve protocol
 *       -add remove user
 *       -much more...
 * @version 1.0.0, 24/04/1999
 */
public class RemoteManager
    extends AbstractProtocolServer implements RemoteManagerMBean {

    /**
     * A HashMap of (user id, passwords) for James administrators
     */
    private HashMap adminAccounts = new HashMap();

    /**
     * The UsersStore that contains all UsersRepositories managed by this RemoteManager
     */
    private UsersStore usersStore;

    /**
     * The current UsersRepository being managed/viewed/modified
     */
    private UsersRepository users;

    /**
     * The reference to the spool management service
     */
    private SpoolManagementService spoolManagement;

    /**
     * The service prompt to be displayed when waiting for input.
     */
    private String prompt = "";

    /**
     * The reference to the internal MailServer service
     */
    private MailServer mailServer;

    /**
     * The reference to the Store
     */
    private Store store;
    
    private Command[] commands = {};
    
    /**
     * reference to administration of Bayesian analyzer
     */
    private BayesianAnalyzerManagementService bayesianAnalyzerManagement;
    
    /**
     * reference to administration of Processors
     */
    private ProcessorManagementService processorManagementService;

    private VirtualUserTableManagementService vutManagemenet;
    
    private DomainListManagementService domListManagement;
    
    /**
     * Set the UserStore 
     * 
     * @param usersStore the UserStore
     */
    public void setUsersStore(UsersStore usersStore) {
        this.usersStore = usersStore;
    }

    /**
     * Set the UsersRepository
     * 
     * @param users the UsersRepository
     */
    public void setUsers(UsersRepository users) {
        this.users = users;
    }

    /**
     * Set the SpoolManagementService
     * 
     * @param spoolManagement the SpoolManagementService
     */
    public void setSpoolManagement(SpoolManagementService spoolManagement) {
        this.spoolManagement = spoolManagement;
    }

    /**
     * Set the MailServer 
     * 
     * @param mailServer the MailServer
     */
    public void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }

    /**
     * Set the Store
     * 
     * @param store the Store
     */
    public void setStore(Store store) {
        this.store = store;
    }
    
    /**
     * Set the BayesianAnalyzerManagementService
     * 
     * @param bayesianAnalyzerManagement the BayesianAnalyzerManagementService
     */
    public void setBayesianAnalyzerManagement(BayesianAnalyzerManagementService bayesianAnalyzerManagement) {
        this.bayesianAnalyzerManagement = bayesianAnalyzerManagement;
    }
    
    /**
     * Set the ProcessorManagementService
     * 
     * @param processorManagement the ProcessorManagementService
     */
    public void setProcessorManagement(ProcessorManagementService processorManagement) {
        this.processorManagementService = processorManagement;
    }
    
    /**
     * Set the VirtualUserTableManagementService
     * 
     * @param vutManagement the VirtualUserTableManagementService 
     */
    public void setVirtualUserTableManagement(VirtualUserTableManagementService vutManagement) {
        this.vutManagemenet = vutManagement;
    }
    
    /**
     * Set the DomainListManagementService
     * 
     * @param domListManagement the DomainListManagementService 
     */
    public void setDomainListManagement(DomainListManagementService domListManagement) {
        this.domListManagement = domListManagement;
    }
    
    /**
     * The configuration data to be passed to the handler
     */
    private RemoteManagerHandlerConfigurationData theConfigData
        = new RemoteManagerHandlerConfigurationDataImpl();

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service( final ServiceManager componentManager )
        throws ServiceException {
        super.service(componentManager);
        MailServer mailServer = (MailServer)componentManager.lookup(MailServer.ROLE );
        setMailServer(mailServer);
        Store store = (Store)componentManager.
            lookup( Store.ROLE );
        setStore(store);
        UsersStore usersStore = (UsersStore)componentManager. lookup(UsersStore.ROLE );
        setUsersStore(usersStore);
        UsersRepository users = (UsersRepository) componentManager.lookup(UsersRepository.ROLE);
        if (users == null) {
            throw new ServiceException("","The user repository could not be found.");
        }
        setUsers(users);
        SpoolManagementService spoolManagement = 
            (SpoolManagementService) componentManager.lookup(SpoolManagementService.ROLE);
        setSpoolManagement(spoolManagement);
        
        setBayesianAnalyzerManagement((BayesianAnalyzerManagementService) componentManager.lookup(BayesianAnalyzerManagementService.ROLE));     
        setProcessorManagement((ProcessorManagementService) componentManager.lookup(ProcessorManagementService.ROLE)); 
        setVirtualUserTableManagement((VirtualUserTableManagementService) componentManager.lookup(VirtualUserTableManagementService.ROLE));
        setDomainListManagement((DomainListManagementService) componentManager.lookup(DomainListManagementService.ROLE));
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( final Configuration configuration )
        throws ConfigurationException {

        super.configure(configuration);
        if (isEnabled()) {
            Configuration handlerConfiguration = configuration.getChild("handler");
            Configuration admin = handlerConfiguration.getChild( "administrator_accounts" );
            Configuration[] accounts = admin.getChildren( "account" );
            for ( int i = 0; i < accounts.length; i++ ) {
                adminAccounts.put( accounts[ i ].getAttribute( "login" ),
                                   accounts[ i ].getAttribute( "password" ) );
            }
            Configuration promtConfiguration = handlerConfiguration.getChild("prompt", false);
            if (promtConfiguration != null) prompt = promtConfiguration.getValue();
            if (prompt == null) prompt = ""; 
            else if (!prompt.equals("") && !prompt.endsWith(" ")) prompt += " "; 
            configureCommands(configuration);
        }
    }
  
    private void configureCommands(final Configuration configuration) throws ConfigurationException {
        Collection commands = new ArrayList();
        Configuration[] commandConfigurations = configuration.getChildren( "command" );
        if (commandConfigurations != null) {
            for(int i=0;i<commandConfigurations.length;i++) {
                final Configuration commandConfiguration = commandConfigurations[i];
                Configuration classConfiguration 
                = commandConfiguration.getChild( "class-name" );
                String className = classConfiguration.getValue();
                if (className != null) {
                    try {
                        Command command 
                        = (Command) Class.forName(className).newInstance();
                        if (command instanceof Configurable) {
                            Configurable configurable = (Configurable) command;
                            configurable.configure(commandConfiguration);
                        }
                        commands.add(command);
                    } catch (Exception e) {
                        final Logger logger = getLogger();
                        if (logger != null) {
                            logger.error("Failed to load custom command", e);
                        }
                    }
                }
            }
        }
        this.commands = (Command[]) commands.toArray(this.commands);
    }
    
    /**
     * @see org.apache.james.socket.AbstractProtocolServer#getDefaultPort()
     */
     protected int getDefaultPort() {
        return 4555;
     }

    /**
     * @see org.apache.james.socket.AbstractProtocolServer#getServiceType()
     */
    public String getServiceType() {
        return "Remote Manager Service";
    }
    
    /**
     * @see org.apache.james.socket.AbstractProtocolServer#newProtocolHandlerInstance()
     */
    public ProtocolHandler newProtocolHandlerInstance() {
        return new RemoteManagerHandler(theConfigData);
    }

    /**
     * A class to provide RemoteManager handler configuration to the handlers
     */
    private class RemoteManagerHandlerConfigurationDataImpl
        implements RemoteManagerHandlerConfigurationData {

        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getHelloName()
         */
        public String getHelloName() {
            if (RemoteManager.this.helloName == null) {
                return RemoteManager.this.mailServer.getHelloName();
            } else {
                return RemoteManager.this.helloName;
            }
        }

        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getMailServer()
         */
        public MailServer getMailServer() {
            return RemoteManager.this.mailServer;
        }
        
        /**
         * 
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getStore()
         */
        public Store getStore() {
            return RemoteManager.this.store;
        }
        
        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getUsersRepository()
         */
        public UsersRepository getUsersRepository() {
            return RemoteManager.this.users;
        }

        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getUserStore() 
         */
        public UsersStore getUserStore() {
            return RemoteManager.this.usersStore;
        }

        public SpoolManagementService getSpoolManagement() {
            return RemoteManager.this.spoolManagement;
        }

        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getAdministrativeAccountData()
         */
        public HashMap getAdministrativeAccountData() {
            return RemoteManager.this.adminAccounts;
        }

        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getPrompt()
         */
        public String getPrompt() {
            return RemoteManager.this.prompt;
        }
        
        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getBayesianAnalyzerManagement()
         */
        public BayesianAnalyzerManagementService getBayesianAnalyzerManagement() {
            return RemoteManager.this.bayesianAnalyzerManagement;
        }

        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getProcessorManagement()
         */
        public ProcessorManagementService getProcessorManagement() {
            return RemoteManager.this.processorManagementService;
        }

        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getVirtualUserTableManagement()
         */
        public VirtualUserTableManagementService getVirtualUserTableManagement() {
            return RemoteManager.this.vutManagemenet;
        }

        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getDomainListManagement()
         */
        public DomainListManagementService getDomainListManagement() {
            return RemoteManager.this.domListManagement;
        }
        
        /**
         * @see org.apache.james.neo.remotemanager.RemoteManagerHandlerConfigurationData#getCommands()
         */
        public Command[] getCommands() {
            return RemoteManager.this.commands;
        }
    }
}
