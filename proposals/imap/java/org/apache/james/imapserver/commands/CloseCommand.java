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

class CloseCommand extends SelectedStateCommand
{
    public boolean process( ImapRequest request, ImapSession session )
    {
        try {
            session.getCurrentMailbox().expunge( session.getCurrentUser() );
        }
        catch ( Exception e ) {
            getLogger().error( "Exception while expunging mailbox on CLOSE : " + e );
        }
        session.getCurrentMailbox().removeMailboxEventListener( session );
        session.getImapHost().releaseMailbox( session.getCurrentUser(), session.getCurrentMailbox() );
        session.setState( AUTHENTICATED );
        session.setCurrentMailbox( null );
        session.setCurrentIsReadOnly( false );
        session.okResponse( request.getCommand() );
        return true;
    }
}
