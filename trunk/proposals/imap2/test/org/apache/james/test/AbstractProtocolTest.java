package org.apache.james.test;

import org.apache.oro.text.perl.Perl5Util;

import junit.framework.TestCase;

import java.io.*;
import java.util.*;
import java.net.Socket;

/**
 * Abstract Protocol Test is the root of all of the James Imap Server test
 * cases.  It provides functionality to create text files for matching
 * client requests and server responses.  In order to use it however you
 * must create a sub class and set all the file names, etc up yourself.
 * All Comments are written by Andy Oliver who is still trying to figure out
 * some of it himself so don't take this as gospel
 *
 * @author Unattributed Original Authors
 * @author Andrew C. Oliver
 */
public abstract class AbstractProtocolTest extends TestCase
{
    private Socket _socket;
    private PrintWriter _out;
    private BufferedReader _in;
    protected String _host = "127.0.0.1";

    protected int _port;
    protected int _timeout = 0;

    protected List _preElements = new ArrayList();
    protected List _testElements = new ArrayList();
    protected List _postElements = new ArrayList();

    private static final Perl5Util perl = new Perl5Util();
    private static final String CLIENT_TAG = "C: ";
    private static final String SERVER_TAG = "S: ";
    private static final String OPEN_UNORDERED_BLOCK_TAG = "SUB {";
    private static final String CLOSE_UNORDERED_BLOCK_TAG = "}";
    private static final String COMMENT_TAG = "#";

    public AbstractProtocolTest( String s )
    {
        super( s );
    }

    // comment in TestCase
    public void setUp() throws Exception
    {
        super.setUp();
        _testElements.clear();

        _socket = new Socket( _host, _port );
        _socket.setSoTimeout( _timeout );
        _out = new PrintWriter( _socket.getOutputStream(), true );
        _in = new BufferedReader( new InputStreamReader( _socket.getInputStream() ) );
    }

    // comment in TestCase
    protected void tearDown() throws Exception
    {
        _out.close();
        _in.close();
        _socket.close();
        super.tearDown();
    }

    // comment in TestCase
    protected void executeTests() throws Exception
    {
        executeTest( _preElements );
        executeTest( _testElements );
        executeTest( _postElements );
    }

    /**
     * executes the test case as specified in the file.  Commands in
     * CL: elements are sent to the server, and the SL: lines are verified
     * against those returning from the server.  The order is important
     * unless in a "SUB:" block in which case the order is not important and
     * the test will pass if any line in the SUB: block matches.
     */
    protected void executeTest( List protocolLines ) throws Exception
    {
        for ( Iterator iter = protocolLines.iterator(); iter.hasNext(); ) {
            Object obj = iter.next();
            if ( obj instanceof ProtocolElement ) {
                ProtocolElement test = ( ProtocolElement ) obj;
                test.testProtocol( _out, _in );
            }
        }
    }

    /**
     * adds a new Client request line to the test elements
     */
    protected void CL( String clientLine )
    {
        _testElements.add( new ClientRequest( clientLine ) );
    }

    /**
     * adds a new Server Response line to the test elements
     */
    protected void SL( String serverLine )
    {
        _testElements.add( new ServerResponse( serverLine ) );
    }

    /**
     * This Line is sent to the server (everything after "CL: ") in expectation
     * that the server will respond.
     */
    protected class ClientRequest implements ProtocolElement
    {
        private String _msg;

        public ClientRequest( String msg )
        {
            _msg = msg;
        }

        /**
         * Sends the request to the server
         */
        public void testProtocol( PrintWriter out, BufferedReader in ) throws Exception
        {
            out.println( _msg );
        }

        /**
         * This should NOT be called, CL is not blockable!  Runtime exception
         * will be thrown.  Implemented because of "ProtocolElement"
         */
        public void testProtocolBlock( PrintWriter out, BufferedReader in, List list )
                throws Exception
        {
            //out.println( _msg );
            throw new RuntimeException( "Syntax error in test case, CL is not " +
                                        "able to be used in a SUB: block" );
        }
    }

    protected class ServerResponse implements ProtocolElement
    {
        private String expectedLine;
        protected String location;

        public ServerResponse( String expectedPattern, String location )
        {
            this.expectedLine = expectedPattern;
            this.location = location;
        }

        public ServerResponse( String expectedPattern )
        {
            this( expectedPattern, "" );
        }

        public void testProtocol( PrintWriter out, BufferedReader in ) throws Exception
        {
            String testLine = readLine( in );
            if ( ! match( expectedLine, testLine ) ) {
                String errMsg = "\nLocation: " + location +
                        "\nExcpected: " + expectedLine +
                        "\nActual   : " + testLine;
                fail( errMsg );
            }
        }

