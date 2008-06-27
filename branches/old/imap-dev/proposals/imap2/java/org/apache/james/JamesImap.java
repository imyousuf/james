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

package org.apache.james;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.core.MailImpl;
import org.apache.james.imapserver.ImapHost;
import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.services.JamesUser;
import org.apache.james.userrepository.DefaultJamesUser;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * Core class for JAMES. Provides three primary services:
 * <br> 1) Instantiates resources, such as user repository, and protocol
 * handlers
 * <br> 2) Handles interactions between components
 * <br> 3) Provides container services for Mailets
 *
 *
 * @version This is $Revision: 1.6 $

 */
public class JamesImap extends James
{
    /**
     * Whether James should use IMAP storage
     */
    private boolean useIMAPstorage = false;

    /**
     * The host to be used for IMAP storage
     */
    private ImapHost imapHost;


    protected void initialiseInboxes( Configuration configuration,
                                      ServiceManager manager ) throws Exception
    {
        try {
            // Get storage config param
            if ( configuration.getChild( "storage" ).getValue().equals( "IMAP" ) ) {
                useIMAPstorage = true;
                getLogger().info( "Using IMAP Store-System" );
            }
        }
        catch ( Exception e ) {
            // No storage entry found in config file
        }

        // Get the LocalInbox repository
        if ( useIMAPstorage ) {
            try {
                // We will need to use a no-args constructor for flexibility
                imapHost = ( ImapHost ) manager.lookup( ImapHost.ROLE );
            }
            catch ( Exception e ) {
                getLogger().error( "Exception in IMAP Storage init: " + e.getMessage() );
                throw e;
            }
        }
        else {
            super.initialiseInboxes(configuration, manager);
        }
    }


    //Methods for MailetContext
    protected void storeMail( String username, MimeMessage message, MailAddress recipient, MailAddress sender ) throws MessagingException
    {
        JamesUser user;
        if ( useIMAPstorage ) {
            ImapMailbox mbox = null;
            try {
                user = ( JamesUser ) getLocalusers().getUserByName( username );
                mbox = imapHost.getInbox( user );
                MailImpl mail = new MailImpl( message );
                mbox.store( mail );
                getLogger().info( "Message " + message.getMessageID() +
                                  " stored in " +
                                  mbox.getFullName() );
                mbox = null;
            }
            catch ( Exception e ) {
                getLogger().error( "Exception storing mail: " + e );
                e.printStackTrace();
                if ( mbox != null ) {
                    mbox = null;
                }
                throw new RuntimeException( "Exception storing mail: " + e );
            }
        }
        else {
            super.storeMail( username, message, recipient, sender );
        }
    }

    /**
     * Adds a user to this mail server. Currently just adds user to a
     * UsersRepository.
     * <p> As we move to IMAP support this will also create mailboxes and
     * access control lists.
     *
     * @param userName String representing user name, that is the portion of
     * an email address before the '@&lt;domain&gt;'.
     * @param password String plaintext password
     * @return boolean true if user added succesfully, else false.
     */
    public boolean addUser(String userName, String password)
    {
        boolean success;
        DefaultJamesUser user = new DefaultJamesUser(userName, "SHA");
        user.setPassword(password);
        user.initialize();
        success = getLocalusers().addUser(user);
        if ( useIMAPstorage && success ) {
            try {
                imapHost.createPrivateMailAccount( user );
                getLogger().info( "New MailAccount created for" + userName );
            }
            catch ( MailboxException e ) {
                return false;
            }
        }

        return success;
    }


}
