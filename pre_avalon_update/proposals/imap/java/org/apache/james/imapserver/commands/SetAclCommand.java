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

class SetAclCommand extends AbstractAclCommand
{
    public boolean checkUsage( int arguments, ImapSession session )
    {
        if ( arguments != 5 ) {
            session.badResponse( "Command should be <tag> <SETACL> <mailbox> <identity> <rights modification>" );
            return false;
        }
        return true;
    }
        
    public void doAclCommand( ImapRequest request, ImapSession session,
                              ACLMailbox target, String folder ) throws AccessControlException, AuthorizationException
    {
        String identity = request.getCommandLine().nextToken();
        String changes = request.getCommandLine().nextToken();
                
        if ( target.setRights( session.getCurrentUser(), identity, changes ) ) {
            session.okResponse( request.getCommand() );
            session.getSecurityLogger().info( "ACL rights for " + identity + " in "
                                 + folder + " changed by " + session.getCurrentUser() + " : "
                                 + changes );
        }
        else {
            session.noResponse( request.getCommand() );
            session.getSecurityLogger().info( "Failed attempt to change ACL rights for "
                                 + identity + " in " + folder + " by "
                                 + session.getCurrentUser() );
        }
    }       
}
