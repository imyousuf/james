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
