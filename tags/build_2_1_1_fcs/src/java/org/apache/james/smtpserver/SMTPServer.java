/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.smtpserver;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.excalibur.pool.DefaultPool;
import org.apache.avalon.excalibur.pool.HardResourceLimitingPool;
import org.apache.avalon.excalibur.pool.ObjectFactory;
import org.apache.avalon.excalibur.pool.Pool;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.james.Constants;
import org.apache.james.core.AbstractJamesService;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.james.util.watchdog.Watchdog;
import org.apache.james.util.watchdog.WatchdogFactory;
import org.apache.james.util.watchdog.WatchdogTarget;
import org.apache.mailet.MailetContext;
import java.net.InetAddress;
import java.net.UnknownHostException;
/**
 * <p>Accepts SMTP connections on a server socket and dispatches them to SMTPHandlers.</p>
 *
 * <p>Also responsible for loading and parsing SMTP specific configuration.</p>
 *
 * @version 1.1.0, 06/02/2001
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author  Matthew Pangaro <mattp@lokitech.com>
 * @author  <a href="mailto:donaldp@apache.org">Peter Donald</a>
 * @author  <a href="mailto:danny@apache.org">Danny Angus</a>
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 */
/*
 * IMPORTANT: SMTPServer extends AbstractJamesService.  If you implement ANY
 * lifecycle methods, you MUST call super.<method> as well.
 */
public class SMTPServer extends AbstractJamesService implements Component {

    /**
     * The mailet context - we access it here to set the hello name for the Mailet API
     */
    MailetContext mailetcontext;

    /**
     * The user repository for this server - used to authenticate
     * users.
     */
    private UsersRepository users;

    /**
     * The internal mail server service.
     */
    private MailServer mailServer;

    /**
     * Whether authentication is required to use
     * this SMTP server.
     */
    private boolean authRequired = false;

    /**
     * Whether the server verifies that the user
     * actually sending an email matches the
     * authentication credentials attached to the
     * SMTP interaction.
     */
    private boolean verifyIdentity = false;

    /**
     * The maximum message size allowed by this SMTP server.  The default
     * value, 0, means no limit.
     */
    private long maxMessageSize = 0;

    /**
     * The number of bytes to read before resetting
     * the connection timeout timer.  Defaults to
     * 20 KB.
     */
    private int lengthReset = 20 * 1024;

    /**
     * The pool used to provide SMTP Handler objects
     */
    private Pool theHandlerPool = null;

    /**
     * The pool used to provide SMTP Handler objects
     */
    private ObjectFactory theHandlerFactory = new SMTPHandlerFactory();

    /**
     * The factory used to generate Watchdog objects
     */
    private WatchdogFactory theWatchdogFactory;

    /**
     * The configuration data to be passed to the handler
     */
    private SMTPHandlerConfigurationData theConfigData
        = new SMTPHandlerConfigurationDataImpl();

