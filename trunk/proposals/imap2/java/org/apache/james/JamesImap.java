/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
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
import org.apache.avalon.framework.service.ServiceManager;

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
 * @version This is $Revision: 1.3 $

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
