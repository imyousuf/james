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
import org.apache.james.imapserver.*;

import java.util.StringTokenizer;
import java.util.List;

class CreateCommand extends AuthenticatedSelectedStateCommand
{
    public CreateCommand()
    {
        this.commandName = "CREATE";
        this.getArgs().add( new AstringArgument( "mailbox" ) );
    }

    protected boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        String command = this.commandName;
        String folder = (String) argValues.get( 0 );

        try {
            if ( session.getCurrentFolder() == folder ) {
                session.noResponse( command, "Folder exists and is selected." );
                return true;
            }

            ACLMailbox target = session.getImapHost().createMailbox( session.getCurrentUser(), folder );
            session.okResponse( command );
            session.getImapHost().releaseMailbox( session.getCurrentUser(), target );
        }
        catch ( AccessControlException ace ) {
            session.noResponse( command, "No such mailbox." );
            session.logACE( ace );
            return true;
        }
        catch ( MailboxException mbe ) {
            if ( mbe.isRemote() ) {
                session.noResponse( "[REFERRAL "
                            + mbe.getRemoteServer() + "]"
                            + SP + "Wrong server. Try remote." );
            }
            else {
                session.noResponse( mbe.getStatus() );
            }
            return true;
        }
        catch ( AuthorizationException aze ) {
            session.noResponse( command, "You do not have the rights to create mailbox: "
                                 + folder );
            return true;
        }
        if ( session.getState() == ImapSessionState.SELECTED ) {
            session.checkSize();
            session.checkExpunge();
        }
        return true;
    }
}
