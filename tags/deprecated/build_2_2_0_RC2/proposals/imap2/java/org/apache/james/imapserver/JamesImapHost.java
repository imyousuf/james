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

import org.apache.james.services.User;
import org.apache.james.imapserver.store.ImapStore;
import org.apache.james.imapserver.store.InMemoryStore;
import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.store.SimpleImapMessage;
import org.apache.james.imapserver.store.MessageFlags;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.ConsoleLogger;

import javax.mail.search.SearchTerm;
import javax.mail.internet.MimeMessage;
import javax.mail.MessagingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.HashMap;
import java.util.Date;

/**
 * An initial implementation of an ImapHost. By default, uses,
 * the {@link org.apache.james.imapserver.store.InMemoryStore} implementation of {@link org.apache.james.imapserver.store.ImapStore}.
 * TODO: Make the underlying store configurable with Phoenix.
 *
 *
 * @version $Revision: 1.5.2.3 $
 */
public class JamesImapHost
        extends AbstractLogEnabled
        implements ImapHost, ImapConstants
{
    private ImapStore store;
    private MailboxSubscriptions subscriptions;

    /**
     * Hack constructor which creates an in-memory store, and creates a console logger.
     */
    public JamesImapHost()
    {
        enableLogging( new ConsoleLogger() );
        store = new InMemoryStore();
        setupLogger( store );
        subscriptions = new MailboxSubscriptions();
    }

    public JamesImapHost( ImapStore store )
    {
        this.store = store;
        subscriptions = new MailboxSubscriptions();
    }

    public char getHierarchyDelimiter()
    {
        return HIERARCHY_DELIMITER_CHAR;
    }

    /** @see ImapHost#getMailbox */
    public ImapMailbox getMailbox( User user, String mailboxName )
    {
        String name = getQualifiedMailboxName( user, mailboxName );
        ImapMailbox mailbox = store.getMailbox( name );
        return ( checkViewable( mailbox ) );
    }

    public ImapMailbox getMailbox( User user, String mailboxName, boolean mustExist )
            throws MailboxException
    {
        ImapMailbox mailbox = getMailbox( user, mailboxName );
        if ( mustExist && mailbox == null )
        {
            throw new MailboxException( "No such mailbox." );
        }
        return mailbox;
    }

    private ImapMailbox checkViewable( ImapMailbox mailbox )
    {
        // TODO implement this.
        return mailbox;
    }

    /** @see ImapHost#getInbox */
    public ImapMailbox getInbox( User user ) throws MailboxException
    {
        return getMailbox( user, INBOX_NAME );
    }

    /** @see ImapHost#createPrivateMailAccount */
    public void createPrivateMailAccount( User user ) throws MailboxException
    {
        ImapMailbox root = store.getMailbox( USER_NAMESPACE );
        ImapMailbox userRoot = store.createMailbox( root, user.getUserName(), false );
        store.createMailbox( userRoot, INBOX_NAME, true );
    }

    /** @see ImapHost#createMailbox */
    public ImapMailbox createMailbox( User user, String mailboxName )
            throws AuthorizationException, MailboxException
    {
        String qualifiedName = getQualifiedMailboxName( user, mailboxName );
        if ( store.getMailbox( qualifiedName ) != null )
        {
            throw new MailboxException( "Mailbox already exists." );
        }

        StringTokenizer tokens = new StringTokenizer( qualifiedName,
                                                      HIERARCHY_DELIMITER );

        if ( tokens.countTokens() < 2 ) {
            throw new MailboxException( "Cannot create mailbox at namespace level." );
        }

        String namespaceRoot = tokens.nextToken();
        ImapMailbox mailbox = store.getMailbox( namespaceRoot );
        if ( mailbox == null ) {
            throw new MailboxException( "Invalid namespace." );
        }

        while ( tokens.hasMoreTokens() ) {
            // Get the next name from the list, and find the child
            String childName = tokens.nextToken();
            ImapMailbox child = store.getMailbox( mailbox, childName );
            // Create if neccessary
            if ( child == null ) {
                // TODO check permissions.
                boolean makeSelectable = ( !tokens.hasMoreTokens() );
                child = store.createMailbox( mailbox, childName, makeSelectable );
            }
            mailbox = child;
        }

        return mailbox;
    }

    /** @see ImapHost#deleteMailbox */
    public void deleteMailbox( User user, String mailboxName )
            throws MailboxException, AuthorizationException
    {
        ImapMailbox toDelete = getMailbox( user, mailboxName, true );

        if ( store.getChildren( toDelete ).isEmpty() ) {
            long[] uids = toDelete.getMessageUids();
            for ( int i = 0; i < uids.length; i++ ) {
                long uid = uids[i];
                SimpleImapMessage imapMessage = toDelete.getMessage( uid );
                toDelete.deleteMessage( imapMessage.getUid() );
            }
            store.deleteMailbox( toDelete );
        }
        else {
            if ( toDelete.isSelectable() ) {
                // TODO delete all messages.
                store.setSelectable( toDelete, false );
            }
            else {
                throw new MailboxException( "Can't delete a non-selectable mailbox with children." );
            }
        }
    }

    /** @see ImapHost#renameMailbox */
    public void renameMailbox( User user,
                               String oldMailboxName,
                               String newMailboxName )
            throws MailboxException, AuthorizationException
    {

        ImapMailbox existingMailbox = getMailbox( user, oldMailboxName, true );

        // TODO: check permissions.

        // Handle case where existing is INBOX
        //          - just create new folder, move all messages,
        //            and leave INBOX (with children) intact.
        String userInboxName = getQualifiedMailboxName( user, INBOX_NAME );
        if ( userInboxName.equals( existingMailbox.getFullName() ) ) {
            ImapMailbox newBox = createMailbox( user, newMailboxName );
            // TODO copy all messages from INBOX.
            return;
        }

        store.renameMailbox( existingMailbox, newMailboxName );
    }

    /** @see ImapHost#listSubscribedMailboxes */
    public Collection listSubscribedMailboxes( User user,
                                               String mailboxPattern )
            throws MailboxException
    {
        return listMailboxes( user, mailboxPattern, true );
    }

    /** @see ImapHost#listMailboxes */
    public Collection listMailboxes( User user,
                                     String mailboxPattern )
            throws MailboxException
    {
        return listMailboxes( user, mailboxPattern, false );
    }

    /**
     * Partial implementation of list functionality.
     * TODO: Handle wildcards anywhere in mailbox pattern
     *       (currently only supported as last character of pattern)
     * @see org.apache.james.imapserver.ImapHost#listMailboxes
     */
    private Collection listMailboxes( User user,
                                     String mailboxPattern,
                                     boolean subscribedOnly )
            throws MailboxException
    {
//        System.out.println( "Listing for user: '" + user.getUserName() + "'" +
//                            " pattern:'" + mailboxPattern + "'" );

        ArrayList mailboxes = new ArrayList();
        String qualifiedPattern = getQualifiedMailboxName( user, mailboxPattern );

        Iterator iter = store.listMailboxes( qualifiedPattern ).iterator();
        while ( iter.hasNext() ) {
            ImapMailbox mailbox = ( ImapMailbox ) iter.next();

            // TODO check subscriptions.
            if ( subscribedOnly ) {
                if ( ! subscriptions.isSubscribed( user, mailbox ) ) {
                    // if not subscribed
                    mailbox = null;
                }
            }

            // Sets the mailbox to null if it's not viewable.
            mailbox = checkViewable( mailbox );

            if ( mailbox != null ) {
                mailboxes.add( mailbox );
            }
        }

        return mailboxes;
    }

    /** @see ImapHost#subscribe */
    public void subscribe( User user, String mailboxName )
            throws MailboxException
    {
        ImapMailbox mailbox = getMailbox( user, mailboxName, true );
        subscriptions.subscribe( user, mailbox );
    }

    /** @see ImapHost#unsubscribe */
    public void unsubscribe( User user, String mailboxName )
            throws MailboxException
    {
        ImapMailbox mailbox = getMailbox( user, mailboxName, true );
        subscriptions.unsubscribe( user, mailbox );
    }

    public int[] expunge( ImapMailbox mailbox )
            throws MailboxException
    {
        ArrayList deletedMessages = new ArrayList();

        long[] uids = mailbox.getMessageUids();
        for ( int i = 0; i < uids.length; i++ ) {
            long uid = uids[i];
            SimpleImapMessage message = mailbox.getMessage( uid );
            if ( message.getFlags().isDeleted() ) {
                deletedMessages.add( message );
            }
        }

        int[] ids = new int[ deletedMessages.size() ];
        for ( int i = 0; i < ids.length; i++ ) {
            SimpleImapMessage imapMessage = ( SimpleImapMessage ) deletedMessages.get( i );
            long uid = imapMessage.getUid();
            int msn = mailbox.getMsn( uid );
            ids[i] = msn;
            mailbox.deleteMessage( uid );
        }

        return ids;
    }

    public long[] search( SearchTerm searchTerm, ImapMailbox mailbox )
    {
        ArrayList matchedMessages = new ArrayList();

        long[] allUids = mailbox.getMessageUids();
        for ( int i = 0; i < allUids.length; i++ ) {
            long uid = allUids[i];
            SimpleImapMessage message = mailbox.getMessage( uid );
            if ( searchTerm.match( message.getMimeMessage() ) ) {
                matchedMessages.add( message );
            }
        }

        long[] matchedUids = new long[ matchedMessages.size() ];
        for ( int i = 0; i < matchedUids.length; i++ ) {
            SimpleImapMessage imapMessage = ( SimpleImapMessage ) matchedMessages.get( i );
            long uid = imapMessage.getUid();
            matchedUids[i] = uid;
        }
        return matchedUids;
    }

    /** @see {@link ImapHost#copyMessage } */
    public void copyMessage( long uid, ImapMailbox currentMailbox, ImapMailbox toMailbox )
            throws MailboxException
    {
        SimpleImapMessage originalMessage = currentMailbox.getMessage( uid );
        MimeMessage newMime = null;
        try {
            newMime = new MimeMessage( originalMessage.getMimeMessage() );
        }
        catch ( MessagingException e ) {
            // TODO chain.
            throw new MailboxException( "Messaging exception: " + e.getMessage() );
        }
        MessageFlags newFlags = new MessageFlags();
        newFlags.setAll( originalMessage.getFlags() );
        Date newDate = originalMessage.getInternalDate();

        toMailbox.createMessage( newMime, newFlags, newDate);
    }

    /**
     * Convert a user specified mailbox name into a server absolute name.
     * If the mailboxName begins with the namespace token,
     * return as-is.
     * If not, need to resolve the Mailbox name for this user.
     * Example:
     * <br> Convert "INBOX" for user "Fred.Flinstone" into
     * absolute name: "#user.Fred.Flintstone.INBOX"
     *
     * @return String of absoluteName, null if not valid selection
     */
    private String getQualifiedMailboxName( User user, String mailboxName )
    {
        String userName = user.getUserName();

        if ( "INBOX".equalsIgnoreCase( mailboxName ) ) {
            return USER_NAMESPACE + HIERARCHY_DELIMITER + userName +
                    HIERARCHY_DELIMITER + INBOX_NAME;
        }

        if ( mailboxName.startsWith( NAMESPACE_PREFIX ) ) {
            return mailboxName;
        }
        else {
            if ( mailboxName.length() == 0 ) {
                return USER_NAMESPACE + HIERARCHY_DELIMITER + userName;
            }
            else {
                return USER_NAMESPACE + HIERARCHY_DELIMITER + userName +
                        HIERARCHY_DELIMITER + mailboxName;
            }
        }
    }

    /**
     * Handles all user subscriptions.
     * TODO make this a proper class
     * TODO persist
     */
    private class MailboxSubscriptions
    {
        private Map userSubs = new HashMap();

        /**
         * Subscribes the user to the mailbox.
         * TODO should this fail if already subscribed?
         * @param user The user making the subscription
         * @param mailbox The mailbox to subscribe
         * @throws MailboxException ??? doesn't yet.
         */
        void subscribe( User user, ImapMailbox mailbox )
                throws MailboxException
        {
            getUserSubs( user ).add( mailbox.getFullName() );
        }

        /**
         * Unsubscribes the user from this mailbox.
         * TODO should this fail if not already subscribed?
         * @param user The user making the request.
         * @param mailbox The mailbox to unsubscribe
         * @throws MailboxException ?? doesn't yet
         */
        void unsubscribe( User user, ImapMailbox mailbox )
                throws MailboxException
        {
            getUserSubs( user ).remove( mailbox.getFullName() );
        }

        /**
         * Returns whether the user is subscribed to the specified mailbox.
         * @param user The user to test.
         * @param mailbox The mailbox to test.
         * @return <code>true</code> if the user is subscribed.
         */
        boolean isSubscribed( User user, ImapMailbox mailbox )
        {
            return getUserSubs( user ).contains( mailbox.getFullName() );
        }

        private Collection getUserSubs( User user )
        {
            Collection subs = (Collection)userSubs.get( user.getUserName() );
            if ( subs == null ) {
                subs = new ArrayList();
                userSubs.put( user.getUserName(), subs );
            }
            return subs;
        }
    }


}
