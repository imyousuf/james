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
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ACLMailbox;

import java.util.StringTokenizer;

abstract class AbstractAclCommand extends AuthenticatedSelectedStateCommand
{
    protected abstract boolean checkUsage( int arguments, ImapSession session  );
        
    protected abstract void doAclCommand( ImapRequest request, ImapSession session,
                                          ACLMailbox target, String folder )
            throws AccessControlException, AuthorizationException;

    public boolean process( ImapRequest request, ImapSession session )
    {
        int arguments = request.arguments();
        StringTokenizer commandLine = request.getCommandLine();
        String command = request.getCommand();

        String folder;
        ACLMailbox target = null;
            
        checkUsage( arguments, session );
            
        folder = decodeMailboxName( commandLine.nextToken() );
            
        target = getMailbox( session, folder, command );
        if ( target == null ) return true;
            
        try {
            doAclCommand( request, session, target, folder );
        }
        catch ( AccessControlException ace ) {
            session.noResponse( command, "Unknown mailbox" );
            session.logACE( ace );
            return true;
        }
        catch ( AuthorizationException aze ) {
            session.taggedResponse( AUTH_FAIL_MSG );
            session.logAZE( aze );
            return true;
        }
        if ( session.getState() == SELECTED ) {
            session.checkSize();
            session.checkExpunge();
        }
        return true;
    }
}
