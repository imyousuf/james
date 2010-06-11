/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;

import java.util.List;

class AuthenticateCommand extends NonAuthenticatedStateCommand
{
    AuthenticateCommand()
    {
        this.commandName = "AUTHENTICATE";

        this.getArgs().add( new AstringArgument( "auth-type" ) );
    }

    public boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        session.noResponse( request.getCommand(), "Unsupported authentication mechanism" );
        return true;
    }
}
