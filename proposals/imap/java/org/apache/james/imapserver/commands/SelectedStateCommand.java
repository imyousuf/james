/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

/**
 * A base class for ImapCommands only valid in the SELECTED state.
 */
abstract class SelectedStateCommand extends CommandTemplate
{
    /**
     * By default, valid in any state (unless overridden by subclass.
     */
    public boolean validForState( int state )
    {
        return ( state == SELECTED );
    }
}
