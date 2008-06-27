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
import junit.framework.TestCase;
import org.apache.james.remotemanager.UserManagementTest;

public final class InitialUsers
        extends TestCase implements IMAPTest
{
    public InitialUsers( String s )
    {
        super( s );
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new UserManagementTest( "addUser", USER, PASSWORD ) );
        return suite;
    }
}
