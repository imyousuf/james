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
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionState;
import org.apache.james.imapserver.MailboxException;

import java.util.Collection;
import java.util.Iterator;
import java.util.StringTokenizer;
import java.util.List;

/**
 * List Command for listing some mailboxes. 
 * See RFC 2060 for more details.
 *
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.2 on 04 Aug 2002
 */

class ListCommand extends AuthenticatedSelectedStateCommand
{
    public ListCommand()
    {
        this.commandName = "LIST";

        this.getArgs().add( new AstringArgument( "reference name" ) );
        this.getArgs().add( new AstringArgument( "mailbox" ) );
    }

    protected boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        String command = this.commandName;

        boolean subscribeOnly;
        if ( command.equalsIgnoreCase( "LIST" ) ) {
            subscribeOnly = false;
        }
        else {
            subscribeOnly = true;
        }

        String reference = (String) argValues.get( 0 );
        String folder = (String) argValues.get( 1 );

        Collection list = null;
        try {
        System.out.println("getImapHost: "+session.getImapHost().getClass().getName());
            list = session.getImapHost().listMailboxes( session.getCurrentUser(), reference, folder,
                                                        subscribeOnly );
            if ( list == null ) {
                session.noResponse( command, " unable to interpret mailbox" );
            }
            else if ( list.size() == 0 ) {
                getLogger().debug( "List request matches zero mailboxes: " + request.getCommandRaw() );
                session.okResponse( command );
            }
            else {
                Iterator it = list.iterator();
                while ( it.hasNext() ) {
                    String listResponse = (String) it.next();
                    session.getOut().println( UNTAGGED + SP + command.toUpperCase()
                                               + SP + listResponse );
                    getLogger().debug( UNTAGGED + SP + command.toUpperCase()
                                       + SP + listResponse );
                }
                session.okResponse( command );
            }
        }
        catch ( MailboxException mbe ) {
            if ( mbe.isRemote() ) {
                session.noResponse( command, "[REFERRAL "
                                           + mbe.getRemoteServer() + "]"
                                           + SP + "Wrong server. Try remote." );
            }
            else {
                session.noResponse( command, "No such mailbox" );
            }
            return true;
        }
        catch ( AccessControlException ace ) {
            session.noResponse( command, "No such mailbox" );
            session.logACE( ace );
            return true;
        }

        if ( session.getState() == ImapSessionState.SELECTED ) {
            session.checkSize();
            session.checkExpunge();
        }
        return true;
    }
}
