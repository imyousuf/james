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

public class TestSelected
        extends TestAuthenticated
{
    public TestSelected( String name )
    {
        super( name );
    }

    public void setUp() throws Exception
    {
        super.setUp();
        addTestFile( "SelectInbox.test" );
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new TestSelected( "FetchSingleMessage" ) );
        suite.addTest( new TestSelected( "FetchMultipleMessages" ) );
        return suite;
    }
}
