package org.apache.james.remotemanager;

import junit.framework.TestCase;
import junit.framework.Test;
import junit.framework.TestSuite;

import java.net.Socket;
import java.io.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;
import java.util.Date;

import org.apache.james.test.SimpleFileProtocolTest;

public class TestRemoteManager
        extends SimpleFileProtocolTest
{

    public TestRemoteManager( String testFileName ) throws Exception
    {
        super( testFileName );
        _port = 4555;
        addTestFile( "RemoteManagerLogin.test", _preElements );
        addTestFile( "RemoteManagerLogout.test", _postElements );
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new TestRemoteManager( "AddUsers" ) );
        suite.addTest( new TestRemoteManager( "DeleteUsers" ) );
        return suite;
    }
}
