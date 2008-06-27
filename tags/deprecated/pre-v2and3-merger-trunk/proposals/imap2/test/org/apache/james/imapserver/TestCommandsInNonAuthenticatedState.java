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

import junit.framework.Test;
import junit.framework.TestSuite;
import org.apache.james.test.SimpleFileProtocolTest;

/**
 * Runs tests for commands valid in the NON_AUTHENTICATED state.
 * A welcome message precedes the execution of the test elements.
 */
public class TestCommandsInNonAuthenticatedState
        extends SimpleFileProtocolTest
{
    public TestCommandsInNonAuthenticatedState( String name )
    {
        super( name );
    }

    /**
     * Adds a welcome message to the {@link #preElements}.
     * @throws Exception
     */
    public void setUp() throws Exception
    {
        super.setUp();
        addTestFile( "Welcome.test", preElements );
    }

    /**
     * Sets up tests valid in the non-authenticated state.
     */ 
    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        // Not valid in this state
        suite.addTest( new TestCommandsInNonAuthenticatedState( "ValidAuthenticated" ) );
        suite.addTest( new TestCommandsInNonAuthenticatedState( "ValidSelected" ) );

        // Valid in all states
        suite.addTest( new TestCommandsInNonAuthenticatedState( "Capability" ) );
        suite.addTest( new TestCommandsInNonAuthenticatedState( "Noop" ) );
        suite.addTest( new TestCommandsInNonAuthenticatedState( "Logout" ) );

        // Valid only in non-authenticated state.
        suite.addTest( new TestCommandsInNonAuthenticatedState( "Authenticate" ) );
        suite.addTest( new TestCommandsInNonAuthenticatedState( "Login" ) );

        return suite;
    }

}
