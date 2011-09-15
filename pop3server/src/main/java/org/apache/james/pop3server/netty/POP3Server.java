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

import org.apache.james.pop3server.POP3HandlerConfigurationData;
import org.apache.james.pop3server.core.CoreCmdHandlerLoader;
import org.apache.james.pop3server.jmx.JMXHandlersLoader;
import org.apache.james.protocols.api.HandlersPackage;
import org.apache.james.protocols.lib.netty.AbstractProtocolAsyncServer;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;

/**
 * NIO POP3 Server which use Netty
 */
public class POP3Server extends AbstractProtocolAsyncServer implements POP3ServerMBean {

    private final static POP3ResponseEncoder POP3_RESPONSE_ENCODER =  new POP3ResponseEncoder();
    /**
     * The configuration data to be passed to the handler
     */
    private POP3HandlerConfigurationData theConfigData = new POP3HandlerConfigurationDataImpl();

    @Override
    protected int getDefaultPort() {
        return 110;
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
        return new POP3ChannelUpstreamHandler(getProtocolHandlerChain(), theConfigData, getLogger(), getSSLContext(), getEnabledCipherSuites());
    }

    @Override
    protected OneToOneEncoder createEncoder() {
        return POP3_RESPONSE_ENCODER;
    }


    @Override
    protected Class<? extends HandlersPackage> getCoreHandlersPackage() {
        return CoreCmdHandlerLoader.class;
    }


    @Override
    protected Class<? extends HandlersPackage> getJMXHandlersPackage() {
        return JMXHandlersLoader.class;
    }
}
