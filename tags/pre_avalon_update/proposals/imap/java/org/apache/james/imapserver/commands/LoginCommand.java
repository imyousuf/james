/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.AuthenticationException;
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionState;

import java.util.StringTokenizer;
import java.util.List;

class LoginCommand extends NonAuthenticatedStateCommand
{
    LoginCommand()
    {
        this.commandName = "LOGIN";

        this.getArgs().add( new AstringArgument( "username" ) );
        this.getArgs().add( new AstringArgument( "password" ) );
    }

    protected boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        String userName = (String) argValues.get(0);
        String password = (String) argValues.get(1);

        session.setCurrentUser( userName );
        if ( session.getUsers().test( session.getCurrentUser(), password ) ) {
            session.getSecurityLogger().info( "Login successful for " + session.getCurrentUser() + " from  "
                                 + session.getRemoteHost() + "(" + session.getRemoteIP() + ")" );
            // four possibilites handled:
            // private mail: isLocal, is Remote
            // other mail (shared, news, etc.) is Local, is Remote

            if ( session.getImapHost().isHomeServer( session.getCurrentUser() ) ) {
                session.okResponse( request.getCommand() );
                session.setState( ImapSessionState.AUTHENTICATED );

            }
            else {
                String remoteServer = null;
                try {
                    remoteServer
                            = session.getImapSystem().getHomeServer( session.getCurrentUser() );
                }
                catch ( AuthenticationException ae ) {
                    session.setConnectionClosed( session.closeConnection( TAGGED_NO,
                                               " cannot find your inbox, closing connection",
                                               "" ) );
                    return false;
                }

                if ( session.getImapHost().hasLocalAccess( session.getCurrentUser() ) ) {
                    session.okResponse( "[REFERRAL "
                                               + remoteServer + "]" + SP
                                               + "Your home server is remote, other mailboxes available here" );
                    session.setState( ImapSessionState.AUTHENTICATED );

                }
                else {
                    session.closeConnection( TAGGED_NO, " [REFERRAL" + SP
                                                + remoteServer + "]" + SP
                                                + "No mailboxes available here, try remote server", "" );
                    return false;
                }
            }
            session.setCurrentNamespace( session.getImapHost().getDefaultNamespace( session.getCurrentUser() ) );
            session.setCurrentSeperator( session.getImapSystem().getHierarchySeperator( session.getCurrentNamespace() ) );
            // position at root of default Namespace,
            // which is not actually a folder
            session.setCurrentFolder( session.getCurrentNamespace() + session.getCurrentSeperator() + "" );
            getLogger().debug( "Current folder for user " + session.getCurrentUser() + " from "
                               + session.getRemoteHost() + "(" + session.getRemoteIP() + ") is "
                               + session.getCurrentFolder() );
            return true;


        } // failed password test

        // We should add ability to monitor attempts to login
        session.noResponse( request.getCommand() );
        session.getSecurityLogger().error( "Failed attempt to use Login command for account "
                              + session.getCurrentUser() + " from " + session.getRemoteHost() + "(" + session.getRemoteIP()
                              + ")" );
        return true;
    }
}
