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
import org.apache.james.imapserver.store.MailboxException;

/**
 * Handles processeing for the UID imap command.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
class UidCommand extends SelectedStateCommand
{
    public static final String NAME = "UID";
    public static final String ARGS = "<fetch-command>|<store-command>|<copy-command>|<search-command>";

    private ImapCommandFactory commandFactory;

    /** @see CommandTemplate#doProcess */
    protected void doProcess( ImapRequestLineReader request,
                              ImapResponse response,
                              ImapSession session )
            throws ProtocolException, MailboxException
    {
        String commandName = parser.atom( request );
        ImapCommand command = commandFactory.getCommand( commandName );
        if ( command == null ||
             ! (command instanceof UidEnabledCommand ) ) {
            throw new ProtocolException("Invalid UID command: '" + commandName + "'" );
        }

        ((UidEnabledCommand)command).doProcess( request, response, session, true );
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

    public void setCommandFactory( ImapCommandFactory imapCommandFactory )
    {
        this.commandFactory = imapCommandFactory;
    }
}
