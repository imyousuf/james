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
package org.apache.james.pop3server.netty;

import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.pop3server.POP3HandlerConfigurationData;
import org.apache.james.pop3server.core.CoreCmdHandlerLoader;
import org.apache.james.pop3server.jmx.JMXHandlersLoader;
import org.apache.james.protocols.api.ProtocolHandlerLoader;
import org.apache.james.protocols.lib.ProtocolHandlerChainImpl;
import org.apache.james.protocols.lib.netty.AbstractConfigurableAsyncServer;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * NIO POP3 Server which use Netty
 */
public class POP3Server extends AbstractConfigurableAsyncServer implements POP3ServerMBean {

    /**
     * The configuration data to be passed to the handler
     */
    private POP3HandlerConfigurationData theConfigData = new POP3HandlerConfigurationDataImpl();

    private ProtocolHandlerChainImpl handlerChain;

    private ProtocolHandlerLoader loader;

    private HierarchicalConfiguration config;

    @Resource(name = "protocolhandlerloader")
    public void setProtocolHandlerLoader(ProtocolHandlerLoader loader) {
        this.loader = loader;
    }
    
    @Override
    protected void preInit() throws Exception {
        super.preInit();
        handlerChain = new ProtocolHandlerChainImpl(loader, config.configurationAt("handlerchain"), jmxName, CoreCmdHandlerLoader.class.getName(), JMXHandlersLoader.class.getName());
        handlerChain.init();
    }

    @Override
    protected void doConfigure(HierarchicalConfiguration config) throws ConfigurationException {
        super.doConfigure(config);
        this.config = config;
    }

    @Override
    protected int getDefaultPort() {
        return 110;
    }
    
    

    @Override
    protected void postDestroy() {
        super.postDestroy();
        handlerChain.destroy();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.socket.ServerMBean#getServiceType()
     */
    public String getServiceType() {
        return "POP3 Service";
    }

    /**
     * A class to provide POP3 handler configuration to the handlers
     */
    private class POP3HandlerConfigurationDataImpl implements POP3HandlerConfigurationData {

        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#getHelloName()
         */
        public String getHelloName() {
            return POP3Server.this.getHelloName();
        }

        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#getResetLength()
         */
        public int getResetLength() {
            return -1;
        }

        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#isStartTLSSupported()
         */
        public boolean isStartTLSSupported() {
            return POP3Server.this.isStartTLSSupported();
        }
    }

    @Override
    protected String getDefaultJMXName() {
        return "pop3server";
    }

    @Override
    protected ChannelUpstreamHandler createCoreHandler() {
        return new POP3ChannelUpstreamHandler(handlerChain, theConfigData, getLogger(), getSSLContext(), getEnabledCipherSuites());
    }

    @Override
    protected OneToOneEncoder createEncoder() {
        return new POP3ResponseEncoder();
    }

}
