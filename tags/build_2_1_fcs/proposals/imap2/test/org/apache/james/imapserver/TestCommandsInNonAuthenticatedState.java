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
