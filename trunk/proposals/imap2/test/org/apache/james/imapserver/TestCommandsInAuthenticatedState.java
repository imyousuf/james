/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import org.apache.james.test.SimpleFileProtocolTest;

import junit.framework.Test;
import junit.framework.TestSuite;

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
        suite.addTest( new TestCommandsInAuthenticatedState( "ValidSelected" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "ValidNonAuthenticated" ) );

        // Valid in all states
        suite.addTest( new TestCommandsInAuthenticatedState( "Capability" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "Noop" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "Logout" ) );

        // Valid in authenticated state
        suite.addTest( new TestCommandsInAuthenticatedState( "ExamineInbox" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "SelectInbox" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "Create" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "ExamineEmpty" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "SelectEmpty" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "ListNamespace" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "ListMailboxes" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "Status" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "Subscribe" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "Delete" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "Append" ) );

        return suite;
    }

}
