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
import org.apache.james.imapserver.ImapSessionState;
import org.apache.james.imapserver.MailboxException;

import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.List;

class ListCommand extends AuthenticatedSelectedStateCommand
{
    public ListCommand()
    {
        this.commandName = "LIST";

        this.getArgs().add( new AstringArgument( "reference name" ) );
        this.getArgs().add( new AstringArgument( "mailbox" ) );
    }

    protected boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        String command = this.commandName;

        boolean subscribeOnly;
        if ( command.equalsIgnoreCase( "LIST" ) ) {
            subscribeOnly = false;
        }
        else {
            subscribeOnly = true;
        }

        String reference = (String) argValues.get( 0 );
        String folder = (String) argValues.get( 1 );

        Collection list = null;
        try {
            list = session.getImapHost().listMailboxes( session.getCurrentUser(), reference, folder,
                                                        subscribeOnly );
            if ( list == null ) {
                session.noResponse( command, " unable to interpret mailbox" );
            }
            else if ( list.size() == 0 ) {
                getLogger().debug( "List request matches zero mailboxes: " + request.getCommandRaw() );
                session.okResponse( command );
            }
            else {
                Iterator it = list.iterator();
                while ( it.hasNext() ) {
                    String listResponse = (String) it.next();
                    session.getOut().println( UNTAGGED + SP + command.toUpperCase()
                                               + SP + listResponse );
                    getLogger().debug( UNTAGGED + SP + command.toUpperCase()
                                       + SP + listResponse );
                }
                session.okResponse( command );
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
