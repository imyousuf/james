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

import org.apache.james.imapserver.*;
import org.apache.james.util.Assert;

import java.util.StringTokenizer;
import java.util.List;

/**
 * Implements the UID Command for calling Commands with the fixed UID
 *
 * @version 0.2 on 04 Aug 2002
 */
class UidCommand implements ImapCommand
{
    public boolean validForState( ImapSessionState state )
    {
        return ( state == ImapSessionState.SELECTED );
    }

    public boolean process( ImapRequest request, ImapSession session )
    {
       // StringTokenizer commandLine = new java.util.StringTokenizer(request.getCommandRaw());
        StringTokenizer commandLine = request.getCommandLine();
        int arguments = commandLine.countTokens();
       // StringTokenizer commandLine = request.getCommandLine();
        String command = request.getCommand();

        StringTokenizer txt = new java.util.StringTokenizer(request.getCommandRaw());
        System.out.println("UidCommand.process: #args="+txt.countTokens());
        while (txt.hasMoreTokens()) {
            System.out.println("UidCommand.process: arg='"+txt.nextToken()+"'");
        }
        if ( arguments < 3 ) {
            session.badResponse( "rawcommand='"+request.getCommandRaw()+"' #args="+request.arguments()+" Command should be <tag> <UID> <command> <command parameters>" );
            return true;
        }
        String uidCommand = commandLine.nextToken();
        System.out.println("UidCommand.uidCommand="+uidCommand);
        System.out.println("UidCommand.session="+session.getClass().getName());
        ImapCommand cmd = session.getImapCommand( uidCommand );
        System.out.println("UidCommand.cmd="+cmd);
        System.out.println("UidCommand.cmd="+cmd.getClass().getName());
        if ( cmd instanceof CommandFetch || cmd instanceof CommandStore  || cmd instanceof CopyCommand) {
            // As in RFC2060 also the COPY Command is valid for UID Command
            request.setCommand( uidCommand );
            ((ImapRequestImpl)request).setUseUIDs( true );
            cmd.process( request, session );
        } else {
            session.badResponse( "Invalid UID secondary command." );
        }
        return true;
    }
}
