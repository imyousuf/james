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

class GetAclCommand extends AbstractAclCommand
{
    protected boolean checkUsage( int arguments, ImapSession session )
    {
        if ( arguments != 3 ) {
            session.badResponse( "Command should be <tag> <GETACL> <mailbox>" );
            return false;
        }
        return true;
    }

    protected void doAclCommand( ImapRequest request, ImapSession session, ACLMailbox target, String folder ) throws AccessControlException, AuthorizationException
    {
        session.untaggedResponse( "ACL " + target.getName() + SP + target.getAllRights( session.getCurrentUser() ) );
        session.okResponse( request.getCommand() );
    }       
}
