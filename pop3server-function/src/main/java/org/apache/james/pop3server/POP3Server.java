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

import javax.annotation.Resource;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.api.kernel.LoaderService;
import org.apache.james.pop3server.core.CoreCmdHandlerLoader;
import org.apache.james.services.MailServer;
import org.apache.james.socket.AbstractProtocolServer;
import org.apache.james.socket.api.ProtocolHandler;
import org.apache.james.socket.shared.ProtocolHandlerChainImpl;
import org.apache.james.util.ConfigurationConverter;

/**
 * <p>Accepts POP3 connections on a server socket and dispatches them to POP3Handlers.</p>
 *
 * <p>Also responsible for loading and parsing POP3 specific configuration.</p>
 *
 * @version 1.0.0, 24/04/1999
 */
public class POP3Server extends AbstractProtocolServer implements POP3ServerMBean {

    /**
     * The handler chain - POP3handlers can lookup handlerchain to obtain
     * Command handlers , Message handlers and connection handlers
     */
    private ProtocolHandlerChainImpl handlerChain;

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


    private LoaderService loader;

    private Configuration handlerConfiguration;

    public void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }

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
        MailServer mailServer = (MailServer)componentManager.lookup( MailServer.ROLE );
        setMailServer(mailServer);
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(final Configuration configuration) throws ConfigurationException {
        super.configure(configuration);
        if (isEnabled()) {
            Configuration handlerConfiguration = configuration.getChild("handler");
            lengthReset = handlerConfiguration.getChild("lengthReset").getValueAsInteger(lengthReset);
            if (getLogger().isInfoEnabled()) {
                getLogger().info("The idle timeout will be reset every " + lengthReset + " bytes.");
            }
            
            this.handlerConfiguration = handlerConfiguration;
        }
    }
    
    private void prepareHandlerChain() throws Exception {

        handlerChain = loader.load(ProtocolHandlerChainImpl.class);
        
        //set the logger
        handlerChain.setLog(new AvalonLogger(getLogger()));
        
        //read from the XML configuration and create and configure each of the handlers
        ConfigurationConverter jamesConfiguration = new ConfigurationConverter(handlerConfiguration.getChild("handlerchain"));
        if (jamesConfiguration.getString("@coreHandlersPackage") == null)
            jamesConfiguration.addProperty("/ @coreHandlersPackage", CoreCmdHandlerLoader.class.getName());
        handlerChain.configure(jamesConfiguration);
    }

    @Override
    protected void prepareInit() throws Exception {
        prepareHandlerChain();
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
        POP3Handler protocolHandler = new POP3Handler(theConfigData, handlerChain);
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
            if (POP3Server.this.getHelloName() == null) {
                return POP3Server.this.mailServer.getHelloName();
            } else {
                return POP3Server.this.getHelloName();
            }
        }

        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#getResetLength()
         */
        public int getResetLength() {
            return POP3Server.this.lengthReset;
        }


        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#isStartTLSSupported()
         */
		public boolean isStartTLSSupported() {
			return POP3Server.this.useStartTLS();
		}
    }
}
