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


import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionState;

import java.util.List;

class NamespaceCommand extends AuthenticatedSelectedStateCommand
{
    public NamespaceCommand()
    {
        this.commandName = "NAMESPACE";
    }

    public boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        String namespaces = session.getImapSystem().getNamespaces( session.getCurrentUser() );
        session.untaggedResponse( "NAMESPACE " + namespaces );
        getLogger().info( "Provided NAMESPACE: " + namespaces );
        if ( session.getState() == ImapSessionState.SELECTED ) {
            session.checkSize();
            session.checkExpunge();
        }
        session.okResponse( request.getCommand() );
        return true;
    }
}
