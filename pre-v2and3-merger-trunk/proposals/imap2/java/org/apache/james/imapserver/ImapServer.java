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

package org.apache.james.imapserver;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.excalibur.pool.DefaultPool;
import org.apache.avalon.excalibur.pool.HardResourceLimitingPool;
import org.apache.avalon.excalibur.pool.ObjectFactory;
import org.apache.avalon.excalibur.pool.Pool;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.core.AbstractJamesService;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersStore;
import org.apache.james.util.watchdog.Watchdog;
import org.apache.james.util.watchdog.WatchdogFactory;
import org.apache.mailet.UsersRepository;

/**
 * TODO: this is a quick cut-and-paste hack from POP3Server. Should probably be
 * rewritten from scratch, together with ImapHandler.
 *
 * <p>Accepts IMAP connections on a server socket and dispatches them to IMAPHandlers.</p>
 *
 * <p>Also responsible for loading and parsing IMAP specific configuration.</p>
 *
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author  <a href="mailto:danny@apache.org">Danny Angus</a>
 * @author Peter M. Goldstein <farsight@alum.mit.edu>
 * @author Darrell DeBoer <darrell@apache.org>
 */
public class ImapServer extends AbstractJamesService
{

    /**
     * The internal mail server service
     */
    private MailServer mailServer;

    /**
     * The user repository for this server - used to authenticate users.
     */
    private UsersRepository users;

    /**
     * The ImapHost for this server - used for all mail storage.
     */
    private ImapHost imapHost;

    /**
     * The number of bytes to read before resetting
     * the connection timeout timer.  Defaults to
     * 20 KB.
     */
    private int lengthReset = 20 * 1024;

    /**
     * The pool used to provide IMAP Handler objects
     */
    private Pool theHandlerPool = null;

    /**
     * The factory used to provide IMAP Handler objects
     */
    private ObjectFactory theHandlerFactory = new IMAPHandlerFactory();

    /**
     * The factory used to generate Watchdog objects
     */
    private WatchdogFactory theWatchdogFactory;

    /**
     * The configuration data to be passed to the handler
     */
    private ImapHandlerConfigurationData theConfigData
            = new IMAPHandlerConfigurationDataImpl();

    public void service( ServiceManager manager ) throws ServiceException
    {
        super.service( manager );
        mailServer = ( MailServer ) manager.
                lookup( "org.apache.james.services.MailServer" );
        UsersStore usersStore = ( UsersStore ) manager.
                lookup( "org.apache.james.services.UsersStore" );
        users = usersStore.getRepository( "LocalUsers" );
        imapHost = ( ImapHost ) manager.
                lookup( "org.apache.james.imapserver.ImapHost" );
        if ( users == null ) {
            throw new ServiceException( "The user repository could not be found." );
        }
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( final Configuration configuration ) throws ConfigurationException
    {
        super.configure( configuration );
        if ( isEnabled() ) {
            Configuration handlerConfiguration = configuration.getChild( "handler" );
            lengthReset = handlerConfiguration.getChild( "lengthReset" ).getValueAsInteger( lengthReset );
            if ( getLogger().isInfoEnabled() ) {
                getLogger().info( "The idle timeout will be reset every " + lengthReset + " bytes." );
            }
        }
    }

    /**
     * @see Initializable#initialize()
     */
    public void initialize() throws Exception
    {
        super.initialize();
        if ( !isEnabled() ) {
            return;
        }

        if ( connectionLimit != null ) {
            theHandlerPool = new HardResourceLimitingPool( theHandlerFactory, 5, connectionLimit.intValue() );
            getLogger().debug( "Using a bounded pool for IMAP handlers with upper limit " + connectionLimit.intValue() );
        }
        else {
            // NOTE: The maximum here is not a real maximum.  The handler pool will continue to
            //       provide handlers beyond this value.
            theHandlerPool = new DefaultPool( theHandlerFactory, null, 5, 30 );
            getLogger().debug( "Using an unbounded pool for IMAP handlers." );
        }
        if ( theHandlerPool instanceof LogEnabled ) {
            ( ( LogEnabled ) theHandlerPool ).enableLogging( getLogger() );
        }
        if ( theHandlerPool instanceof Initializable ) {
            ( ( Initializable ) theHandlerPool ).initialize();
        }

        theWatchdogFactory = getWatchdogFactory();
    }

    /**
     * @see AbstractJamesService#getDefaultPort()
     */
    protected int getDefaultPort()
    {
        return 110;
    }

    /**
     * @see AbstractJamesService#getServiceType()
     */
    public String getServiceType()
    {
        return "IMAP Service";
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.AbstractHandlerFactory#newHandler()
     */
    protected ConnectionHandler newHandler()
            throws Exception
    {
        ImapHandler theHandler = ( ImapHandler ) theHandlerPool.get();

        Watchdog theWatchdog = theWatchdogFactory.getWatchdog( theHandler.getWatchdogTarget() );

        theHandler.setConfigurationData( theConfigData );

        theHandler.setWatchdog( theWatchdog );

        return theHandler;
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory#releaseConnectionHandler(ConnectionHandler)
     */
    public void releaseConnectionHandler( ConnectionHandler connectionHandler )
    {
        if ( !( connectionHandler instanceof ImapHandler ) ) {
            throw new IllegalArgumentException( "Attempted to return non-ImapHandler to pool." );
        }
        theHandlerPool.put( ( Poolable ) connectionHandler );
    }

    /**
     * The factory for producing handlers.
     */
    private static class IMAPHandlerFactory
            implements ObjectFactory
    {

        /**
         * @see ObjectFactory#newInstance()
         */
        public Object newInstance() throws Exception
        {
            return new ImapHandler();
        }

        /**
         * @see ObjectFactory#getCreatedClass()
         */
        public Class getCreatedClass()
        {
            return ImapHandler.class;
        }

        /**
         * @see ObjectFactory#decommission(Object)
         */
        public void decommission( Object object ) throws Exception
        {
            return;
        }
    }

    /**
     * A class to provide POP3 handler configuration to the handlers
     */
    private class IMAPHandlerConfigurationDataImpl
            implements ImapHandlerConfigurationData
    {

        /**
         * @see ImapHandlerConfigurationData#getHelloName()
         */
        public String getHelloName()
        {
            return ImapServer.this.helloName;
        }

        /**
         * @see ImapHandlerConfigurationData#getResetLength()
         */
        public int getResetLength()
        {
            return ImapServer.this.lengthReset;
        }

        /**
         * @see ImapHandlerConfigurationData#getMailServer()
         */
        public MailServer getMailServer()
        {
            return ImapServer.this.mailServer;
        }

        /**
         * @see ImapHandlerConfigurationData#getUsersRepository()
         */
        public UsersRepository getUsersRepository()
        {
            return ImapServer.this.users;
        }

        /** @see ImapHandlerConfigurationData#getImapHost */
        public ImapHost getImapHost()
        {
            return ImapServer.this.imapHost;
        }
    }
}