    /**
     * @see org.apache.avalon.framework.component.Composable#compose(ComponentManager)
     */
    public void compose(final ComponentManager componentManager) throws ComponentException {
        super.compose(componentManager);
        mailetcontext = (MailetContext) componentManager.lookup("org.apache.mailet.MailetContext");
        mailServer = (MailServer) componentManager.lookup("org.apache.james.services.MailServer");
        UsersStore usersStore =
            (UsersStore) componentManager.lookup("org.apache.james.services.UsersStore");
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
            mailetcontext.setAttribute(Constants.HELLO_NAME, helloName);
            Configuration handlerConfiguration = configuration.getChild("handler");
            authRequired = handlerConfiguration.getChild("authRequired").getValueAsBoolean(false);
            verifyIdentity = handlerConfiguration.getChild("verifyIdentity").getValueAsBoolean(false);
            if (authRequired) {
                if (verifyIdentity) {
                    getLogger().info("This SMTP server requires authentication and verifies that the authentication credentials match the sender address.");
                } else {
                    getLogger().info("This SMTP server requires authentication, but doesn't verify that the authentication credentials match the sender address.");
                }
            } else {
                getLogger().info("This SMTP server does not require authentication.");
            }
            // get the message size limit from the conf file and multiply
            // by 1024, to put it in bytes
            maxMessageSize = handlerConfiguration.getChild( "maxmessagesize" ).getValueAsLong( maxMessageSize ) * 1024;
            if (maxMessageSize > 0) {
                getLogger().info("The maximum allowed message size is " + maxMessageSize + " bytes.");
            } else {
                getLogger().info("No maximum message size is enforced for this server.");
            }
            // How many bytes to read before updating the timer that data is being transfered
            lengthReset = configuration.getChild("lengthReset").getValueAsInteger(lengthReset);
            if (lengthReset <= 0) {
                throw new ConfigurationException("The configured value for the idle timeout reset, " + lengthReset + ", is not valid.");
            }
            if (getLogger().isInfoEnabled()) {
                getLogger().info("The idle timeout will be reset every " + lengthReset + " bytes.");
            }
        } else {
            mailetcontext.setAttribute(Constants.HELLO_NAME, "localhost");
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
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Using a bounded pool for SMTP handlers with upper limit " + connectionLimit.intValue());
            }
        } else {
            // NOTE: The maximum here is not a real maximum.  The handler pool will continue to
            //       provide handlers beyond this value.
            theHandlerPool = new DefaultPool(theHandlerFactory, null, 5, 30);
            getLogger().debug("Using an unbounded pool for SMTP handlers.");
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
        return 25;
     }

    /**
     * @see org.apache.james.core.AbstractJamesService#getServiceType()
     */
    public String getServiceType() {
        return "SMTP Service";
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.AbstractHandlerFactory#newHandler()
     */
    protected ConnectionHandler newHandler()
            throws Exception {
        SMTPHandler theHandler = (SMTPHandler)theHandlerPool.get();

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Getting SMTPHandler from pool.");
        }
        Watchdog theWatchdog = theWatchdogFactory.getWatchdog(theHandler.getWatchdogTarget());

        theHandler.setConfigurationData(theConfigData);

        theHandler.setWatchdog(theWatchdog);
        return theHandler;
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory#releaseConnectionHandler(ConnectionHandler)
     */
    public void releaseConnectionHandler( ConnectionHandler connectionHandler ) {
        if (!(connectionHandler instanceof SMTPHandler)) {
            throw new IllegalArgumentException("Attempted to return non-SMTPHandler to pool.");
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Returning SMTPHandler to pool.");
        }
        theHandlerPool.put((Poolable)connectionHandler);
    }

    /**
     * The factory for producing handlers.
     */
    private static class SMTPHandlerFactory
        implements ObjectFactory {

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#newInstance()
         */
        public Object newInstance() throws Exception {
            return new SMTPHandler();
        }

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#getCreatedClass()
         */
        public Class getCreatedClass() {
            return SMTPHandler.class;
        }

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#decommision(Object)
         */
        public void decommission( Object object ) throws Exception {
            return;
        }
    }

    /**
     * A class to provide SMTP handler configuration to the handlers
     */
    private class SMTPHandlerConfigurationDataImpl
        implements SMTPHandlerConfigurationData {

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#getHelloName()
         */
        public String getHelloName() {
            return SMTPServer.this.helloName;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#getResetLength()
         */
        public int getResetLength() {
            return SMTPServer.this.lengthReset;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#getMaxMessageSize()
         */
        public long getMaxMessageSize() {
            return SMTPServer.this.maxMessageSize;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#isAuthRequired()
         */
        public boolean isAuthRequired() {
            return SMTPServer.this.authRequired;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#isVerifyIdentity()
         */
        public boolean isVerifyIdentity() {
            return SMTPServer.this.verifyIdentity;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#getMailServer()
         */
        public MailServer getMailServer() {
            return SMTPServer.this.mailServer;
        }

        /**
         * @see org.apache.james.smtpserver.SMTPHandlerConfigurationData#getUsersRepository()
         */
        public UsersRepository getUsersRepository() {
            return SMTPServer.this.users;
        }
    }
}
