/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james;

import org.apache.james.core.MailImpl;
import org.apache.james.imapserver.ImapHost;
import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.services.JamesUser;
import org.apache.james.userrepository.DefaultJamesUser;
import org.apache.mailet.MailAddress;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.component.ComponentManager;

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
 * @version This is $Revision: 1.1 $

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
                                      ComponentManager componentManager ) throws Exception
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
                imapHost = ( ImapHost ) componentManager.lookup( ImapHost.ROLE );
            }
            catch ( Exception e ) {
                getLogger().error( "Exception in IMAP Storage init: " + e.getMessage() );
                throw e;
            }
        }
        else {
            super.initialiseInboxes(configuration, componentManager);
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
