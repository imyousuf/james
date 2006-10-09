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

package org.apache.james.imapserver;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.excalibur.pool.ObjectFactory;
import org.apache.avalon.excalibur.pool.Pool;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.core.AbstractJamesService;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.services.UsersRepository;
import org.apache.james.util.watchdog.Watchdog;
import org.apache.james.util.watchdog.WatchdogFactory;

/**
 * TODO: this is a quick cut-and-paste hack from POP3Server. Should probably be
 * rewritten from scratch, together with ImapHandler.
 * TODO: Using a custom, not AbstractJamesHandler, handler together with AbstractJamesServices is not a good idea
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
     * The factory used to generate Watchdog objects
     */
    private WatchdogFactory theWatchdogFactory;

    /**
     * The configuration data to be passed to the handler
     */
    private IMAPHandlerConfigurationDataImpl theConfigData
            = new IMAPHandlerConfigurationDataImpl();

    public MailboxManagerProvider mailboxManagerProvider;

    public void service( ServiceManager serviceManager ) throws ServiceException
    {
        super.service( serviceManager );
        UsersRepository usersRepository = ( UsersRepository ) serviceManager.
                lookup( "org.apache.james.services.UsersRepository" );
        setUserRepository(usersRepository);
		MailboxManagerProvider mailboxManagerProvider =(MailboxManagerProvider) serviceManager.lookup("org.apache.james.mailboxmanager.manager.MailboxManagerProvider");
		getLogger().debug("MailboxManagerMailRepository uses service "+mailboxManagerProvider);
		setMailboxManagerProvider(mailboxManagerProvider);
    }

    void setUserRepository(UsersRepository repository) {
    	this.users=repository;
	}

	void setMailboxManagerProvider(MailboxManagerProvider mailboxManagerProvider) {
		this.mailboxManagerProvider=mailboxManagerProvider;
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
            getLogger().info( "The idle timeout will be reset every " + lengthReset + " bytes." );
            boolean streamdump=handlerConfiguration.getChild("streamdump").getAttributeAsBoolean("enabled", false);
            theConfigData.setStreamDump(streamdump);
            String streamdumpDir=handlerConfiguration.getChild("streamdump").getAttribute("directory", null);
            theConfigData.setStreamDumpDir(streamdumpDir);
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

        theWatchdogFactory = getWatchdogFactory();
    }

    /**
     * @see AbstractJamesService#getDefaultPort()
     */
    protected int getDefaultPort()
    {
        return 143;
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
        ImapHandler theHandler = (ImapHandler) theHandlerPool.get();

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

    /**
     * A class to provide POP3 handler configuration to the handlers
     */
    private class IMAPHandlerConfigurationDataImpl
            implements ImapHandlerConfigurationData
    {

    	private String streamdumpDir = null;
    	private boolean streamdump = false;
        /**
         * @see ImapHandlerConfigurationData#getHelloName()
         */
        public String getHelloName()
        {
            return ImapServer.this.helloName;
        }

        public void setStreamDumpDir(String streamdumpDir) {
			this.streamdumpDir=streamdumpDir;
		}

		public void setStreamDump(boolean streamdump) {
			this.streamdump=streamdump;
			
		}

		/**
         * @see ImapHandlerConfigurationData#getResetLength()
         */
        public int getResetLength()
        {
            return ImapServer.this.lengthReset;
        }

       /**
         * @see ImapHandlerConfigurationData#getUsersRepository()
         */
        public UsersRepository getUsersRepository()
        {
            return ImapServer.this.users;
        }

        public MailboxManagerProvider getMailboxManagerProvider() {
          return ImapServer.this.mailboxManagerProvider;
        }

		public boolean doStreamdump() {
			return streamdump;
		}

		public String getStreamdumpDir() {
			return streamdumpDir;
		}

    }

	protected Object getConfigurationData() {
		return theConfigData;
	}

}
