package org.apache.james.test;

import java.io.InputStream;

/**
 * A Protocol test which reads the test protocol session from a file. The
 * file read is taken as "<test-name>.test", where <test-name> is the value
 * passed into the constructor.
 */
public class SimpleFileProtocolTest
        extends AbstractProtocolTest
{
    private FileProtocolSessionBuilder builder =
            new FileProtocolSessionBuilder();

    public SimpleFileProtocolTest( String fileName )
    {
        super( fileName );
    }

    protected void runTest() throws Throwable
    {
        String fileName = getName() + ".test";
        addTestFile( fileName, testElements );

        runSessions();
    }

    protected void addTestFile( String fileName, ProtocolSession session) throws Exception
    {
        // Need to find local resource.
        InputStream is = this.getClass().getResourceAsStream( fileName );
        if ( is == null ) {
            throw new Exception( "Test Resource '" + fileName + "' not found." );
        }

        builder.addProtocolLinesFromStream( is, session, fileName );
    }
}