        protected boolean match( String expected, String actual )
        {
            String pattern = "m/" + expected + "/";
            return perl.match( pattern, actual );
        }

        /**
         * Grabs a line from the server and throws an error message if it
         * doesn't work out
         * @param in BufferedReader for getting the server response
         * @return String of the line from the server
         */
        protected String readLine( BufferedReader in ) throws Exception
        {
            try {
                return in.readLine();
            }
            catch ( InterruptedIOException e ) {
                String errMsg = "\nLocation: " + location +
                        "\nExpected: " + expectedLine +
                        "\nReason: Server Timeout.";
                fail( errMsg );
                return "";
            }
        }
    }

    private class UnorderedBlockResponse extends ServerResponse
    {
        private List expectedLines = new ArrayList();

        public UnorderedBlockResponse( List expectedLines, String location )
        {
            super( "<Unordered Block>", location );
            this.expectedLines = expectedLines;
        }

        public void testProtocol( PrintWriter out, BufferedReader in ) throws Exception
        {
            List testLines = new ArrayList( expectedLines );
            while ( testLines.size() > 0 )
            {
                String actualLine = readLine( in );
                boolean foundMatch = false;

                for ( int i = 0; i < testLines.size(); i++ )
                {
                    String expected = (String)testLines.get( i );
                    if ( match( expected, actualLine ))
                    {
                        foundMatch = true;
                        testLines.remove( expected );
                        break;
                    }
                }

                if (! foundMatch )
                {
                    StringBuffer errMsg = new StringBuffer()
                        .append( "\nLocation: " )
                        .append( location )
                        .append( "\nExpected one of: " );
                    Iterator iter = expectedLines.iterator();
                    while ( iter.hasNext() ) {
                        errMsg.append( "\n    " );
                        errMsg.append( iter.next() );
                    }
                    errMsg.append("\nActual: " )
                          .append( actualLine );

                    fail( errMsg.toString() );
                }
            }
        }
    }


    protected interface ProtocolElement
    {
        void testProtocol( PrintWriter out, BufferedReader in ) throws Exception;
    }

    protected void addTestFile( String fileName ) throws Exception
    {
        addTestFile( fileName, _testElements );
    }

    protected void addTestFile( String fileName, List protocolLines ) throws Exception
    {
        // Need to find local resource.
        InputStream is = this.getClass().getResourceAsStream( fileName );
        if ( is == null ) {
            throw new Exception( "Test Resource '" + fileName + "' not found." );
        }

        addProtocolLinesFromStream( is, protocolLines, fileName );
    }

    private void addProtocolLinesFromStream( InputStream is, List protocolElements, String fileName )
            throws Exception
    {
        BufferedReader reader = new BufferedReader( new InputStreamReader( is ) );
        String next;
        int lineNumber = 1;
        while ( ( next = reader.readLine() ) != null ) {
            String location = fileName + ":" + lineNumber;
            if ( next.startsWith( CLIENT_TAG ) ) {
                String clientMsg = next.substring( 3 );
                protocolElements.add( new ClientRequest( clientMsg ) );
            }
            else if ( next.startsWith( SERVER_TAG ) ) {
                String serverMsg = next.substring( 3 );
                protocolElements.add( new ServerResponse( serverMsg, location ) );
            }
            else if ( next.startsWith( OPEN_UNORDERED_BLOCK_TAG ) ) {
                List unorderedLines = new ArrayList( 5 );
                next = reader.readLine();

                while ( !next.startsWith( CLOSE_UNORDERED_BLOCK_TAG ) ) {
                    if (! next.startsWith( SERVER_TAG ) ) {
                        throw new Exception( "Only 'S: ' lines are permitted inside a 'SUB {' block.");
                    }
                    String serverMsg = next.substring( 3 );
                    unorderedLines.add( serverMsg );
                    next = reader.readLine();
                    lineNumber++;
                }

                UnorderedBlockResponse blockResponse =
                        new UnorderedBlockResponse( unorderedLines, location );
                protocolElements.add( blockResponse );
            }
            else if ( next.startsWith( COMMENT_TAG )
                    || next.trim().length() == 0 ) {
                // ignore these lines.
            }
            else {
                String prefix = next;
                if ( next.length() > 3 ) {
                    prefix = next.substring( 0, 3 );
                }
                throw new Exception( "Invalid line prefix: " + prefix );
            }
            lineNumber++;
        }
    }
}
