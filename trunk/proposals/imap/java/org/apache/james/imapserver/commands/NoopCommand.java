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

class NoopCommand extends CommandTemplate
{
    public boolean process( ImapRequest request, ImapSession session )
    {
        if ( session.getState() == SELECTED ) {
            session.checkSize();
            session.checkExpunge();
        }
        // we could send optional untagged status responses as well
        session.okResponse( request.getCommand() );
        logCommand( request, session );
        return true;
    }
}
