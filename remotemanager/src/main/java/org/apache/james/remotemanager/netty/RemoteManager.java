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

package org.apache.james.remotemanager.netty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.impl.AbstractChannelPipelineFactory;
import org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData;
import org.apache.james.socket.netty.AbstractConfigurableAsyncServer;
import org.apache.james.socket.netty.ConnectionCountHandler;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;


/**
 * NIO RemoteManager which use Netty
 *
 */
public class RemoteManager extends AbstractConfigurableAsyncServer implements RemoteManagerMBean{


    private Map<String,String> adminAccounts = new HashMap<String, String>();
    private RemoteManagerHandlerConfigurationData configData = new RemoteManagerHandlerConfigurationDataImpl();
    private final ConnectionCountHandler countHandler = new ConnectionCountHandler();
    private ProtocolHandlerChain handlerChain;

    public void setProtocolHandlerChain(ProtocolHandlerChain handlerChain) {
        this.handlerChain = handlerChain;
    }
    

    
    @Override
    protected int getDefaultPort() {
        return 4555;
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.socket.ServerMBean#getServiceType()
     */
    public String getServiceType() {
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

    
    /**
     * A class to provide RemoteManager handler configuration to the handlers
     */
    private class RemoteManagerHandlerConfigurationDataImpl
        implements RemoteManagerHandlerConfigurationData {

        /**
         * @see org.apache.james.remotemanager.RemoteManagerHandlerConfigurationData#getHelloName()
         */
        public String getHelloName() {
            return RemoteManager.this.getHelloName();

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
            return "";
        }
        
    }
    
    @Override
    protected ChannelPipelineFactory createPipelineFactory(ChannelGroup group) {
        return new RemoteManagerChannelPipelineFactory(getTimeout(), connectionLimit, connPerIP, group);
    }
    
    private final class RemoteManagerChannelPipelineFactory extends AbstractChannelPipelineFactory {

        public RemoteManagerChannelPipelineFactory(int timeout,
                int maxConnections, int maxConnectsPerIp, ChannelGroup group) {
            super(timeout, maxConnections, maxConnectsPerIp, group);
        }
        
        
        @Override
		public ChannelPipeline getPipeline() throws Exception {
			ChannelPipeline pipeLine =  super.getPipeline();
			pipeLine.addBefore("coreHandler", "countHandler", countHandler);
			return pipeLine;
		}


		@Override
        protected OneToOneEncoder createEncoder() {
            return new RemoteManagerResponseEncoder();
        }

        @Override
        protected ChannelUpstreamHandler createHandler() {
            return new RemoteManagerChannelUpstreamHandler(configData, handlerChain, getLogger());
        }
        
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.socket.ServerMBean#getCurrentConnections()
     */
	public int getCurrentConnections() {
		return countHandler.getCurrentConnectionCount();
	}


    @Override
    protected String getDefaultJMXName() {
        return "remotemanager";
    }

}
