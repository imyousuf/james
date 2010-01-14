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



package org.apache.james.pop3server;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.pop3server.core.CoreCmdHandlerLoader;
import org.apache.james.services.MailServer;
import org.apache.james.socket.api.ProtocolHandler;
import org.apache.james.socket.shared.AbstractSupportLoaderProtocolHandlerFactory;

/**
 * <p>Accepts POP3 connections on a server socket and dispatches them to POP3Handlers.</p>
 *
 * <p>Also responsible for loading and parsing POP3 specific configuration.</p>
 *
 * @version 1.0.0, 24/04/1999
 */
public class POP3ServerProtocolServerHandlerFactory extends AbstractSupportLoaderProtocolHandlerFactory {

    /**
     * The internal mail server service
     */
    private MailServer mailServer;


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


	private boolean useStartTLS;




    @Resource(name="James")
    public void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }

    
    @Override
    protected void onConfigure(final HierarchicalConfiguration configuration) throws ConfigurationException {
        super.onConfigure(configuration);
        HierarchicalConfiguration handlerConfiguration = configuration.configurationAt("handler");
        lengthReset = handlerConfiguration.getInteger("lengthReset", lengthReset);
        if (getLogger().isInfoEnabled()) {
            getLogger().info("The idle timeout will be reset every " + lengthReset + " bytes.");
        }
        
        // TODO: does this belong here ?
        useStartTLS = configuration.getBoolean("startTLS.[@enable]", false);
    }
    /**
     * @see org.apache.james.socket.AbstractProtocolServer#getDefaultPort()
     */
     public int getDefaultPort() {
        return 110;
     }

    /**
     * @see org.apache.james.socket.AbstractProtocolServer#getServiceType()
     */
    public String getServiceType() {
        return "POP3 Service";
    }


    /**
     * @see org.apache.james.socket.AbstractProtocolServer#newProtocolHandlerInstance()
     */
    public ProtocolHandler newProtocolHandlerInstance() {
        //pass the handler chain to every POP3handler
        POP3Handler protocolHandler = new POP3Handler(theConfigData, getProtocolHandlerChain());
        return protocolHandler;
    }

    /**
     * A class to provide POP3 handler configuration to the handlers
     */
    private class POP3HandlerConfigurationDataImpl implements POP3HandlerConfigurationData {

        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#getHelloName()
         */
        public String getHelloName() {
            if (POP3ServerProtocolServerHandlerFactory.this.getHelloName() == null) {
                return POP3ServerProtocolServerHandlerFactory.this.mailServer.getHelloName();
            } else {
                return POP3ServerProtocolServerHandlerFactory.this.getHelloName();
            }
        }

        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#getResetLength()
         */
        public int getResetLength() {
            return POP3ServerProtocolServerHandlerFactory.this.lengthReset;
        }


        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#isStartTLSSupported()
         */
		public boolean isStartTLSSupported() {
			return POP3ServerProtocolServerHandlerFactory.this.useStartTLS;
		}
    }

    @Override
    protected Class<?> getHandlersPackage() {
        return CoreCmdHandlerLoader.class;
    }
}
