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

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.core.AbstractJamesService;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.services.MailServer;
import org.apache.james.services.UsersRepository;

/**
 * TODO: this is a quick cut-and-paste hack from POP3Server. Should probably be
 * rewritten from scratch, together with ImapHandler.
 * TODO: Using a custom, not AbstractJamesHandler, handler together with AbstractJamesServices is not a good idea
 *
 * <p>Accepts IMAP connections on a server socket and dispatches them to IMAPHandlers.</p>
 *
 * <p>Also responsible for loading and parsing IMAP specific configuration.</p>
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
     * The configuration data to be passed to the handler
     */
    private IMAPHandlerConfigurationDataImpl theConfigData
            = new IMAPHandlerConfigurationDataImpl();

    public MailboxManagerProvider mailboxManagerProvider;

    private MailServer mailServer;

    public void service( ServiceManager serviceManager ) throws ServiceException
    {
        super.service( serviceManager );
        UsersRepository usersRepository = ( UsersRepository ) serviceManager.
                lookup( "org.apache.james.services.UsersRepository" );
        setUserRepository(usersRepository);
        MailboxManagerProvider mailboxManagerProvider =(MailboxManagerProvider) serviceManager.lookup("org.apache.james.mailboxmanager.manager.MailboxManagerProvider");
        getLogger().debug("MailboxManagerMailRepository uses service "+mailboxManagerProvider);
        setMailboxManagerProvider(mailboxManagerProvider);
        setMailServer((MailServer) serviceManager.lookup(MailServer.ROLE));
    }

    void setUserRepository(UsersRepository repository) {
        this.users=repository;
    }
    
    void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
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
        }
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
     * The factory for producing handlers.
     */
        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#newInstance()
         */
        public Object newInstance() throws Exception
        {
            final ImapHandler imapHandler = new ImapHandler(); 
            final Logger logger = getLogger(); 
            logger.debug("Create handler instance"); 
            setupLogger(imapHandler); 
            return imapHandler; 
        }

        /**
         * @see org.apache.avalon.excalibur.pool.ObjectFactory#getCreatedClass()
         */
        public Class getCreatedClass()
        {
            return ImapHandler.class;
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
            if (ImapServer.this.helloName == null) {
                return ImapServer.this.mailServer.getHelloName();
            } else {
                return ImapServer.this.helloName;
            }
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

    }

    protected Object getConfigurationData() {
        return theConfigData;
    }

}
