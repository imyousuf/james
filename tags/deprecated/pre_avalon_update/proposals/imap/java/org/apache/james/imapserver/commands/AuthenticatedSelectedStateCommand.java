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
import org.apache.james.util.Assert;

import java.util.List;


/**
 * A base class for ImapCommands only valid in AUTHENTICATED and SELECTED states.
 */
abstract class AuthenticatedSelectedStateCommand extends CommandTemplate
{
    /**
     * Check that the state is AUTHENTICATED or SELECTED
     */
    public boolean validForState( ImapSessionState state )
    {
        return ( state == ImapSessionState.AUTHENTICATED
                || state == ImapSessionState.SELECTED );
    }
}
