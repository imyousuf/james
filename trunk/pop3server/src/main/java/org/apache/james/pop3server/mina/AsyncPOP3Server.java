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


package org.apache.james.pop3server.mina;

import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.configuration.SubnodeConfiguration;
import org.apache.james.pop3server.POP3HandlerConfigurationData;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3ServerMBean;
import org.apache.james.pop3server.core.CoreCmdHandlerLoader;
import org.apache.james.pop3server.mina.filter.POP3ResponseFilter;
import org.apache.james.socket.ProtocolHandlerChainImpl;
import org.apache.james.socket.mina.AbstractAsyncServer;
import org.apache.james.socket.mina.filter.ResponseValidationFilter;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandler;

/**
 * Async implementation of a POP3Server
 * 
 *
 */
public class AsyncPOP3Server extends AbstractAsyncServer implements POP3ServerMBean{

    /**
     * The number of bytes to read before resetting
     * the connection timeout timer.  Defaults to
     * 20 KB.
     */
    private int lengthReset = 20 * 1024;
    
    /**
     * The configuration data to be passed to the handler
     */
    private POP3HandlerConfigurationData theConfigData
        = new POP3HandlerConfigurationDataImpl();

	private SubnodeConfiguration handlerConfiguration;

	private ProtocolHandlerChainImpl handlerChain;


	@Override
	protected IoHandler createIoHandler() {
		return new POP3IoHandler(handlerChain, theConfigData, getLogger(), getSslContextFactory());
	}

	 /**
     * Prepare the handlerchain
     * 
     * @throws Exception
     */
    private void prepareHandlerChain() throws Exception {
        //read from the XML configuration and create and configure each of the handlers
        HierarchicalConfiguration handlerchainConfig = handlerConfiguration.configurationAt("handlerchain");
        if (handlerchainConfig.getString("[@coreHandlersPackage]") == null)
            handlerchainConfig.addProperty("[@coreHandlersPackage]", CoreCmdHandlerLoader.class.getName());
        
        handlerChain = getLoader().load(ProtocolHandlerChainImpl.class, getLogger(), handlerchainConfig);
        handlerChain.configure(handlerchainConfig);
        
    }


    /**
     * @see org.apache.james.socket.mina.AbstractAsyncServer#preInit()
     */
    protected void preInit() throws Exception {
        prepareHandlerChain();
    }
    
	@Override
	protected int getDefaultPort() {
		return 110;
	}

	@Override
	protected String getServiceType() {
        return "POP3 Service";
	}

	 
    @Override
    protected void doConfigure(final HierarchicalConfiguration configuration) throws ConfigurationException {
        super.doConfigure(configuration);
        handlerConfiguration = configuration.configurationAt("handler");
        lengthReset = handlerConfiguration.getInteger("lengthReset", lengthReset);
        if (getLogger().isInfoEnabled()) {
            getLogger().info("The idle timeout will be reset every " + lengthReset + " bytes.");
        }
    }
    
    /**
     * @see org.apache.james.socket.mina.AbstractAsyncServer#createIoFilterChainBuilder()
     */
    protected DefaultIoFilterChainBuilder createIoFilterChainBuilder() {
        DefaultIoFilterChainBuilder builder = super.createIoFilterChainBuilder();
        
        // response and validation filter to the chain
        builder.addLast("pop3ResponseFilter", new POP3ResponseFilter());
        builder.addLast("responseValidationFilter", new ResponseValidationFilter<POP3Response>(getLogger(), POP3Response.class));
        return builder;
    }

    /**
     * A class to provide POP3 handler configuration to the handlers
     */
    private class POP3HandlerConfigurationDataImpl implements POP3HandlerConfigurationData {

        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#getHelloName()
         */
        public String getHelloName() {
            if (AsyncPOP3Server.this.getHelloName() == null) {
                return AsyncPOP3Server.this.getMailServer().getHelloName();
            } else {
                return AsyncPOP3Server.this.getHelloName();
            }
        }

        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#getResetLength()
         */
        public int getResetLength() {
            return AsyncPOP3Server.this.lengthReset;
        }


        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#isStartTLSSupported()
         */
		public boolean isStartTLSSupported() {
			return AsyncPOP3Server.this.isStartTLSSupported();
		}
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.pop3server.POP3ServerMBean#getNetworkInterface()
     */
	public String getNetworkInterface() {
		return "unkown";
	}

	/*
	 * (non-Javadoc)
	 * @see org.apache.james.pop3server.POP3ServerMBean#getSocketType()
	 */
	public String getSocketType() {
	    if (isSSLSocket()) {
	        return "secure";
	    }
		return "plain";
	}



}
