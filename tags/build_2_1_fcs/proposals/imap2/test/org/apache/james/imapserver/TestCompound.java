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
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
public class TestCompound extends TestCommandsInAuthenticatedState
{
    public TestCompound( String name )
    {
        super( name );
    }

    /**
     * Provides all tests which should be run in the authenicated state. Each test name
     * corresponds to a protocol session file.
     */
    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new TestCommandsInAuthenticatedState( "AppendExpunge" ) );
        suite.addTest( new TestCommandsInAuthenticatedState( "StringArgs" ) );
        // TODO various mailbox names (eg with spaces...)
        return suite;
    }

}
