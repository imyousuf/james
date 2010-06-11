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
 * Runs tests for commands valid in the AUTHENTICATED state. A login session precedes
 * the execution of the test elements.
 */
public class TestCommandsInAuthenticatedState
        extends SimpleFileProtocolTest implements ImapTest
{
    public TestCommandsInAuthenticatedState( String name )
    {
        super( name );
    }

    /**
     * Sets up {@link #preElements} with a welcome message and login request/response.
     * @throws Exception
     */
    public void setUp() throws Exception
    {
        super.setUp();
        addTestFile( "Welcome.test", preElements );
        addLogin( USER, PASSWORD );
    }

    protected void addLogin( String username, String password )
    {
        preElements.CL( "a001 LOGIN " + username + " " + password );
        preElements.SL( "a001 OK LOGIN completed", "TestCommandsInAuthenticatedState.java:33" );
    }

    /**
     * Provides all tests which should be run in the authenicated state. Each test name
     * corresponds to a protocol session file.
     */
    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        // Not valid in this state
//        suite.addTest( new TestCommandsInAuthenticatedState( "ValidSelected" ) );
//        suite.addTest( new TestCommandsInAuthenticatedState( "ValidNonAuthenticated" ) );

        // Valid in all states
//        suite.addTest( new TestCommandsInAuthenticatedState( "Capability" ) );
//        suite.addTest( new TestCommandsInAuthenticatedState( "Noop" ) );
//        suite.addTest( new TestCommandsInAuthenticatedState( "Logout" ) );

        // Valid in authenticated state
//        suite.addTest( new TestCommandsInAuthenticatedState( "AppendExamineInbox" ) );
//        suite.addTest( new TestCommandsInAuthenticatedState( "AppendSelectInbox" ) );
//        suite.addTest( new TestCommandsInAuthenticatedState( "Create" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "ExamineEmpty" ) );
//        suite.addTest( new TestCommandsInAuthenticatedState( "SelectEmpty" ) );
//        suite.addTest( new TestCommandsInAuthenticatedState( "ListNamespace" ) );
//        suite.addTest( new TestCommandsInAuthenticatedState( "ListMailboxes" ) );
//        suite.addTest( new TestCommandsInAuthenticatedState( "Status" ) );
//        suite.addTest( new TestCommandsInAuthenticatedState( "Subscribe" ) );
//        suite.addTest( new TestCommandsInAuthenticatedState( "Delete" ) );
//        suite.addTest( new TestCommandsInAuthenticatedState( "Append" ) );

        return suite;
    }
}
