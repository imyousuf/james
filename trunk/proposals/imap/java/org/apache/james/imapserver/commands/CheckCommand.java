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

import java.util.StringTokenizer;

class CheckCommand extends SelectedStateCommand
{
    public boolean process( ImapRequest request, ImapSession session )
    {
        int arguments = request.arguments();
        StringTokenizer commandLine = request.getCommandLine();
        String command = request.getCommand();

        if ( request.getCurrentMailbox().checkpoint() ) {
            session.okResponse( command );
            session.checkSize();
            session.checkExpunge();
        }
        else {
            session.noResponse( command );
        }
        return true;
    }
}
