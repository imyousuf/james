/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import org.apache.james.services.User;
import org.apache.james.imapserver.store.ImapStore;
import org.apache.james.imapserver.store.InMemoryStore;
import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.store.MailboxException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;

/**
 * An initial implementation of an ImapHost. By default, uses,
 * the {@link org.apache.james.imapserver.store.InMemoryStore} implementation of {@link org.apache.james.imapserver.store.ImapStore}.
 * TODO: Make the underlying store configurable with Phoenix.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.2 $
 */
public class JamesImapHost
        implements ImapHost, ImapConstants
{
    private ImapStore store;

    public JamesImapHost()
    {
        store = new InMemoryStore();
    }

    public JamesImapHost( ImapStore store )
    {
        this.store = store;
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
            // Does this delete all messages?
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

    public Collection listSubscribedMailboxes( User user,
                                               String mailboxPattern )
            throws MailboxException
    {
        throw new MailboxException( "Subscriptions not implemented" );
//        return listMailboxes( user, mailboxPattern, true );
    }

    public Collection listMailboxes( User user,
                                     String mailboxPattern )
            throws MailboxException
    {
        return listMailboxes( user, mailboxPattern, false );
    }

    /**
     * Partial implementation of list functionality.
     * TODO: Handle subscriptions (currently ignored)
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

            // Sets the mailbox to null if it's not viewable.
            mailbox = checkViewable( mailbox );

            // TODO check subscriptions.
            if ( subscribedOnly ) {
                // if not subscribed
                mailbox = null;
            }

            if ( mailbox != null ) {
                mailboxes.add( mailbox );
            }
        }

        return mailboxes;
    }

    public void subscribe( User user, String mailbox )
            throws MailboxException
    {
        // TODO implement
    }

    public void unsubscribe( String username, String mailbox )
            throws MailboxException
    {
        // TODO implement
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


}
