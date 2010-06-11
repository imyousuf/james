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
import org.apache.james.imapserver.ImapHost;
import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.store.MailboxException;

/**
 * Handles processeing for the EXPUNGE imap command.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
class ExpungeCommand extends SelectedStateCommand
{
    public static final String NAME = "EXPUNGE";
    public static final String ARGS = null;

    /** @see CommandTemplate#doProcess */
    protected void doProcess( ImapRequestLineReader request,
                              ImapResponse response,
                              ImapSession session )
            throws ProtocolException, MailboxException
    {
        parser.endLine( request );

        if ( session.selectedIsReadOnly() ) {
            response.commandFailed( this, "Mailbox selected read only." );
        }

        ImapMailbox mailbox = session.getSelected();
        ImapHost host = session.getHost();
        int[] msns = host.expunge( mailbox );
        for ( int i = 0; i < msns.length; i++ ) {
            int msn = msns[i];
            response.expungeResponse( msn );
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
/*
6.4.3.  EXPUNGE Command

   Arguments:  none

   Responses:  untagged responses: EXPUNGE

   Result:     OK - expunge completed
               NO - expunge failure: can't expunge (e.g. permission
                    denied)
               BAD - command unknown or arguments invalid

      The EXPUNGE command permanently removes from the currently
      selected mailbox all messages that have the \Deleted flag set.
      Before returning an OK to the client, an untagged EXPUNGE response
      is sent for each message that is removed.

   Example:    C: A202 EXPUNGE
               S: * 3 EXPUNGE
               S: * 3 EXPUNGE
               S: * 5 EXPUNGE
               S: * 8 EXPUNGE
               S: A202 OK EXPUNGE completed

      Note: in this example, messages 3, 4, 7, and 11 had the
      \Deleted flag set.  See the description of the EXPUNGE
      response for further explanation.
*/

