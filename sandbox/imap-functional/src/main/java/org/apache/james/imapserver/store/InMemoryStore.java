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

package org.apache.james.imapserver.store;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.core.MailImpl;
import org.apache.james.imapserver.ImapConstants;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * A simple in-memory implementation of {@link ImapStore}, used for testing
 * and development. Note: this implementation does not persist *anything* to disk.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision$
 */
public class InMemoryStore
        extends AbstractLogEnabled
        implements ImapStore, ImapConstants
{
    private RootMailbox rootMailbox = new RootMailbox();
    private static final Flags PERMANENT_FLAGS = new Flags();
    static {
        PERMANENT_FLAGS.add(Flags.Flag.ANSWERED);
        PERMANENT_FLAGS.add(Flags.Flag.DELETED);
        PERMANENT_FLAGS.add(Flags.Flag.DRAFT);
        PERMANENT_FLAGS.add(Flags.Flag.FLAGGED);
        PERMANENT_FLAGS.add(Flags.Flag.SEEN);
    }

    public ImapMailbox getMailbox( String absoluteMailboxName )
    {
        StringTokenizer tokens = new StringTokenizer( absoluteMailboxName, HIERARCHY_DELIMITER );

        // The first token must be "#mail"
        if ( !tokens.hasMoreTokens() ||
                !tokens.nextToken().equalsIgnoreCase( USER_NAMESPACE ) ) {
            return null;
        }

        HierarchicalMailbox parent = rootMailbox;
        while ( parent != null && tokens.hasMoreTokens() ) {
            String childName = tokens.nextToken();
            parent = parent.getChild( childName );
        }
        return parent;
    }

    public ImapMailbox getMailbox( ImapMailbox parent, String name )
    {
        return ( ( HierarchicalMailbox ) parent ).getChild( name );
    }

    public ImapMailbox createMailbox( ImapMailbox parent,
                                      String mailboxName,
                                      boolean selectable )
            throws MailboxException
    {
        if ( mailboxName.indexOf( HIERARCHY_DELIMITER_CHAR ) != -1 ) {
            throw new MailboxException( "Invalid mailbox name." );
        }
        HierarchicalMailbox castParent = ( HierarchicalMailbox ) parent;
        HierarchicalMailbox child = new HierarchicalMailbox( castParent, mailboxName );
        castParent.getChildren().add( child );
        child.setSelectable( selectable );
        return child;
    }

    public void deleteMailbox( ImapMailbox mailbox ) throws MailboxException
    {
        HierarchicalMailbox toDelete = ( HierarchicalMailbox ) mailbox;

        if ( !toDelete.getChildren().isEmpty() ) {
            throw new MailboxException( "Cannot delete mailbox with children." );
        }

        if ( toDelete.getMessageCount() != 0 ) {
            throw new MailboxException( "Cannot delete non-empty mailbox" );
        }

        HierarchicalMailbox parent = toDelete.getParent();
        parent.getChildren().remove( toDelete );
    }

    public void renameMailbox( ImapMailbox existingMailbox, String newName ) throws MailboxException
    {
        HierarchicalMailbox toRename = ( HierarchicalMailbox ) existingMailbox;
        toRename.setName( newName );
    }

    public Collection getChildren( ImapMailbox parent )
    {
        Collection children = ( ( HierarchicalMailbox ) parent ).getChildren();
        return Collections.unmodifiableCollection( children );
    }

    public ImapMailbox setSelectable( ImapMailbox mailbox, boolean selectable )
    {
        ( ( HierarchicalMailbox ) mailbox ).setSelectable( selectable );
        return mailbox;
    }

    /** @see org.apache.james.imapserver.store.ImapStore#listMailboxes */
    public Collection listMailboxes( String searchPattern )
            throws MailboxException
    {
        int starIndex = searchPattern.indexOf( '*' );
        int percentIndex = searchPattern.indexOf( '%' );

        // We only handle wildcard at the end of the search pattern.
        if ( ( starIndex > -1 && starIndex < searchPattern.length() - 1 ) ||
                ( percentIndex > -1 && percentIndex < searchPattern.length() - 1 ) ) {
            throw new MailboxException( "WIldcard characters are only handled as the last character of a list argument." );
        }

        ArrayList mailboxes = new ArrayList();
        if ( starIndex != -1 || percentIndex != -1 ) {
            int lastDot = searchPattern.lastIndexOf( HIERARCHY_DELIMITER );
            String parentName = searchPattern.substring( 0, lastDot );
            String matchPattern = searchPattern.substring( lastDot + 1, searchPattern.length() - 1 );

            HierarchicalMailbox parent = ( HierarchicalMailbox ) getMailbox( parentName );
            // If the parent from the search pattern doesn't exist,
            // return empty.
            if ( parent != null ) {
                Iterator children = parent.getChildren().iterator();
                while ( children.hasNext() ) {
                    HierarchicalMailbox child = ( HierarchicalMailbox ) children.next();
                    if ( child.getName().startsWith( matchPattern ) ) {
                        mailboxes.add( child );

                        if ( starIndex != -1 ) {
                            addAllChildren( child, mailboxes );
                        }
                    }
                }
            }

        }
        else {
            ImapMailbox mailbox = getMailbox( searchPattern );
            if ( mailbox != null ) {
                mailboxes.add( mailbox );
            }
        }

        return mailboxes;
    }

    private void addAllChildren( HierarchicalMailbox mailbox, Collection mailboxes )
    {
        Collection children = mailbox.getChildren();
        Iterator iterator = children.iterator();
        while ( iterator.hasNext() ) {
            HierarchicalMailbox child = ( HierarchicalMailbox ) iterator.next();
            mailboxes.add( child );
            addAllChildren( child, mailboxes );
        }
    }

    private class RootMailbox extends HierarchicalMailbox
    {
        public RootMailbox()
        {
            super( null, ImapConstants.USER_NAMESPACE );
        }

        public String getFullName()
        {
            return name;
        }
    }

    private class HierarchicalMailbox implements ImapMailbox
    {
        private Collection children;
        private HierarchicalMailbox parent;

        protected String name;
        private boolean isSelectable = false;

        private List mailMessages = new LinkedList();
        private long nextUid = 1;
        private long uidValidity;

        private List _mailboxListeners = new LinkedList();

        public HierarchicalMailbox( HierarchicalMailbox parent,
                                    String name )
        {
            this.name = name;
            this.children = new ArrayList();
            this.parent = parent;
            this.uidValidity = System.currentTimeMillis();
        }

        public Collection getChildren()
        {
            return children;
        }

        public HierarchicalMailbox getParent()
        {
            return parent;
        }

        public HierarchicalMailbox getChild( String name )
        {
            Iterator iterator = children.iterator();
            while ( iterator.hasNext() ) {
                HierarchicalMailbox child = ( HierarchicalMailbox ) iterator.next();
                if ( child.getName().equalsIgnoreCase( name ) ) {
                    return child;
                }
            }
            return null;
        }

        public void setName( String name )
        {
            this.name = name;
        }

        public String getName()
        {
            return name;
        }

        public String getFullName()
        {
            return parent.getFullName() + HIERARCHY_DELIMITER_CHAR + name;
        }

        public Flags getPermanentFlags() {
            return PERMANENT_FLAGS;
        }

        public int getMessageCount()
        {
            return mailMessages.size();
        }

        public long getUidValidity()
        {
            return uidValidity;
        }

        public long getUidNext()
        {
            return nextUid;
        }

        public int getUnseenCount()
        {
            int count = 0;
            for (int i = 0; i < mailMessages.size(); i++) {
                SimpleImapMessage message = (SimpleImapMessage) mailMessages.get(i);
                if (! message.getFlags().contains(Flags.Flag.SEEN)) {
                    count++;
                }
            }
            return count;
        }

        /**
         * Returns the 1-based index of the first unseen message. Unless there are outstanding
         * expunge responses in the ImapSessionMailbox, this will correspond to the MSN for
         * the first unseen.
         */ 
        public int getFirstUnseen()
        {
            for (int i = 0; i < mailMessages.size(); i++) {
                SimpleImapMessage message = (SimpleImapMessage) mailMessages.get(i);
                if (! message.getFlags().contains(Flags.Flag.SEEN)) {
                    return i + 1;
                }
            }
            return -1;
        }

        public int getRecentCount(boolean reset)
        {
            int count = 0;
            for (int i = 0; i < mailMessages.size(); i++) {
                SimpleImapMessage message = (SimpleImapMessage) mailMessages.get(i);
                if (message.getFlags().contains(Flags.Flag.RECENT)) {
                    count++;
                    if (reset) {
                        message.getFlags().remove(Flags.Flag.RECENT);
                    }
                }
            }
            return count;
        }

        public int getMsn( long uid ) throws MailboxException
        {
            for (int i = 0; i < mailMessages.size(); i++) {
                SimpleImapMessage message = (SimpleImapMessage) mailMessages.get(i);
                if (message.getUid() == uid) {
                    return i + 1;
                }
            }
            throw new MailboxException( "No such message." );
        }

        public void signalDeletion() {
            // Notify all the listeners of the new message
            synchronized (_mailboxListeners) {
                for (int j = 0; j < _mailboxListeners.size(); j++) {
                    MailboxListener listener = (MailboxListener) _mailboxListeners.get(j);
                    listener.mailboxDeleted();
                }
            }

        }

        public boolean isSelectable()
        {
            return isSelectable;
        }

        public void setSelectable( boolean selectable )
        {
            isSelectable = selectable;
        }

        public long appendMessage( MimeMessage message,
                                          Flags flags,
                                          Date internalDate )
        {
            long uid = nextUid;
            nextUid++;

//            flags.setRecent(true);
            SimpleImapMessage imapMessage = new SimpleImapMessage( message, flags,
                                                       internalDate, uid );
            imapMessage.getFlags().add(Flags.Flag.RECENT);
            setupLogger( imapMessage );

            mailMessages.add(imapMessage);
            int newMsn = mailMessages.size();

            // Notify all the listeners of the new message
            synchronized (_mailboxListeners) {
                for (int j = 0; j < _mailboxListeners.size(); j++) {
                    MailboxListener listener = (MailboxListener) _mailboxListeners.get(j);
                    listener.added(newMsn);
                }
            }

            return uid;
        }

        public void setFlags(Flags flags, boolean value, long uid, MailboxListener silentListener, boolean addUid) throws MailboxException {
            int msn = getMsn(uid);
            SimpleImapMessage message = (SimpleImapMessage) mailMessages.get(msn - 1);
            
            if (value) {
                message.getFlags().add(flags);
            } else {
                message.getFlags().remove(flags);
            }

            Long uidNotification = null;
            if (addUid) {
                uidNotification = new Long(uid);
            }
            notifyFlagUpdate(msn, message.getFlags(), uidNotification, silentListener);
        }
        
        public void replaceFlags(Flags flags, long uid, MailboxListener silentListener, boolean addUid) throws MailboxException {
            int msn = getMsn(uid);
            SimpleImapMessage message = (SimpleImapMessage) mailMessages.get(msn - 1);
            message.getFlags().remove(MessageFlags.ALL_FLAGS);
            message.getFlags().add(flags);

            Long uidNotification = null;
            if (addUid) {
                uidNotification = new Long(uid);
            }
            notifyFlagUpdate(msn, message.getFlags(), uidNotification, silentListener);
        }

        private void notifyFlagUpdate(int msn, Flags flags, Long uidNotification, MailboxListener silentListener) {
            synchronized(_mailboxListeners) {
                for (int i = 0; i < _mailboxListeners.size(); i++) {
                    MailboxListener listener = (MailboxListener) _mailboxListeners.get(i);
                    
                    if (listener == silentListener) {
                        continue;
                    }

                    listener.flagsUpdated(msn, flags, uidNotification);
                }
            }
        }
        
        public void deleteAllMessages() {
            mailMessages.clear();
        }

        public void store( MailImpl mail )
                throws Exception
        {
            MimeMessage message = mail.getMessage();
            Date internalDate = new Date();
            Flags flags = new Flags();
            appendMessage( message, flags, internalDate );
        }

        public SimpleImapMessage getMessage( long uid )
        {
            for (int i = 0; i < mailMessages.size(); i++) {
                SimpleImapMessage message = (SimpleImapMessage) mailMessages.get(i);
                if (message.getUid() == uid) {
                    return message;
                }
            }
            return null;
        }
        
        public long[] getMessageUids()
        {
            long[] uids = new long[ mailMessages.size() ];
            for (int i = 0; i < mailMessages.size(); i++) {
                SimpleImapMessage message = (SimpleImapMessage) mailMessages.get(i);
                uids[i] = message.getUid();
            }
            return uids;
        }

        private void deleteMessage( int msn )
        {
            mailMessages.remove(msn - 1);
        }

        public long[] search(SearchTerm searchTerm) {
            ArrayList matchedMessages = new ArrayList();

            for (int i = 0; i < mailMessages.size(); i++) {
                SimpleImapMessage message = (SimpleImapMessage) mailMessages.get(i);
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

        public void copyMessage( long uid, ImapMailbox toMailbox )
                throws MailboxException
        {
            SimpleImapMessage originalMessage = getMessage( uid );
            MimeMessage newMime = null;
            try {
                newMime = new MimeMessage( originalMessage.getMimeMessage() );
            }
            catch ( MessagingException e ) {
                // TODO chain.
                throw new MailboxException( "Messaging exception: " + e.getMessage() );
            }
            Flags newFlags = new Flags();
            newFlags.add( originalMessage.getFlags() );
            Date newDate = originalMessage.getInternalDate();

            toMailbox.appendMessage( newMime, newFlags, newDate);
        }

        public void expunge() throws MailboxException {
            for (int i = 0; i < mailMessages.size(); i++) {
                SimpleImapMessage message = (SimpleImapMessage) mailMessages.get(i);
                if (message.getFlags().contains(Flags.Flag.DELETED)) {
                    expungeMessage(i + 1);
                }
            }
        }

        private void expungeMessage(int msn) {
            // Notify all the listeners of the pending delete
            synchronized (_mailboxListeners) {
                for (int j = 0; j < _mailboxListeners.size(); j++) {
                    MailboxListener expungeListener = (MailboxListener) _mailboxListeners.get(j);
                    expungeListener.expunged(msn);
                }
            }

            deleteMessage(msn);
        }

        public void addListener(MailboxListener listener) {
            synchronized(_mailboxListeners) {
                _mailboxListeners.add(listener);
            }
        }

        public void removeListener(MailboxListener listener) {
            synchronized (_mailboxListeners) {
                _mailboxListeners.remove(listener);
            }
        }
    }
}
