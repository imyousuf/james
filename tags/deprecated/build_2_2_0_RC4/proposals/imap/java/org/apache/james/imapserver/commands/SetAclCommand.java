/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.AccessControlException;
import org.apache.james.imapserver.AuthorizationException;
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
