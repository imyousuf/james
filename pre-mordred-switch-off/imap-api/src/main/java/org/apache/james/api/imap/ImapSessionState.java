/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.api.imap;

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
