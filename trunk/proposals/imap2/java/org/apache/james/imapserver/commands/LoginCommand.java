/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapRequestParser;
import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.services.User;

/**
 * Handles processeing for the LOGIN imap command.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
class LoginCommand extends NonAuthenticatedStateCommand
{
    public static final String NAME = "LOGIN";
    public static final String ARGS = "<userid> <password>";

    /** @see CommandTemplate#doProcess */
    protected void doProcess( ImapRequestParser request,
                              ImapResponse response,
                              ImapSession session )
            throws ProtocolException
    {
        String userid = request.astring();
        String password = request.astring();
        request.endLine();

        if ( session.getUsers().test( userid, password ) ) {
            User user = session.getUsers().getUserByName( userid );
            session.setAuthenticated( user );
            response.commandComplete( this );

            // Log the login.
            session.getSecurityLogger().info( "Login successful for " + user.getUserName() +
                                              " from  " + session.getClientHostname() +
                                              "(" + session.getClientIP() + ")" );
        }
        else {
            response.commandFailed( this, "Invalid login/password" );
            session.getSecurityLogger().error( "Login failed for " + userid +
                                               " from " + session.getClientHostname() +
                                               "(" + session.getClientIP() + ")" );
        }
    }

    /** @see ImapCommand#getName */
    public String getName()
    {
        return NAME;
    }

    /** @see CommandTemplate#getArgSyntax */
    public String getArgSyntax()
    {
        return ARGS;
    }
}

/*
6.2.2.  LOGIN Command

   Arguments:  user name
               password

   Responses:  no specific responses for this command

   Result:     OK - login completed, now in authenticated state
               NO - login failure: user name or password rejected
               BAD - command unknown or arguments invalid

      The LOGIN command identifies the client to the server and carries
      the plaintext password authenticating this user.

   Example:    C: a001 LOGIN SMITH SESAME
               S: a001 OK LOGIN completed
*/
