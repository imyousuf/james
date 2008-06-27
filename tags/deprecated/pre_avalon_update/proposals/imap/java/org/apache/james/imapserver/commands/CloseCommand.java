/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionState;

import java.util.List;

class CloseCommand extends SelectedStateCommand
{
    public CloseCommand()
    {
        this.commandName = "CLOSE";
    }

    protected boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        try {
            session.getCurrentMailbox().expunge( session.getCurrentUser() );
        }
        catch ( Exception e ) {
            getLogger().error( "Exception while expunging mailbox on CLOSE : " + e );
        }
        session.getCurrentMailbox().removeMailboxEventListener( session );
        session.getImapHost().releaseMailbox( session.getCurrentUser(), session.getCurrentMailbox() );
        session.setState( ImapSessionState.AUTHENTICATED );
        session.setCurrentMailbox( null );
        session.setCurrentIsReadOnly( false );
        session.okResponse( request.getCommand() );
        return true;
    }
}
