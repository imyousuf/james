/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.james.imapserver.store;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import javax.mail.Flags;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.core.MailImpl;
import org.apache.james.imapserver.ImapConstants;

/**
 * A simple in-memory implementation of {@link ImapStore}, used for testing
 * and development. Note: this implementation does not persist *anything* to disk.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.10 $
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

        public int getRecentCount()
        {
            return 0;
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
            setupLogger( imapMessage );

            mailMessages.add(imapMessage);
            int newMsn = mailMessages.size();

            // Notify all the listeners of the new message
            synchronized (_mailboxListeners) {
                for (int j = 0; j < _mailboxListeners.size(); j++) {
                    MailboxListener listener = (MailboxListener) _mailboxListeners.get(j);
                    listener.added(uid);
                }
            }

            return uid;
        }

        public void setFlags(Flags flags, boolean value, long uid, boolean silent) throws MailboxException {
            SimpleImapMessage message = getMessage(uid);
            if (message == null) {
                throw new MailboxException( "Message doesn't exist" );
            }

            if (value) {
                message.getFlags().add(flags);
            } else {
                message.getFlags().remove(flags);
            }

            // TODO - this doesn't send silent updates to *any* listeners
            // I think "silence" is supposed to be restricted to the session sending the command?.
            if (! silent) {
                notifyFlagUpdate(uid, message.getFlags());
            }
        }

        private void notifyFlagUpdate(long uid, Flags flags) throws MailboxException {
            synchronized(_mailboxListeners) {
                for (int i = 0; i < _mailboxListeners.size(); i++) {
                    MailboxListener listener = (MailboxListener) _mailboxListeners.get(i);

                    listener.flagsUpdated(uid, flags);
                }
            }
        }

        public void replaceFlags(Flags flags, long uid, boolean silent) throws MailboxException {
            SimpleImapMessage message = getMessage(uid);
            if (message == null) {
                throw new MailboxException( "Message doesn't exist" );
            }
            message.getFlags().remove(MessageFlags.ALL_FLAGS);
            message.getFlags().add(flags);

            if (! silent) {
                notifyFlagUpdate(uid, message.getFlags());
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

        private void deleteMessage( long uid )
        {
            SimpleImapMessage message = getMessage(uid);
            mailMessages.remove(message);
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

            long[] allUids = getMessageUids();
            for (int i = 0; i < allUids.length; i++) {
                long uid = allUids[i];
                SimpleImapMessage message = getMessage(uid);
                if (message.getFlags().contains(Flags.Flag.DELETED)) {
                    expungeMessage(uid);
                }
            }
        }

        private void expungeMessage(long uid) throws MailboxException {
            // Notify all the listeners of the pending delete
            synchronized (_mailboxListeners) {
                for (int j = 0; j < _mailboxListeners.size(); j++) {
                    MailboxListener expungeListener = (MailboxListener) _mailboxListeners.get(j);
                    expungeListener.expunged(uid);
                }
            }

            deleteMessage(uid);
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
