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
import org.apache.james.imapserver.ImapSessionState;
import org.apache.james.imapserver.MailboxException;

import java.util.StringTokenizer;
import java.util.List;

class DeleteCommand extends AuthenticatedSelectedStateCommand
{
    public DeleteCommand()
    {
        this.commandName = "DELETE";

        this.getArgs().add( new AstringArgument( "mailbox" ) );
    }

    public boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        String command = this.getCommand();

        String folder = (String) argValues.get( 0 );

        if ( session.getCurrentFolder().equals( folder ) ) {
            session.noResponse( command, "You can't delete a folder while you have it selected." );
            return true;
        }
        try {
            if ( session.getImapHost().deleteMailbox( session.getCurrentUser(), folder ) ) {
                session.okResponse( command );
            }
            else {
                session.noResponse( command );
                getLogger().info( "Attempt to delete mailbox " + folder
                                  + " by user " + session.getCurrentUser() + " failed." );
            }
            if ( session.getState() == ImapSessionState.SELECTED ) {
                session.checkSize();
                session.checkExpunge();
            }
            return true;
        }
        catch ( MailboxException mbe ) {
            session.noResponse( command, mbe.getMessage() );
            return true;
        }
        catch ( AuthorizationException aze ) {
            session.noResponse( command, "You do not have the rights to delete mailbox: " + folder );
            session.logAZE( aze );
            return true;
        }
        catch ( AccessControlException ace ) {
            session.noResponse( command, ace.getMessage() );
            session.logACE( ace );
            return true;
        }
    }
}
