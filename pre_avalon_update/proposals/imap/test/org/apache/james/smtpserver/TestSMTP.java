package org.apache.james.smtpserver;

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


public class TestSMTP
        extends SimpleFileProtocolTest
{
    public TestSMTP( String name )
    {
        super( name );
        _port = 25;
        _timeout = 0;
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new TestSMTP( "Send" ) );
        return suite;
    }
}
