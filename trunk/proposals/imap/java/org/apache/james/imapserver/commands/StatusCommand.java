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
import java.util.List;
import java.util.ArrayList;

class StatusCommand extends AuthenticatedSelectedStateCommand
{
    public boolean process( ImapRequest request, ImapSession session )
    {
        int arguments = request.arguments();
        StringTokenizer commandLine = request.getCommandLine();
        String command = request.getCommand();

        String folder;
        if ( arguments < 4 ) {
            session.badResponse( "Command should be <tag> <STATUS> <mailboxname> (status data items)" );
            return true;
        }
        folder = decodeMailboxName( commandLine.nextToken() );
        List dataNames = new ArrayList();
        String attr = commandLine.nextToken();
        if ( !attr.startsWith( "(" ) ) { //single attr
            session.badResponse( "Command should be <tag> <STATUS> <mailboxname> (status data items)" );
            return true;
        }
        else if ( attr.endsWith( ")" ) ) { //single attr in paranthesis
            dataNames.add( attr.substring( 1, attr.length() - 1 ) );
        }
        else { // multiple attrs
            dataNames.add( attr.substring( 1 ).trim() );
            while ( commandLine.hasMoreTokens() ) {
                attr = commandLine.nextToken();
                if ( attr.endsWith( ")" ) ) {
                    dataNames.add( attr.substring( 0, attr.length() - 1 ) );
                }
                else {
                    dataNames.add( attr );
                }
            }
        }
        try {
            String response = session.getImapHost().getMailboxStatus( session.getCurrentUser(), folder,
                                                         dataNames );
            session.untaggedResponse( " STATUS " + folder + " ("
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
