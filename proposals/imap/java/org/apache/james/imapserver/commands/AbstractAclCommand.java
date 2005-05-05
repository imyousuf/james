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
import org.apache.james.util.Assert;
import org.apache.james.imapserver.ACLMailbox;
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionState;

import java.util.StringTokenizer;
import java.util.List;

abstract class AbstractAclCommand extends AuthenticatedSelectedStateCommand
{
    protected abstract boolean checkUsage( int arguments, ImapSession session  );
        
    protected abstract void doAclCommand( ImapRequest request, ImapSession session,
                                          ACLMailbox target, String folder )
            throws AccessControlException, AuthorizationException;

    protected boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        Assert.fail();
        return false;
    }

    public boolean process( ImapRequest request, ImapSession session )
    {
        int arguments = request.arguments();
        StringTokenizer commandLine = request.getCommandLine();
        String command = request.getCommand();

        String folder;
        ACLMailbox target = null;
            
        checkUsage( arguments, session );
            
        folder = readAstring( commandLine );
            
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
        if ( session.getState() == ImapSessionState.SELECTED ) {
            session.checkSize();
            session.checkExpunge();
        }
        return true;
    }
}
