/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.AccessControlException;
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.MailboxException;
import org.apache.james.imapserver.ImapSessionState;

import java.util.StringTokenizer;

class SubscribeCommand extends AuthenticatedSelectedStateCommand
{
    public boolean process( ImapRequest request, ImapSession session )
    {
        int arguments = request.arguments();
        StringTokenizer commandLine = request.getCommandLine();
        String command = request.getCommand();

        String folder;
        if ( arguments != 1 ) {
            session.badResponse( "Command should be <tag> <SUBSCRIBE> <mailbox>" );
            return true;
        }
        folder = decodeMailboxName( commandLine.nextToken() );

        try {
            if ( session.getImapHost().subscribe( session.getCurrentUser(), folder ) ) {
                session.okResponse( command );
            }
            else {
                session.noResponse( command );
            }
        }
        catch ( MailboxException mbe ) {
            if ( mbe.isRemote() ) {
                session.noResponse( command, "[REFERRAL "
                                           + mbe.getRemoteServer() + "]"
                                           + SP + "Wrong server. Try remote." );
            }
            else {
                session.noResponse( command, "No such mailbox" );
            }
            return true;
        }
        catch ( AccessControlException ace ) {
            session.noResponse( command, "No such mailbox" );
            session.logACE( ace );
            return true;
        }
        if ( session.getState() == ImapSessionState.SELECTED ) {
            session.checkSize();
            session.checkExpunge();
        }
        return true;
    }
}
