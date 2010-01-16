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

package org.apache.james.remotemanager.mina;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData;
import org.apache.james.remotemanager.RemoteManagerMBean;
import org.apache.james.remotemanager.core.CoreCmdHandlerLoader;
import org.apache.james.remotemanager.mina.filter.RemoteManagerResponseFilter;
import org.apache.james.remotemanager.mina.filter.RemoteManagerValidationFilter;
import org.apache.james.socket.mina.AbstractAsyncServer;
import org.apache.james.socket.shared.ProtocolHandlerChainImpl;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandler;

public class AsyncRemoteManager extends AbstractAsyncServer implements RemoteManagerMBean{

    private HierarchicalConfiguration handlerConfiguration;
    private Map<String,String> adminAccounts = new HashMap<String, String>();
    private ProtocolHandlerChainImpl handlerChain;
    private RemoteManagerHandlerConfigurationData configData = new RemoteManagerHandlerConfigurationDataImpl();
    
    @Override
    protected IoHandler createIoHandler() {
        return new RemoteManagerIoHandler(configData, handlerChain, getLogger());
    }

    @Override
    protected int getDefaultPort() {
        return 4555;
    }

    @Override
    protected String getServiceType() {
        return "plain";
    }

    @Override
    protected void doConfigure(HierarchicalConfiguration config) throws ConfigurationException {
        super.doConfigure(config);
        handlerConfiguration = config.configurationAt("handler");
        List<HierarchicalConfiguration> accounts = handlerConfiguration.configurationsAt("administrator_accounts.account");
        for (int i = 0; i < accounts.size(); i++) {
            adminAccounts.put(accounts.get(i).getString("[@login]"), accounts.get(i).getString("[@password]"));
        }
    }

    
    @Override
    protected void preInit() throws Exception {
        prepareHandlerChain();
    }

    
    protected DefaultIoFilterChainBuilder createIoFilterChainBuilder() {
        DefaultIoFilterChainBuilder builder = super.createIoFilterChainBuilder();
        
        // response and validation filter to the chain
        builder.addLast(RemoteManagerResponseFilter.NAME, new RemoteManagerResponseFilter());
        builder.addLast("requestValidationFilter", new RemoteManagerValidationFilter(getLogger()));
        return builder;
    }
    
    
    private void prepareHandlerChain() throws Exception {
        
        //read from the XML configuration and create and configure each of the handlers
        HierarchicalConfiguration jamesConfiguration = handlerConfiguration.configurationAt("handlerchain");
        if (jamesConfiguration.getString("[@coreHandlersPackage]") == null)
            jamesConfiguration.addProperty("[@coreHandlersPackage]", CoreCmdHandlerLoader.class.getName());
        
        handlerChain = getLoader().load(ProtocolHandlerChainImpl.class, getLogger(), jamesConfiguration);
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.remotemanager.RemoteManagerMBean#getNetworkInterface()
     */
    public String getNetworkInterface() {
        return "unknown";
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.remotemanager.RemoteManagerMBean#getSocketType()
     */
    public String getSocketType() {
        return "plain";
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
                return AsyncRemoteManager.this.getMailServer().getHelloName();
            } else {
                return AsyncRemoteManager.this.getHelloName();
            }
        }
        
        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getAdministrativeAccountData()
         */
        public Map<String,String> getAdministrativeAccountData() {
            return AsyncRemoteManager.this.adminAccounts;
        }

        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getPrompt()
         */
        public String getPrompt() {
            return "";
        }
        
    }

}
