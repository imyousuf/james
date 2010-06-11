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
 * Implements the command for searching after Mails.
 *
 * @author <a href="mailto:sascha@kulawik.de">Sascha Kulawik</a>
 * @version 0.2 on 04 Aug 2002
 */

class SearchCommand extends AuthenticatedSelectedStateCommand
{
    public SearchCommand()
    {
        System.out.println("*SEARCH*: <init>");
        this.commandName = "SEARCH";

        this.getArgs().add( new AstringArgument( "search1" ) );
        this.getArgs().add( new AstringArgument( "search2" ) );
    }

    protected boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        String command = this.commandName;

        String search1 = (String) argValues.get( 0 );
        String search2 = (String) argValues.get( 1 );

        System.out.println("*SEARCH*: got arg1: "+ search1);
        System.out.println("*SEARCH*: got arg2: "+ search2);
        
        System.out.println("*SEARCH*: currentMailbox:"+request.getCurrentMailbox().getName());
        
        session.getOut().print( UNTAGGED + SP + command.toUpperCase());
        getLogger().debug( UNTAGGED + SP + command.toUpperCase());
        if (request.getCurrentMailbox().matchesName("inbox")) {
            session.getOut().print( SP + "1" );
            getLogger().debug( SP + "1"  );
        }
        session.getOut().println();

        session.okResponse( command );

        if ( session.getState() == ImapSessionState.SELECTED ) {
            session.checkSize();
            session.checkExpunge();
        }
        return true;
    }
}
