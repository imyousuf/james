/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.AuthenticationException;
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
