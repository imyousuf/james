/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.pop3server;

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
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.james.util.watchdog.Watchdog;
import org.apache.james.util.watchdog.WatchdogFactory;
import org.apache.james.util.watchdog.WatchdogTarget;

import java.net.InetAddress;
import java.net.UnknownHostException;
/**
 * <p>Accepts POP3 connections on a server socket and dispatches them to POP3Handlers.</p>
 *
 * <p>Also responsible for loading and parsing POP3 specific configuration.</p>
 *
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author  <a href="mailto:danny@apache.org">Danny Angus</a>
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
public class POP3Server extends AbstractJamesService implements Component {

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
     * The pool used to provide POP3 Handler objects
     */
    private Pool theHandlerPool = null;

    /**
     * The factory used to provide POP3 Handler objects
     */
    private ObjectFactory theHandlerFactory = new POP3HandlerFactory();

    /**
     * The factory used to generate Watchdog objects
     */
    private WatchdogFactory theWatchdogFactory;

    /**
     * The configuration data to be passed to the handler
     */
    private POP3HandlerConfigurationData theConfigData
        = new POP3HandlerConfigurationDataImpl();

    /**
     * @see org.apache.avalon.framework.component.Composable#compose(ComponentManager)
     */
    public void compose( final ComponentManager componentManager )
        throws ComponentException {
        super.compose(componentManager);
        mailServer = (MailServer)componentManager.
            lookup( "org.apache.james.services.MailServer" );
        UsersStore usersStore = (UsersStore)componentManager.
            lookup( "org.apache.james.services.UsersStore" );
        users = usersStore.getRepository("LocalUsers");
        if (users == null) {
            throw new ComponentException("The user repository could not be found.");
        }
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
            getLogger().debug("Using a bounded pool for POP3 handlers with upper limit " + connectionLimit.intValue());
        } else {
            // NOTE: The maximum here is not a real maximum.  The handler pool will continue to
            //       provide handlers beyond this value.
            theHandlerPool = new DefaultPool(theHandlerFactory, null, 5, 30);
            getLogger().debug("Using an unbounded pool for POP3 handlers.");
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
