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

class CopyCommand extends SelectedStateCommand
{
    public boolean process( ImapRequest request, ImapSession session )
    {
        int arguments = request.arguments();
        StringTokenizer commandLine = request.getCommandLine();
        String command = request.getCommand();

        if ( arguments < 4 ) {
            session.badResponse( "Command should be <tag> <COPY> <message set> <mailbox name>" );
            return true;
        }
        List set = session.decodeSet( commandLine.nextToken(),
                              session.getCurrentMailbox().getExists() );
        getLogger().debug( "Fetching message set of size: " + set.size() );
        String targetFolder = decodeMailboxName( commandLine.nextToken() );


        ACLMailbox targetMailbox = getMailbox( session, targetFolder, command );
        if ( targetMailbox == null ) {
            return true;
        }
        try { // long tries clause against an AccessControlException
            if ( !session.getCurrentMailbox().hasInsertRights( session.getCurrentUser() ) ) {
                session.noResponse( command, "Insert access not granted." );
                return true;
            }
            for ( int i = 0; i < set.size(); i++ ) {
                int msn = ((Integer) set.get( i )).intValue();
                MessageAttributes attrs = session.getCurrentMailbox().getMessageAttributes( msn, session.getCurrentUser() );
            }
        }
        catch ( AccessControlException ace ) {
            session.noResponse( command, "No such mailbox." );
            session.logACE( ace );
            return true;
        }
        catch ( AuthorizationException aze ) {
            session.noResponse( command, "You do not have the rights to expunge mailbox: " + targetFolder );
            session.logAZE( aze );
            return true;
        }

        session.okResponse( command );
        return true;
    }
}
