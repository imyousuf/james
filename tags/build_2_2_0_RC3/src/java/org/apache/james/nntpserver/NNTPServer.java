/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
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

package org.apache.james.nntpserver;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.excalibur.pool.DefaultPool;
import org.apache.avalon.excalibur.pool.HardResourceLimitingPool;
import org.apache.avalon.excalibur.pool.ObjectFactory;
import org.apache.avalon.excalibur.pool.Pool;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.logger.LogEnabled;

import org.apache.james.core.AbstractJamesService;
import org.apache.james.nntpserver.repository.NNTPRepository;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.james.util.watchdog.Watchdog;
import org.apache.james.util.watchdog.WatchdogFactory;
import org.apache.james.util.watchdog.WatchdogTarget;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * NNTP Server
 *
 */
public class NNTPServer extends AbstractJamesService implements Component, NNTPServerMBean {

    /**
     * Whether authentication is required to access this NNTP server
     */
    private boolean authRequired = false;

    /**
     * The repository that stores the news articles for this NNTP server.
     */
    private NNTPRepository repo;

    /**
     * The repository that stores the local users.  Used for authentication.
     */
    private UsersRepository userRepository = null;

    /**
     * The pool used to provide NNTP Handler objects
     */
    private Pool theHandlerPool = null;

    /**
     * The pool used to provide NNTP Handler objects
     */
    private ObjectFactory theHandlerFactory = new NNTPHandlerFactory();

    /**
     * The factory used to generate Watchdog objects
     */
    private WatchdogFactory theWatchdogFactory;

    /**
     * The configuration data to be passed to the handler
     */
    private NNTPHandlerConfigurationData theConfigData
        = new NNTPHandlerConfigurationDataImpl();

    /**
     * @see org.apache.avalon.framework.component.Composable#compose(ComponentManager)
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentException {
        super.compose(componentManager);
        UsersStore usersStore = (UsersStore)componentManager.lookup(UsersStore.ROLE);
        userRepository = usersStore.getRepository("LocalUsers");
        if (userRepository == null) {
            throw new ComponentException("The user repository could not be found.");
        }

        repo = (NNTPRepository)componentManager
            .lookup("org.apache.james.nntpserver.repository.NNTPRepository");

    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(final Configuration configuration) throws ConfigurationException {
        super.configure(configuration);
        if (isEnabled()) {
            Configuration handlerConfiguration = configuration.getChild("handler");
            authRequired =
                handlerConfiguration.getChild("authRequired").getValueAsBoolean(false);
            if (getLogger().isDebugEnabled()) {
                if (authRequired) {
                    getLogger().debug("NNTP Server requires authentication.");
                } else {
                    getLogger().debug("NNTP Server doesn't require authentication.");
                }
            }
        }
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        super.initialize();
        if (!isEnabled()) {
            return;
        }

        if (connectionLimit != null) {
            theHandlerPool = new HardResourceLimitingPool(theHandlerFactory, 5, connectionLimit.intValue());
            getLogger().debug("Using a bounded pool for NNTP handlers with upper limit " + connectionLimit.intValue());
        } else {
            // NOTE: The maximum here is not a real maximum.  The handler pool will continue to
            //       provide handlers beyond this value.
            theHandlerPool = new DefaultPool(theHandlerFactory, null, 5, 30);
            getLogger().debug("Using an unbounded pool for NNTP handlers.");
        }
        if (theHandlerPool instanceof LogEnabled) {
            ((LogEnabled)theHandlerPool).enableLogging(getLogger());
        }
        if (theHandlerPool instanceof Initializable) {
            ((Initializable)theHandlerPool).initialize();
        }

        theWatchdogFactory = getWatchdogFactory();
    }

    /**
     * @see org.apache.james.core.AbstractJamesService#getDefaultPort()
     */
     protected int getDefaultPort() {
        return 119;
     }

    /**
     * @see org.apache.james.core.AbstractJamesService#getServiceType()
     */
    public String getServiceType() {
        return "NNTP Service";
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.AbstractHandlerFactory#newHandler()
     */
    protected ConnectionHandler newHandler()
            throws Exception {
        NNTPHandler theHandler = (NNTPHandler)theHandlerPool.get();

        Watchdog theWatchdog = theWatchdogFactory.getWatchdog(theHandler.getWatchdogTarget());

        theHandler.setConfigurationData(theConfigData);
        theHandler.setWatchdog(theWatchdog);
        return theHandler;
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory#releaseConnectionHandler(ConnectionHandler)
     */
    public void releaseConnectionHandler( ConnectionHandler connectionHandler ) {
        if (!(connectionHandler instanceof NNTPHandler)) {
            throw new IllegalArgumentException("Attempted to return non-NNTPHandler to pool.");
        }
        theHandlerPool.put((Poolable)connectionHandler);
    }

    /**
     * The factory for producing handlers.
     */
    private static class NNTPHandlerFactory
        implements ObjectFactory {

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#newInstance()
         */
        public Object newInstance() throws Exception {
            return new NNTPHandler();
        }

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#getCreatedClass()
         */
        public Class getCreatedClass() {
            return NNTPHandler.class;
        }

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#decommision(Object)
         */
        public void decommission( Object object ) throws Exception {
            return;
        }
    }

    /**
     * A class to provide NNTP handler configuration to the handlers
     */
    private class NNTPHandlerConfigurationDataImpl
        implements NNTPHandlerConfigurationData {

        /**
         * @see org.apache.james.smtpserver.NNTPHandlerConfigurationData#getHelloName()
         */
        public String getHelloName() {
            return NNTPServer.this.helloName;
        }

        /**
         * @see org.apache.james.smtpserver.NNTPHandlerConfigurationData#isAuthRequired()
         */
        public boolean isAuthRequired() {
            return NNTPServer.this.authRequired;
        }

        /**
         * @see org.apache.james.smtpserver.NNTPHandlerConfigurationData#getUsersRepository()
         */
        public UsersRepository getUsersRepository() {
            return NNTPServer.this.userRepository;
        }

        /**
         * @see org.apache.james.smtpserver.NNTPHandlerConfigurationData#getNNTPRepository()
         */
        public NNTPRepository getNNTPRepository() {
            return NNTPServer.this.repo;
        }

    }
}
