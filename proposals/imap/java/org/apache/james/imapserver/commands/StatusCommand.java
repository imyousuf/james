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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Implements the Status Command for getting the status of a Mailbox.
 *
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.2 on 04 Aug 2002
 */
class StatusCommand extends AuthenticatedSelectedStateCommand
{
    public StatusCommand()
    {
        this.commandName = "STATUS";

       // this.getArgs().add( "mailbox" );
       // this.getArgs().add( "status data item" );
        this.getArgs().add( new AstringArgument( "mailbox" ) );
        this.getArgs().add( new ListArgument( "status data item" ) );
    }

    protected boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        String command = this.getCommand();

        String folder = (String) argValues.get( 0 );
        List dataNames = (List) argValues.get( 1 );

        try {
            String response = session.getImapHost().getMailboxStatus( session.getCurrentUser(), folder,
                                                         dataNames );
            session.untaggedResponse( " STATUS \"" + folder + "\" ("
                                       + response + ")" );
            session.okResponse( command );
        }
        catch ( MailboxException mbe ) {
            if ( mbe.isRemote() ) {
                session.noResponse( command , "[REFERRAL "
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
