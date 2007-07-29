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

package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionMailbox;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.store.MailboxException;

/**
 * Handles processeing for the COPY imap command.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision$
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
        IdRange[] idSet = parser.parseIdRange( request );
        String mailboxName = parser.mailbox( request );
        parser.endLine( request );

        ImapSessionMailbox currentMailbox = session.getSelected();
        ImapMailbox toMailbox;
        try {
            toMailbox = getMailbox( mailboxName, session, true );
        }
        catch ( MailboxException e ) {
            e.setResponseCode( "TRYCREATE" );
            throw e;
        }

//        if (! useUids) {
//            idSet = currentMailbox.toUidSet(idSet);
//        }
//        currentMailbox.copyMessages(toMailbox, idSet);
        long[] uids = currentMailbox.getMessageUids();
        for ( int i = 0; i < uids.length; i++ ) {
            long uid = uids[i];
            boolean inSet;
            if ( useUids ) {
                inSet = includes( idSet, uid );
            }
            else {
                int msn = currentMailbox.getMsn( uid );
                inSet = includes( idSet, msn );
            }

            if ( inSet ) {
                currentMailbox.copyMessage(uid, toMailbox);
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
