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
import org.apache.james.imapserver.ACLMailbox;
import org.apache.james.imapserver.MailboxException;

import java.util.StringTokenizer;

class CreateCommand extends AuthenticatedSelectedStateCommand
{
    public boolean process( ImapRequest request, ImapSession session )
    {
        int arguments = request.arguments();
        StringTokenizer commandLine = request.getCommandLine();
        String command = request.getCommand();
        String folder = null;

        if ( arguments != 1 ) {
            session.badResponse( "Command should be <tag> <CREATE> <mailbox>" );
            return true;
        }
        try {
            folder = decodeMailboxName( commandLine.nextToken() );
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
        if ( session.getState() == SELECTED ) {
            session.checkSize();
            session.checkExpunge();
        }
        return true;
    }
}
