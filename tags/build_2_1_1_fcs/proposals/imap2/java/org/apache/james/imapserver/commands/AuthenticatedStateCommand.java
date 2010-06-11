/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.ImapSessionState;

/**
 * A base class for ImapCommands only valid in AUTHENTICATED and SELECTED states.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
abstract class AuthenticatedStateCommand extends CommandTemplate
{
    /**
     * Check that the state is {@link ImapSessionState#AUTHENTICATED } or
     * {@link ImapSessionState#SELECTED}
     */
    public boolean validForState( ImapSessionState state )
    {
        return ( state == ImapSessionState.AUTHENTICATED
                || state == ImapSessionState.SELECTED );
    }
}
