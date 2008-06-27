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

import org.apache.james.services.MailRepository;
import org.apache.james.core.MailImpl;

import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;
import java.util.Date;
import java.util.Collection;

/**
 * Represents a mailbox within an {@link org.apache.james.imapserver.store.ImapStore}.
 * May provide storage for MailImpl objects, or be a non-selectable placeholder in the
 * Mailbox hierarchy.
 * TODO this is a "grown" interface, which needs some more design and thought re:
 * how it will fit in with the other mail storage in James.
 *
 *
 * @version $Revision: 1.4.2.3 $
 */
public interface ImapMailbox
{
    String getName();

    String getFullName();

    MessageFlags getAllowedFlags();

    int getMessageCount();

    int getRecentCount();

    long getUidValidity();

    int getFirstUnseen();

    int getMsn( long uid ) throws MailboxException;

    boolean isSelectable();

    long getUidNext();

    int getUnseenCount();

    SimpleImapMessage createMessage( MimeMessage message, MessageFlags flags, Date internalDate );

    void updateMessage( SimpleImapMessage message ) throws MailboxException;

    void store( MailImpl mail) throws Exception;

    SimpleImapMessage getMessage( long uid );

    long[] getMessageUids();

    void deleteMessage( long uid );
}
