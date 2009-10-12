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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.services.MailServer;
import org.apache.james.socket.AbstractProtocolServer;
import org.apache.james.socket.configuration.JamesConfiguration;
import org.apache.james.socket.shared.ProtocolHandler;

/**
 * Provides a really rude network interface to administer James.
 */
public class RemoteManager
    extends AbstractProtocolServer implements RemoteManagerMBean {

    /**
     * A Map of (user id, passwords) for James administrators
     */
    private Map<String,String> adminAccounts = new HashMap<String,String>();


    /**
     * The service prompt to be displayed when waiting for input.
     */
    private String prompt = "";

    /**
     * The reference to the internal MailServer service
     */
    private MailServer mailServer;


    /**
     * Set the MailServer 
     * 
     * @param mailServer the MailServer
     */
    public void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }

    /**
     * The configuration data to be passed to the handler
     */
    private RemoteManagerHandlerConfigurationData theConfigData
        = new RemoteManagerHandlerConfigurationDataImpl();


    /**
     * The chain to use
     */
    private RemoteManagerHandlerChain handlerChain;


    /**
     * The loader
     */
    private LoaderService loader;


    /**
     * The configuration
     */
    private Configuration handlerConfiguration;
    
    /**
     * Gets the current instance loader.
     * @return the loader
     */
    public final LoaderService getLoader() {
        return loader;
    }

    /**
     * Sets the loader to be used for instances.
     * @param loader the loader to set, not null
     */
    @Resource(name="org.apache.james.LoaderService")
    public final void setLoader(LoaderService loader) {
        this.loader = loader;
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service( final ServiceManager componentManager )
        throws ServiceException {
        super.service(componentManager);
        MailServer mailServer = (MailServer)componentManager.lookup(MailServer.ROLE );
        setMailServer(mailServer);
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
           
            this.handlerConfiguration = handlerConfiguration;
        }
    }
    
    private void prepareHandlerChain() throws Exception {

        handlerChain = loader.load(RemoteManagerHandlerChain.class);
        
        //set the logger
        handlerChain.setLog(new AvalonLogger(getLogger()));
        
        //read from the XML configuration and create and configure each of the handlers
        handlerChain.configure(new JamesConfiguration(handlerConfiguration.getChild("handlerchain")));
    }
    

    @Override
    protected void prepareInit() throws Exception {
        prepareHandlerChain();
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
        return new RemoteManagerHandler(theConfigData, handlerChain);
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
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getAdministrativeAccountData()
         */
        public Map<String,String> getAdministrativeAccountData() {
            return RemoteManager.this.adminAccounts;
        }

        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getPrompt()
         */
        public String getPrompt() {
            return RemoteManager.this.prompt;
        }
        
    }
}
