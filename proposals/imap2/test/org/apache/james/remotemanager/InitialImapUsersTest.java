/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.remotemanager;

import junit.framework.Test;
import junit.framework.TestSuite;

public final class InitialImapUsersTest
        extends TestRemoteManager
{
    public InitialImapUsersTest( String s ) throws Exception
    {
        super( s );
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new InitialImapUsersTest( "InitialUsers" ) );
        return suite;
    }
}
