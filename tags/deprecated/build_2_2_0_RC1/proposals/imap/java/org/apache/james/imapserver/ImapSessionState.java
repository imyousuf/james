/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.imapserver;

/**
 * Enumerated type representing an IMAP session state.
 * TODO: use this instead of int constants.
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
