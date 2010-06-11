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

import java.util.StringTokenizer;

class ListRightsCommand extends AbstractAclCommand
{
    protected boolean checkUsage( int arguments, ImapSession session )
    {
        if ( arguments != 4 ) {
            session.taggedResponse( BAD_LISTRIGHTS_MSG );
            return false;
        }
        return true;
    }

    protected void doAclCommand( ImapRequest request, ImapSession session,
                                 ACLMailbox target, String folder )
            throws AccessControlException, AuthorizationException
    {
        StringTokenizer commandLine = request.getCommandLine();
        String command = request.getCommand();
        String identity = commandLine.nextToken();
        session.untaggedResponse( command + SP + target.getName() + SP + identity + SP
                          + target.getRequiredRights( session.getCurrentUser(), identity )
                          + SP
                          + target.getOptionalRights( session.getCurrentUser(), identity ) );
        session.okResponse( command );
    }

}
