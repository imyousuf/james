/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapSessionState;
import org.apache.james.imapserver.ImapRequest;
import org.apache.james.imapserver.ImapSession;

import java.util.StringTokenizer;

/**
 * A base class for ImapCommands only valid in the NON_AUTHENTICATED state.
 */
abstract class NonAuthenticatedStateCommand extends CommandTemplate
{

    /**
     * By default, valid in any state (unless overridden by subclass.
     */
    public boolean validForState( ImapSessionState state )
    {
        return ( state == ImapSessionState.NON_AUTHENTICATED );
    }
}
