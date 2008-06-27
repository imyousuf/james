/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.AccessControlException;
import org.apache.james.AuthorizationException;
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;

import java.util.StringTokenizer;
import java.util.List;

class ExpungeCommand extends SelectedStateCommand
{
    public ExpungeCommand()
    {
        this.commandName = "EXPUNGE";
    }

    public boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        String command = this.getCommand();

        try {
            if ( session.getCurrentMailbox().expunge( session.getCurrentUser() ) ) {
                session.checkExpunge();
                session.checkSize();
                session.okResponse( command );
            }
            else {
                session.noResponse( command, "Unknown server error." );
            }
            return true;
        }
        catch ( AccessControlException ace ) {
            session.noResponse( command, "No such mailbox" );
            session.logACE( ace );
            return true;
        }
        catch ( AuthorizationException aze ) {
            session.noResponse( command, "You do not have the rights to expunge mailbox: " + session.getCurrentMailbox().getAbsoluteName() );
            session.logAZE( aze );
            return true;
        }
        catch ( Exception e ) {
            session.noResponse( command, "Unknown server error." );
            getLogger().error( "Exception expunging mailbox " + 
                                       session.getCurrentMailbox().getAbsoluteName() + " by user " + 
                                       session.getCurrentUser() + " was : " + e );
            if ( DEEP_DEBUG ) {
                e.printStackTrace();
            }
            return true;
        }
    }
}
