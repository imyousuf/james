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

import org.apache.james.core.MailImpl;
import org.apache.james.imapserver.ImapConstants;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.LogKitLogger;
import org.apache.avalon.framework.logger.ConsoleLogger;
import org.apache.log.Logger;

import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * A simple in-memory implementation of {@link ImapStore}, used for testing
 * and development. Note: this implementation does not persist *anything* to disk.
 *
 *
 * @version $Revision: 1.4.2.3 $
 */
public class InMemoryStore
        extends AbstractLogEnabled
        implements ImapStore, ImapConstants
{
    private RootMailbox rootMailbox = new RootMailbox();
    private static MessageFlags mailboxFlags = new MessageFlags();
    static {
        mailboxFlags.setAnswered( true );
        mailboxFlags.setDeleted( true );
        mailboxFlags.setDraft( true );
        mailboxFlags.setFlagged( true );
        mailboxFlags.setSeen( true );
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

        private Map mailMessages = new TreeMap();
        private long nextUid = 10;
        private long uidValidity;

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

        public MessageFlags getAllowedFlags()
        {
            return mailboxFlags;
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
            return 0;
        }

        public int getFirstUnseen()
        {
            return 0;
        }

        public int getRecentCount()
        {
            return 0;
        }

        public int getMsn( long uid ) throws MailboxException
        {
            Collection keys = mailMessages.keySet();
            Iterator iterator = keys.iterator();
            for ( int msn = 1; iterator.hasNext(); msn++ ) {
                Long messageUid = ( Long ) iterator.next();
                if ( messageUid.longValue() == uid ) {
                    return msn;
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

        public SimpleImapMessage createMessage( MimeMessage message,
                                          MessageFlags flags,
                                          Date internalDate )
        {
            long uid = nextUid;
            nextUid+=10;

            SimpleImapMessage imapMessage = new SimpleImapMessage( message, flags,
                                                       internalDate, uid );
            setupLogger( imapMessage );

            mailMessages.put( new Long( uid ), imapMessage );
            return imapMessage;
        }

        public void updateMessage( SimpleImapMessage message )
                throws MailboxException
        {

            Long key = new Long( message.getUid() );
            if ( ! mailMessages.containsKey( key ) ) {
                throw new MailboxException( "Message doesn't exist" );
            }
            mailMessages.put( key, message );
        }

        public void store( MailImpl mail )
                throws Exception
        {
            MimeMessage message = mail.getMessage();
            Date internalDate = new Date();
            MessageFlags flags = new MessageFlags();
            createMessage( message, flags, internalDate );
        }

        public SimpleImapMessage getMessage( long uid )
        {
            return (SimpleImapMessage)mailMessages.get( new Long(uid ) );
        }

        public long[] getMessageUids()
        {
            long[] uids = new long[ mailMessages.size() ];
            Collection keys = mailMessages.keySet();
            Iterator iterator = keys.iterator();
            for ( int i = 0; iterator.hasNext(); i++ ) {
                Long key = ( Long ) iterator.next();
                uids[i] = key.longValue();
            }
            return uids;
        }

        public void deleteMessage( long uid )
        {
            mailMessages.remove( new Long( uid ) );
        }

    }
}
