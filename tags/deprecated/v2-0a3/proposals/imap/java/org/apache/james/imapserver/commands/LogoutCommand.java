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

class LogoutCommand extends CommandTemplate
{
    public LogoutCommand()
    {
        this.commandName = "LOGOUT";
    }

    public boolean doProcess( ImapRequest request, ImapSession session, List argValues )
    {
        session.setConnectionClosed( session.closeConnection( NORMAL_CLOSE, "", "" ) );
        return false;
    }
}
