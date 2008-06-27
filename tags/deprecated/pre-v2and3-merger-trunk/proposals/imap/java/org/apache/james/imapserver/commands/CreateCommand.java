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
import org.apache.james.imapserver.*;

import java.util.StringTokenizer;
import java.util.List;

/**
 * Create Command for creating some new mailboxes.
 *
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.2 on 04 Aug 2002
 */

class CreateCommand extends AuthenticatedSelectedStateCommand
{
    public CreateCommand()
    {
        this.commandName = "CREATE";
        this.getArgs().add( new AstringArgument( "mailbox" ) );
    }

    protected boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        String command = this.commandName;
        String folder = (String) argValues.get( 0 );

        try {
            if ( session.getCurrentFolder() == folder ) {
                session.noResponse( command, "Folder exists and is selected." );
                return true;
            }
            System.out.println("CreteCommand FOLDERNAME: "+folder);
            ACLMailbox target = session.getImapHost().createMailbox( session.getCurrentUser(), folder );
            session.okResponse( command );
            session.getImapHost().releaseMailbox( session.getCurrentUser(), target );
        }
        catch ( AccessControlException ace ) {
            session.noResponse( command, "No such mailbox." );
            session.logACE( ace );
            return true;
        }
        catch ( MailboxException mbe ) {
            if ( mbe.isRemote() ) {
                session.noResponse( "[REFERRAL "
                            + mbe.getRemoteServer() + "]"
                            + SP + "Wrong server. Try remote." );
            }
            else {
                session.noResponse( mbe.getStatus() );
            }
            return true;
        }
        catch ( AuthorizationException aze ) {
            session.noResponse( command, "You do not have the rights to create mailbox: "
                                 + folder );
            return true;
        }
        if ( session.getState() == ImapSessionState.SELECTED ) {
            session.checkSize();
            session.checkExpunge();
        }
        return true;
    }
}
