/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.store.MailboxException;

import java.util.Collection;

/**
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.2 $
 */
class LsubCommand extends ListCommand
{
    public static final String NAME = "LSUB";

    protected Collection doList( ImapSession session, String searchPattern )
            throws MailboxException
    {
        return session.getHost().listSubscribedMailboxes( session.getUser(), searchPattern );
    }

    /** @see ImapCommand#getName */
    public String getName()
    {
        return NAME;
    }
}
