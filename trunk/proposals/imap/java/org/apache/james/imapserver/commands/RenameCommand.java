/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.AuthorizationException;
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.MailboxException;

import java.util.StringTokenizer;

class RenameCommand extends AuthenticatedSelectedStateCommand
{
    public boolean process( ImapRequest request, ImapSession session )
    {
        int arguments = request.arguments();
        StringTokenizer commandLine = request.getCommandLine();
        String command = request.getCommand();

        String folder;
        if ( arguments != 4 ) {
            session.badResponse( "Command should be <tag> <RENAME> <oldname> <newname>" );
            return true;
        }
        folder = decodeMailboxName( commandLine.nextToken() );
        String newName = decodeMailboxName( commandLine.nextToken() );
        if ( session.getCurrentFolder() == folder ) {
            session.noResponse( command, "You can't rename a folder while you have it selected." );
            return true;
        }
        try {
            if ( session.getImapHost().renameMailbox( session.getCurrentUser(), folder, newName ) ) {
                session.okResponse( command );
            }
            else {
                session.noResponse( command, "Rename failed, unknown error" );
                getLogger().info( "Attempt to rename mailbox " + folder
                                  + " to " + newName
                                  + " by user " + session.getCurrentUser() + " failed." );
            }
        }
        catch ( MailboxException mbe ) {
            if ( mbe.getStatus().equals( MailboxException.NOT_LOCAL ) ) {
                session.taggedResponse( NO_NOTLOCAL_MSG );
            }
            else {
                session.noResponse( command, mbe.getMessage() );
            }
            return true;
        }
        catch ( AuthorizationException aze ) {
            session.noResponse( command, "You do not have the rights to delete mailbox: " + folder );
            return true;
        }
        if ( session.getState() == SELECTED ) {
            session.checkSize();
            session.checkExpunge();
        }
        return true;
    }
}
