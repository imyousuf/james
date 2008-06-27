/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.AccessControlException;
import org.apache.james.imapserver.ACLMailbox;
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionState;

import java.util.StringTokenizer;
import java.util.List;

class SelectCommand extends AuthenticatedSelectedStateCommand
{
    public SelectCommand()
    {
        this.commandName = "SELECT";

        this.getArgs().add( new AstringArgument( "mailbox" ) );
    }

    protected boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        String command = this.getCommand();

        // selecting a mailbox deselects current mailbox,
        // even if this select fails
        if ( session.getState() == ImapSessionState.SELECTED ) {
            session.getCurrentMailbox().removeMailboxEventListener( session );
            session.getImapHost().releaseMailbox( session.getCurrentUser(), session.getCurrentMailbox() );
            session.setState( ImapSessionState.AUTHENTICATED );
            session.setCurrentMailbox( null );
            session.setCurrentIsReadOnly( false );
        }

        String folder = (String) argValues.get( 0 );
        ACLMailbox mailbox = getMailbox( session, folder, command );
        if ( mailbox == null ) {
            return true;
        }
        else {
            session.setCurrentMailbox( mailbox );
        }

        try { // long tries clause against an AccessControlException
            if ( !session.getCurrentMailbox().hasReadRights( session.getCurrentUser() ) ) {
                session.noResponse( command, "Read access not granted." );
                return true;
            }
            if ( command.equalsIgnoreCase( "SELECT" ) ) {
                if ( !session.getCurrentMailbox().isSelectable( session.getCurrentUser() ) ) {
                    session.noResponse( "Mailbox exists but is not selectable" );
                    return true;
                }
            }

            // Have mailbox with at least read rights. Server setup.
            session.getCurrentMailbox().addMailboxEventListener( session );
            session.setCurrentFolder( folder );
            session.setState( ImapSessionState.SELECTED );
            getLogger().debug( "Current folder for user " + session.getCurrentUser() + " from "
                               + session.getRemoteHost() + "(" + session.getRemoteIP() + ") is "
                               + session.getCurrentFolder() );

            // Inform client
            session.getOut().println( UNTAGGED + SP + "FLAGS ("
                                       + session.getCurrentMailbox().getSupportedFlags() + ")" );
            if ( !session.getCurrentMailbox().allFlags( session.getCurrentUser() ) ) {
                session.untaggedResponse( " [PERMANENTFLAGS ("
                                           + session.getCurrentMailbox().getPermanentFlags( session.getCurrentUser() )
                                           + ") ]" );
            }
            session.checkSize();
            session.getOut().println( UNTAGGED + SP + OK + " [UIDVALIDITY "
                                       + session.getCurrentMailbox().getUIDValidity() + "]" );
            int oldestUnseen = session.getCurrentMailbox().getOldestUnseen( session.getCurrentUser() );
            if ( oldestUnseen > 0 ) {
                session.getOut().println( UNTAGGED + SP + OK + " [UNSEEN "
                                           + oldestUnseen + "] " + oldestUnseen + " is the first unseen" );
            }
            else {
                session.getOut().println( UNTAGGED + SP + OK + " No unseen messages" );
            }
            session.setSequence( session.getCurrentMailbox().listUIDs( session.getCurrentUser() ));

            if ( command.equalsIgnoreCase( "EXAMINE" ) ) {
                session.setCurrentIsReadOnly( true );

                session.okResponse("[READ-ONLY] " + command );
                return true;

            }
            else if ( session.getCurrentMailbox().isReadOnly( session.getCurrentUser() ) ) {
                session.setCurrentIsReadOnly( true );
                session.okResponse( "[READ-ONLY] " + command );
                return true;
            }
            session.okResponse( "[READ-WRITE] " + command );
            return true;
        }
        catch ( AccessControlException ace ) {
            session.noResponse( command, "No such mailbox." );
            session.logACE( ace );
            return true;
        }
    }
}
