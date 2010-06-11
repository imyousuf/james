package org.apache.james.smtpserver;

import org.apache.james.test.SimpleFileProtocolTest;

import junit.framework.Test;
import junit.framework.TestSuite;


public class TestSMTP
        extends SimpleFileProtocolTest
{
    public TestSMTP( String name )
    {
        super( name );
    }

    public static Test suite() throws Exception
    {
        TestSuite suite = new TestSuite();
        suite.addTest( new TestSMTP( "Send" ) );
        return suite;
    }
}
