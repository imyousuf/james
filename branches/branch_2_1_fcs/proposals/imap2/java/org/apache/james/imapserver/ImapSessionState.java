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
 */
public class ImapSessionState
{
    public static final ImapSessionState NON_AUTHENTICATED = new ImapSessionState( "NON_AUTHENTICATED" );
    public static final ImapSessionState AUTHENTICATED = new ImapSessionState( "AUTHENTICATED" );
    public static final ImapSessionState SELECTED = new ImapSessionState( "SELECTED" );
    public static final ImapSessionState LOGOUT = new ImapSessionState( "LOGOUT" );

    private final String myName; // for debug only

    private ImapSessionState( String name )
    {
        myName = name;
    }

    public String toString()
    {
        return myName;
    }
}
