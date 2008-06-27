package org.apache.james.test;

public class SimpleFileProtocolTest
        extends AbstractProtocolTest
{
    public SimpleFileProtocolTest( String fileName )
    {
        super( fileName );
    }

    public SimpleFileProtocolTest( String fileName, String host, int port )
    {
        super( fileName );
        _host = host;
        _port = port;
    }

    protected void runTest() throws Throwable
    {
        String testFileName = getName() + ".test";
        addTestFile( testFileName );
        executeTests();
    }
}
