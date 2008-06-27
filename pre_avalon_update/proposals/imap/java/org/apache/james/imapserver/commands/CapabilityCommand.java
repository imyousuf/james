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
import org.apache.james.imapserver.ImapSessionState;

import java.util.List;

class CapabilityCommand extends CommandTemplate
{
    String CAPABILITY_RESPONSE = "CAPABILITY " + VERSION; //add as implemented

    public CapabilityCommand()
    {
        this.commandName = "CAPABILITY";
    }

    protected boolean doProcess( ImapRequest request, ImapSession session, List args )
    {
        session.untaggedResponse( CAPABILITY_RESPONSE );
        if ( session.getState() == ImapSessionState.SELECTED ) {
            session.checkSize();
            session.checkExpunge();
        }
        session.okResponse( request.getCommand() );
        logCommand( request, session );
        return true;
    }
}
