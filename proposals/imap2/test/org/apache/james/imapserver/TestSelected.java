/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Runs tests for commands valid in the AUTHENTICATED state. A login session
 * and SelectInbox session precedes the execution of the test elements.
 */
public class TestSelected
        extends TestAuthenticated
{
    public TestSelected( String name )
    {
        super( name );
    }

    /**
     * Superclass sets up welcome message and login session in {@link #preElements}.
     * A "SELECT INBOX" session is then added to these elements.
     * @throws Exception
     */
    public void setUp() throws Exception
    {
        super.setUp();
        addTestFile( "SelectInbox.test", preElements );
    }

    /**
     * Provides all tests which should be run in the selected state. Each test name
     * corresponds to a protocol session file.
     */
    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new TestSelected( "FetchSingleMessage" ) );
        suite.addTest( new TestSelected( "FetchMultipleMessages" ) );
        return suite;
    }
}
