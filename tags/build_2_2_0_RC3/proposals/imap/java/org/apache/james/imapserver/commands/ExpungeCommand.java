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
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;

import java.util.StringTokenizer;
import java.util.List;

class ExpungeCommand extends SelectedStateCommand
{
    public ExpungeCommand()
    {
        this.commandName = "EXPUNGE";
    }

    public boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        String command = this.getCommand();

        try {
            if ( session.getCurrentMailbox().expunge( session.getCurrentUser() ) ) {
                session.checkExpunge();
                session.checkSize();
                session.okResponse( command );
            }
            else {
                session.noResponse( command, "Unknown server error." );
            }
            return true;
        }
        catch ( AccessControlException ace ) {
            session.noResponse( command, "No such mailbox" );
            session.logACE( ace );
            return true;
        }
        catch ( AuthorizationException aze ) {
            session.noResponse( command, "You do not have the rights to expunge mailbox: " + session.getCurrentMailbox().getAbsoluteName() );
            session.logAZE( aze );
            return true;
        }
        catch ( Exception e ) {
            session.noResponse( command, "Unknown server error." );
            getLogger().error( "Exception expunging mailbox " + 
                                       session.getCurrentMailbox().getAbsoluteName() + " by user " + 
                                       session.getCurrentUser() + " was : " + e );
            if ( DEEP_DEBUG ) {
                e.printStackTrace();
            }
            return true;
        }
    }
}
