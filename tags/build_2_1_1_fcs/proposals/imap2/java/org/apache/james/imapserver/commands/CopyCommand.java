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
import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.store.SimpleImapMessage;

/**
 * Handles processeing for the COPY imap command.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
class CopyCommand extends SelectedStateCommand implements UidEnabledCommand
{
    public static final String NAME = "COPY";
    public static final String ARGS = "<message-set> <mailbox>";

    /** @see CommandTemplate#doProcess */
    protected void doProcess( ImapRequestLineReader request,
                              ImapResponse response,
                              ImapSession session )
        throws ProtocolException, MailboxException
    {
        doProcess( request, response, session, false );
    }

    public void doProcess( ImapRequestLineReader request,
                              ImapResponse response,
                              ImapSession session,
                              boolean useUids)
            throws ProtocolException, MailboxException
    {
        IdSet idSet = parser.set( request );
        String mailboxName = parser.mailbox( request );
        parser.endLine( request );

        ImapMailbox currentMailbox = session.getSelected();
        ImapMailbox toMailbox;
        try {
            toMailbox = getMailbox( mailboxName, session, true );
        }
        catch ( MailboxException e ) {
            e.setResponseCode( "TRYCREATE" );
            throw e;
        }

        long[] uids = currentMailbox.getMessageUids();
        for ( int i = 0; i < uids.length; i++ ) {
            long uid = uids[i];
            boolean inSet;
            if ( useUids ) {
                inSet = idSet.includes( uid );
            }
            else {
                int msn = currentMailbox.getMsn( uid );
                inSet = idSet.includes( msn );
            }

            if ( inSet ) {
                session.getHost().copyMessage( uid, currentMailbox, toMailbox );
            }
        }

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
