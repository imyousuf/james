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
public class TestAuthenticated
        extends SimpleFileProtocolTest implements ImapTest
{
    public TestAuthenticated( String name )
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
        testElements.CL( "a001 LOGIN " + username + " " + password );
        testElements.SL( "a001 OK LOGIN completed", "TestAuthenticated.java:33" );
    }

    /**
     * Provides all tests which should be run in the authenicated state. Each test name
     * corresponds to a protocol session file.
     */
    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new TestAuthenticated( "Capability" ) );
        suite.addTest( new TestAuthenticated( "AuthenticateAuthenticated" ) );
        suite.addTest( new TestAuthenticated( "LoginAuthenticated" ) );
        suite.addTest( new TestAuthenticated( "Logout" ) );
        suite.addTest( new TestAuthenticated( "ExamineInbox" ) );
        suite.addTest( new TestAuthenticated( "SelectInbox" ) );
        suite.addTest( new TestAuthenticated( "Create" ) );
        suite.addTest( new TestAuthenticated( "ExamineEmpty" ) );
        suite.addTest( new TestAuthenticated( "SelectEmpty" ) );
        suite.addTest( new TestAuthenticated( "ListNamespace" ) );
        suite.addTest( new TestAuthenticated( "ListMailboxes" ) );
        suite.addTest( new TestAuthenticated( "Status" ) );
        suite.addTest( new TestAuthenticated( "StringArgs" ) );
        suite.addTest( new TestAuthenticated( "Subscribe" ) );
        suite.addTest( new TestAuthenticated( "Subscribe2" ) );

        suite.addTest( new TestAuthenticated( "Append" ) );

        // Run delete last, because many of the tests depend on created mailboxes.
        suite.addTest( new TestAuthenticated( "Delete" ) );

        return suite;
    }

}
