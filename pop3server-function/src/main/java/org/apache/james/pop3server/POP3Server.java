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

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.core.AbstractJamesService;
import org.apache.james.services.MailServer;

/**
 * <p>Accepts POP3 connections on a server socket and dispatches them to POP3Handlers.</p>
 *
 * <p>Also responsible for loading and parsing POP3 specific configuration.</p>
 *
 * @version 1.0.0, 24/04/1999
 */
public class POP3Server extends AbstractJamesService implements POP3ServerMBean {

    /**
     * The handler chain - POP3handlers can lookup handlerchain to obtain
     * Command handlers , Message handlers and connection handlers
     */
    POP3HandlerChain handlerChain = new POP3HandlerChain();

    /**
     * The internal mail server service
     */
    private MailServer mailServer;

    /**
     * The user repository for this server - used to authenticate users.
     */
    private UsersRepository users;

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

    private ServiceManager serviceManager;

    public void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }

    public void setUsers(UsersRepository users) {
        this.users = users;
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager) 
     */
    public void service( final ServiceManager componentManager )
        throws ServiceException {
        super.service(componentManager);
        serviceManager = componentManager;
        MailServer mailServer = (MailServer)componentManager.lookup( MailServer.ROLE );
        setMailServer(mailServer);
        UsersRepository users = (UsersRepository)componentManager.lookup( UsersRepository.ROLE );
        setUsers(users);
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
            //set the logger
            ContainerUtil.enableLogging(handlerChain,getLogger());

            try {
                ContainerUtil.service(handlerChain,serviceManager);
            } catch (ServiceException e) {
                if (getLogger().isErrorEnabled()) {
                    getLogger().error("Failed to service handlerChain",e);
                }
                throw new ConfigurationException("Failed to service handlerChain");
            }
            
            //read from the XML configuration and create and configure each of the handlers
            ContainerUtil.configure(handlerChain,handlerConfiguration.getChild("handlerchain"));

        }
    }

    /**
     * @see org.apache.james.core.AbstractJamesService#getDefaultPort()
     */
     protected int getDefaultPort() {
        return 110;
     }

    /**
     * @see org.apache.james.core.AbstractJamesService#getServiceType()
     */
    public String getServiceType() {
        return "POP3 Service";
    }


    /**
     * @see org.apache.avalon.excalibur.pool.ObjectFactory#newInstance()
     */
     public Object newInstance() throws Exception {
        POP3Handler theHandler = new POP3Handler();
        
        //pass the handler chain to every POP3handler
        theHandler.setHandlerChain(handlerChain);

        return theHandler;
    }

    /**
     * @see org.apache.avalon.excalibur.pool.ObjectFactory#getCreatedClass()
     */
     public Class getCreatedClass() {
         return POP3Handler.class;
     }

    /**
     * A class to provide POP3 handler configuration to the handlers
     */
    private class POP3HandlerConfigurationDataImpl
        implements POP3HandlerConfigurationData {

        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#getHelloName()
         */
        public String getHelloName() {
            if (POP3Server.this.helloName == null) {
                return POP3Server.this.mailServer.getHelloName();
            } else {
                return POP3Server.this.helloName;
            }
        }

        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#getResetLength()
         */
        public int getResetLength() {
            return POP3Server.this.lengthReset;
        }

        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#getMailServer()
         */
        public MailServer getMailServer() {
            return POP3Server.this.mailServer;
        }

        /**
         * @see org.apache.james.pop3server.POP3HandlerConfigurationData#getUsersRepository()
         */
        public UsersRepository getUsersRepository() {
            return POP3Server.this.users;
        }
    }

    /**
     * @see org.apache.james.core.AbstractJamesService#getConfigurationData()
     */
    protected Object getConfigurationData() {
        return theConfigData;
    }
}
