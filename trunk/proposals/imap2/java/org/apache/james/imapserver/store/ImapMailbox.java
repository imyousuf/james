/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.store;

import org.apache.james.services.MailRepository;

/**
 * Represents a mailbox within an {@link org.apache.james.imapserver.store.ImapStore}.
 * May provide storage for MailImpl objects, or be a non-selectable placeholder in the
 * Mailbox hierarchy.
 * TODO this is a "grown" interface, which needs some more design and thought re:
 * how it will fit in with the other mail storage in James.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.2 $
 */
public interface ImapMailbox extends MailRepository
{
    String getName();

    String getFullName();

    MessageFlags getAllowedFlags();

    int getMessageCount();

    int getRecentCount();

    int getUidValidity();

    int getFirstUnseen();

    int getIndex( int uid );

    boolean isSelectable();

    int getUidNext();

    int getUnseenCount();
    
}
