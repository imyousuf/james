/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

/**
 * Enumerated type representing an IMAP session state.
 * @todo use this instead of int constants.
 */
public class ImapSessionState
{
    private int _state;
    private String _name;

    /**
     * Private constructor only.
     */
    private ImapSessionState( int state, String name )
    {
        _state = state;
        _name = name;
    }
    
    public int getState()
    {
        return _state;
    }
    
    public String getName()
    {
        return _name;
    }
    
    public static final ImapSessionState NON_AUTHENTICATED = new ImapSessionState( 0, "NONAUTHENTICATED" );
    public static final ImapSessionState AUTHENTICATED = new ImapSessionState( 1, "AUTHENTICATED" );
    public static final ImapSessionState SELECTED = new ImapSessionState( 2, "SELECTED" );
    public static final ImapSessionState LOGOUT = new ImapSessionState( 3, "LOGOUT" );
    
}
