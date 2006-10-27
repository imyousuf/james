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
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.core.AbstractJamesService;
import org.apache.james.services.BayesianAnalyzerManagementService;
import org.apache.james.services.MailServer;
import org.apache.james.services.SpoolManagementService;
import org.apache.james.services.UsersStore;
import org.apache.mailet.UsersRepository;

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
    extends AbstractJamesService implements RemoteManagerMBean {

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
    
    private BayesianAnalyzerManagementService bayesianAnalyzerManagement;
    
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
            lookup( "org.apache.avalon.cornerstone.services.store.Store" );
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
        }
    }
  
    /**
     * @see org.apache.james.core.AbstractJamesService#getDefaultPort()
     */
     protected int getDefaultPort() {
        return 4555;
     }

    /**
     * @see org.apache.james.core.AbstractJamesService#getServiceType()
     */
    public String getServiceType() {
        return "Remote Manager Service";
    }
    
    /**
     * @see org.apache.avalon.excalibur.pool.ObjectFactory#newInstance()
     */
    public Object newInstance() throws Exception {
        return new RemoteManagerHandler();
    }
    
    /**
    * @see org.apache.avalon.excalibur.pool.ObjectFactory#getCreatedClass()
    */
    public Class getCreatedClass() {
        return RemoteManagerHandler.class;
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
            return RemoteManager.this.helloName;
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
    }

    /**
     * @see org.apache.james.core.AbstractJamesService#getConfigurationData()
     */
    protected Object getConfigurationData() {
        return theConfigData;
    }
}
