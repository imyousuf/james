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
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.remotemanager.core.CoreCmdHandlerLoader;
import org.apache.james.services.MailServer;
import org.apache.james.socket.api.ProtocolHandler;
import org.apache.james.socket.shared.AbstractSupportLoaderProtocolHandlerFactory;

/**
 * Provides a really rude network interface to administer James.
 */
public class RemoteManagerProtocolHandlerFactory extends AbstractSupportLoaderProtocolHandlerFactory {

    
    /**
     * A Map of (user id, passwords) for James administrators
     */
    private Map<String,String> adminAccounts = new HashMap<String,String>();


    /**
     * The service prompt to be displayed when waiting for input.
     */
    private String prompt = "";


    
    @Resource(name="James")
    public final void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }

    
    /**
     * The configuration data to be passed to the handler
     */
    private RemoteManagerHandlerConfigurationData theConfigData
        = new RemoteManagerHandlerConfigurationDataImpl();

    private MailServer mailServer;

    @SuppressWarnings("unchecked")
    public void onConfigure(HierarchicalConfiguration configuration)
        throws ConfigurationException {
        super.onConfigure(configuration);
        HierarchicalConfiguration handlerConfiguration = configuration.configurationAt("handler");
        List<HierarchicalConfiguration> accounts = handlerConfiguration.configurationsAt("administrator_accounts.account");
        for (int i = 0; i < accounts.size(); i++) {
            adminAccounts.put(accounts.get(i).getString("[@login]"), accounts.get(i).getString("[@password]"));
        }
        prompt = handlerConfiguration.getString("prompt", null);

        if (prompt == null)
            prompt = "";
        else if (!prompt.equals("") && !prompt.endsWith(" "))
            prompt += " ";
                
    }
    


    /**
     * @see org.apache.james.socket.AbstractProtocolServer#getDefaultPort()
     */
     public int getDefaultPort() {
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
        return new RemoteManagerHandler(theConfigData, getProtocolHandlerChain());
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
            if (getHelloName() == null) {
                return RemoteManagerProtocolHandlerFactory.this.mailServer.getHelloName();
            } else {
                return RemoteManagerProtocolHandlerFactory.this.getHelloName();
            }
        }
        
        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getAdministrativeAccountData()
         */
        public Map<String,String> getAdministrativeAccountData() {
            return RemoteManagerProtocolHandlerFactory.this.adminAccounts;
        }

        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getPrompt()
         */
        public String getPrompt() {
            return RemoteManagerProtocolHandlerFactory.this.prompt;
        }
        
    }

    @Override
    protected Class<?> getHandlersPackage() {
        return CoreCmdHandlerLoader.class;
    }
}
