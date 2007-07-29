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


package org.apache.james.imapserver.store;

import org.apache.james.core.MailImpl;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;
import java.util.Date;

/**
 * Represents a mailbox within an {@link org.apache.james.imapserver.store.ImapStore}.
 * May provide storage for MailImpl objects, or be a non-selectable placeholder in the
 * Mailbox hierarchy.
 * TODO this is a "grown" interface, which needs some more design and thought re:
 * how it will fit in with the other mail storage in James.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision$
 */
public interface ImapMailbox
{
    String getName();

    String getFullName();

    Flags getPermanentFlags();

    int getMessageCount();

    int getRecentCount(boolean reset);

    long getUidValidity();

    int getFirstUnseen();

    int getUnseenCount();

    boolean isSelectable();

    long getUidNext();

    long appendMessage( MimeMessage message, Flags flags, Date internalDate );

    void deleteAllMessages();

    void expunge() throws MailboxException;

    void addListener(MailboxListener listener);

    void removeListener(MailboxListener listener);

    void store( MailImpl mail) throws Exception;

    SimpleImapMessage getMessage( long uid );

    long[] getMessageUids();

    long[] search(SearchTerm searchTerm);

    void copyMessage( long uid, ImapMailbox toMailbox )
            throws MailboxException;

    void setFlags(Flags flags, boolean value, long uid, MailboxListener silentListener, boolean addUid) throws MailboxException;

    void replaceFlags(Flags flags, long uid, MailboxListener silentListener, boolean addUid) throws MailboxException;

    int getMsn( long uid ) throws MailboxException;

    void signalDeletion();

}
