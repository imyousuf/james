package org.apache.james.remotemanager;

import org.apache.james.test.SimpleFileProtocolTest;

import junit.framework.Test;
import junit.framework.TestSuite;

public class TestRemoteManager
        extends SimpleFileProtocolTest
{

    public TestRemoteManager( String testFileName ) throws Exception
    {
        super( testFileName );
        port = 4555;
        addTestFile( "RemoteManagerLogin.test", preElements );
        addTestFile( "RemoteManagerLogout.test", postElements );
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new TestRemoteManager( "AddUsers" ) );
        suite.addTest( new TestRemoteManager( "DeleteUsers" ) );
        return suite;
    }
}
