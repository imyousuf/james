/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.store;

import org.apache.james.services.MailRepository;
import org.apache.james.core.MailImpl;

import javax.mail.internet.MimeMessage;
import java.util.Date;
import java.util.Collection;

/**
 * Represents a mailbox within an {@link org.apache.james.imapserver.store.ImapStore}.
 * May provide storage for MailImpl objects, or be a non-selectable placeholder in the
 * Mailbox hierarchy.
 * TODO this is a "grown" interface, which needs some more design and thought re:
 * how it will fit in with the other mail storage in James.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.3 $
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

    int getIndex( int uid );

    boolean isSelectable();

    long getUidNext();

    int getUnseenCount();

    ImapMessage createMessage( MimeMessage message, MessageFlags flags, Date internalDate );

    void storeMessage( ImapMessage message );

    void store( MailImpl mail) throws Exception;

    ImapMessage getMessage( long uid );

    Collection getMessages();


}
