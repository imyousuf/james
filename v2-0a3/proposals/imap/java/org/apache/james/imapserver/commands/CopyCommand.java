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
import org.apache.james.imapserver.ACLMailbox;
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.MessageAttributes;

import java.util.List;
import java.util.StringTokenizer;

class CopyCommand extends SelectedStateCommand
{
    public CopyCommand()
    {
        this.commandName = "COPY";

        this.getArgs().add( new SetArgument() );
        this.getArgs().add( new AstringArgument( "mailbox" ) );
    }

    protected boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        List set = (List) argValues.get( 0 );
        getLogger().debug( "Fetching message set of size: " + set.size() );
        String targetFolder = (String) argValues.get( 1 );

        ACLMailbox targetMailbox = getMailbox( session, targetFolder, this.commandName );
        if ( targetMailbox == null ) {
            return true;
        }
        try { // long tries clause against an AccessControlException
            if ( !session.getCurrentMailbox().hasInsertRights( session.getCurrentUser() ) ) {
                session.noResponse( this.commandName, "Insert access not granted." );
                return true;
            }
            // TODO - copy all messages in set.
            int msn = ((Integer)set.get( 0 ) ).intValue();
            session.getCurrentMailbox().getMessageAttributes( msn, session.getCurrentUser() );
        }
        catch ( AccessControlException ace ) {
            session.noResponse( this.commandName, "No such mailbox." );
            session.logACE( ace );
            return true;
        }
        catch ( AuthorizationException aze ) {
            session.noResponse( this.commandName, "You do not have the rights to expunge mailbox: " + targetFolder );
            session.logAZE( aze );
            return true;
        }

        session.okResponse( this.commandName );
        return true;
    }
}
