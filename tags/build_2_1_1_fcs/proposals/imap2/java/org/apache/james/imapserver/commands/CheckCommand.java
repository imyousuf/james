/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ProtocolException;

/**
 * Handles processeing for the CHECK imap command.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
class CheckCommand extends SelectedStateCommand
{
    public static final String NAME = "CHECK";
    public static final String ARGS = null;

    /** @see CommandTemplate#doProcess */
    protected void doProcess( ImapRequestLineReader request,
                              ImapResponse response,
                              ImapSession session ) throws ProtocolException
    {
        parser.endLine( request );
        session.unsolicitedResponses( response );
        response.commandComplete( this );
    }

    /** @see ImapCommand#getName */
    public String getName()
    {
        return NAME;
    }

    /** @see CommandTemplate#getArgSyntax */
    public String getArgSyntax()
    {
        return ARGS;
    }
}
