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
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData;
import org.apache.james.remotemanager.RemoteManagerMBean;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.mina.filter.RemoteManagerResponseFilter;
import org.apache.james.socket.mina.AbstractAsyncServer;
import org.apache.james.socket.mina.filter.ResponseValidationFilter;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandler;

public class AsyncRemoteManager extends AbstractAsyncServer implements RemoteManagerMBean{

    private Map<String,String> adminAccounts = new HashMap<String, String>();
    private ProtocolHandlerChain handlerChain;
    private RemoteManagerHandlerConfigurationData configData = new RemoteManagerHandlerConfigurationDataImpl();
    
    public void setProtocolHandlerChain(ProtocolHandlerChain handlerChain) {
        this.handlerChain = handlerChain;
    }
    
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
        return "RemoteManager Service";
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void doConfigure(HierarchicalConfiguration config) throws ConfigurationException {
        super.doConfigure(config);
        HierarchicalConfiguration handlerConfiguration = config.configurationAt("handler");
        List<HierarchicalConfiguration> accounts = handlerConfiguration.configurationsAt("administrator_accounts.account");
        for (int i = 0; i < accounts.size(); i++) {
            adminAccounts.put(accounts.get(i).getString("[@login]"), accounts.get(i).getString("[@password]"));
        }
    }

    protected DefaultIoFilterChainBuilder createIoFilterChainBuilder() {
        DefaultIoFilterChainBuilder builder = super.createIoFilterChainBuilder();
        
        // response and validation filter to the chain
        builder.addLast(RemoteManagerResponseFilter.NAME, new RemoteManagerResponseFilter());
        builder.addLast("requestValidationFilter", new ResponseValidationFilter<RemoteManagerResponse>(getLogger(),RemoteManagerResponse.class));
        return builder;
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
