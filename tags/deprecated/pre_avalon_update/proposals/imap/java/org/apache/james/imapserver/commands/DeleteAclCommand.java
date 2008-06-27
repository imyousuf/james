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

class DeleteAclCommand extends AbstractAclCommand
{
    protected boolean checkUsage( int arguments, ImapSession session )
    {
        if ( arguments != 4 ) {
            session.badResponse( "Command should be <tag> <DELETEACL> <mailbox> <identity>" );
            return false;
        }
        return true;
    }

    protected void doAclCommand( ImapRequest request, ImapSession session,
                                 ACLMailbox target, String folder )
            throws AccessControlException, AuthorizationException
    {
        String command = request.getCommand();
        String identity = request.getCommandLine().nextToken();
        String changes = "";
            
        if ( target.setRights( session.getCurrentUser(), identity, changes ) ) {
            session.okResponse( command );
            session.getSecurityLogger().info( "ACL rights for " + identity + " in "
                                 + folder + " deleted by " + session.getCurrentUser() );
        }
        else {
            session.noResponse( command );
            session.getSecurityLogger().info( "Failed attempt to delete ACL rights for "
                                 + identity + " in " + folder + " by "
                                 + session.getCurrentUser() );
        }
    }
}
