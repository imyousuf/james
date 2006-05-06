/***********************************************************************
 * Copyright (c) 1999-2006 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.pop3server;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.excalibur.pool.ObjectFactory;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.core.AbstractJamesService;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.util.watchdog.Watchdog;

/**
 * <p>Accepts POP3 connections on a server socket and dispatches them to POP3Handlers.</p>
 *
 * <p>Also responsible for loading and parsing POP3 specific configuration.</p>
 *
 * @version 1.0.0, 24/04/1999
 */
public class POP3Server extends AbstractJamesService implements POP3ServerMBean {

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

    /**
     * @see org.apache.avalon.framework.service.Serviceable#compose(ServiceManager)
     */
    public void service( final ServiceManager componentManager )
        throws ServiceException {
        super.service(componentManager);
        mailServer = (MailServer)componentManager.lookup( MailServer.ROLE );
        users = (UsersRepository)componentManager.lookup( UsersRepository.ROLE );
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
        }
        theHandlerFactory = new POP3HandlerFactory();
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
     * @see org.apache.avalon.cornerstone.services.connection.AbstractHandlerFactory#newHandler()
     */
    protected ConnectionHandler newHandler()
            throws Exception {
        POP3Handler theHandler = (POP3Handler)theHandlerPool.get();

        Watchdog theWatchdog = theWatchdogFactory.getWatchdog(theHandler.getWatchdogTarget());

        theHandler.setConfigurationData(theConfigData);

        theHandler.setWatchdog(theWatchdog);

        return theHandler;
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory#releaseConnectionHandler(ConnectionHandler)
     */
    public void releaseConnectionHandler( ConnectionHandler connectionHandler ) {
        if (!(connectionHandler instanceof POP3Handler)) {
            throw new IllegalArgumentException("Attempted to return non-POP3Handler to pool.");
        }
        theHandlerPool.put((Poolable)connectionHandler);
    }

    /**
     * The factory for producing handlers.
     */
    private static class POP3HandlerFactory
        implements ObjectFactory {

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#newInstance()
         */
        public Object newInstance() throws Exception {
            return new POP3Handler();
        }

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#getCreatedClass()
         */
        public Class getCreatedClass() {
            return POP3Handler.class;
        }

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#decommision(Object)
         */
        public void decommission( Object object ) throws Exception {
            return;
        }
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
            return POP3Server.this.helloName;
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
}
