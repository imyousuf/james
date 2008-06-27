package org.apache.james.test;

import junit.framework.TestCase;

import java.io.*;
import java.util.*;
import java.net.Socket;

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

    public AbstractProtocolTest( String s )
    {
        super( s );
    }

    public void setUp() throws Exception
    {
        super.setUp();
        _testElements.clear();

        _socket = new Socket( _host, _port );
        _socket.setSoTimeout( _timeout );
        _out = new PrintWriter( _socket.getOutputStream(), true );
        _in = new BufferedReader( new InputStreamReader( _socket.getInputStream() ) );
    }

    protected void tearDown() throws Exception
    {
        _out.close();
        _in.close();
        _socket.close();
        super.tearDown();
    }

    protected void executeTests() throws Exception
    {
        executeTest( _preElements );
        executeTest( _testElements );
        executeTest( _postElements );
    }

    protected void executeTest( List protocolLines ) throws Exception
    {
        for ( Iterator iter = protocolLines.iterator(); iter.hasNext(); ) {
            ProtocolLine test = (ProtocolLine) iter.next();
            test.testProtocol( _out, _in );
        }
    }

    protected void CL( String clientLine )
    {
        _testElements.add( new ClientRequest( clientLine ) );
    }

    protected void SL( String serverLine )
    {
        _testElements.add( new ServerResponse( serverLine ) );
    }

    protected class ClientRequest implements ProtocolLine
    {
        private String _msg;

        public ClientRequest( String msg )
        {
            _msg = msg;
        }

        public void testProtocol( PrintWriter out, BufferedReader in ) throws Exception
        {
            out.println( _msg );
        }
    }

    protected class ServerResponse implements ProtocolLine
    {
        private String _msg;
        private List _elementTests;
        private boolean _ignoreExtraCharacters = false;
        private String _location;

        public ServerResponse( String msg,
                           String location,
                           boolean ignoreExtraCharacters)
        {
            _msg = msg;
            _elementTests = buildElementTests( getMessageTokens( _msg ) );
            if ( ! ignoreExtraCharacters ) {
                _elementTests.add( new EndOfLineTest() );
            }

            _location = location;
        }

        public ServerResponse( String msg,
                           String location )
        {
            this( msg, location, false );
        }

        public ServerResponse( String msg, boolean ignoreExtraCharacters )
        {
            this( msg, null, ignoreExtraCharacters );
        }

        public ServerResponse( String msg )
        {
            this( msg, null, false );
        }

        public void testProtocol( PrintWriter out, BufferedReader in ) throws Exception
        {
            String testLine = readLine( in );
            if ( _ignoreExtraCharacters
                    && ( testLine.length() > _msg.length() ) ) {
                testLine = testLine.substring( 0, _msg.length() );
            }

            ListIterator testTokens = getMessageTokens( testLine ).listIterator();
            Iterator tests = _elementTests.iterator();
            while ( tests.hasNext() ) {
                ElementTest test = (ElementTest)tests.next();
                if ( _location != null ) {
                    test.setLocation( _location );
                }
                test.test( testTokens, testLine );
            }
        }

        private String readLine( BufferedReader in ) throws Exception
        {
            try {
                return in.readLine();
            }
            catch ( InterruptedIOException e ) {
                String errMsg = "\nLocation: " + _location +
                                "\nExpected: " + _msg +
                                "\nReason: Server Timeout.";
                fail( errMsg );
                return "";
            }
        }

        private List getMessageTokens( String message )
        {
            List tokenList = new ArrayList();
            StringTokenizer tokens = new StringTokenizer( message, " \t\n\r\f\"\'{}()[];$", true );
            while ( tokens.hasMoreTokens() ) {
                tokenList.add( tokens.nextToken() );
            }
            return tokenList;
        }

        private List buildElementTests( List tokenList )
        {
            List elementTests = new ArrayList();
            for ( int i = 0; i < tokenList.size(); i++ ) {
                if ( ( i < ( tokenList.size() - 3 ) )
                        && tokenList.get( i ).equals( "$" )
                        && tokenList.get( i+1 ).equals( "{" )
                        && tokenList.get( i+3 ).equals( "}" ) ) {
                    // For now, assume all special tokens are simple consume tokens.
                    String special = (String) tokenList.get( i+2 );
                    elementTests.add( buildSpecialTest( special ) );
                    i += 3;
                }
                else {
                    elementTests.add( new StringElementTest( (String)tokenList.get( i ) ) );
                }
            }
            return elementTests;
        }

        /**
         * An atomic unit of a ProtocolLine
         */
        private abstract class ElementTest
        {
            protected String _description;

            void setLocation( String location ) {
                _description = "\nLocation: " + location;
            }

            void test( ListIterator testElements, String fullTestLine ) throws Exception
            {
                _description += "\nActual  : " + fullTestLine +
                                "\nExpected: " + _msg +
                                "\nReason: ";
                doTest( testElements );
            }

            void test( ListIterator testElements ) throws Exception
            {
                _description += "Reason: ";
                doTest( testElements );
            }

            abstract void doTest( ListIterator testElements ) throws Exception;
        }
        
        /**
         * An element test which always fails with a null 
         */ 
        
        /**
         * An element test which does a simple String comparison with the element.
         */
        private class StringElementTest extends ElementTest
        {
            private String _elementValue;

            public StringElementTest( String elementValue )
            {
                _elementValue = elementValue;
            }

            public void doTest( ListIterator testElements ) throws Exception
            {
                String next;
                if ( testElements.hasNext() ) {
                    next = (String) testElements.next();
                }
                else {
                    next = "No more elements";
                }
                assertEquals( _description, _elementValue, next );
            }
        }

        private ElementTest buildSpecialTest( String testName )
        {
            if ( testName.startsWith("ignore") ) {
                return new ConsumeElementTest( testName );
            }
            if ( testName.startsWith("rfcDate") ) {
                return new RfcDateElementTest( testName );
            }
            else {
                return new StringElementTest( "${" + testName + "}" );
            }
        }


        /**
         * A simple element test which simply consumes a specified number of test elements,
         * ignoring the actual element values.
         */
        private class ConsumeElementTest extends ElementTest
        {
            private int _elementsToConsume;
            ConsumeElementTest( String token )
            {
                if ( token.equals("ignore") ) {
                    _elementsToConsume = 1;
                }
                else if ( token.startsWith( "ignore-") ) {
                    _elementsToConsume = Integer.parseInt( token.substring( "ignore-".length() ) );
                }
                else {
                    _elementsToConsume = Integer.parseInt( token );
                }
            }

            ConsumeElementTest(int number)
            {
                _elementsToConsume = number;
            }

            public void doTest( ListIterator testElements ) throws Exception
            {
                for ( int i = 0; i < _elementsToConsume; i++ )
                {
                    if ( ! testElements.hasNext() ) {
                        fail( _description + "Not enough elements to ignore." );
                    }
                    String ignored = (String)testElements.next();
                }
            }
        }

        /**
         * Accepts an RFC date (or anything with 12 tokens - todo make this better)
         */
        private class RfcDateElementTest extends ConsumeElementTest
        {
            public RfcDateElementTest( String token )
            {
                super( 11 );
            }
        }

        /**
         * A Test that ensures that no more tokens are present.
         */
        private class EndOfLineTest extends ElementTest
        {
            public void doTest( ListIterator testElements ) throws Exception
            {
                if ( testElements.hasNext() ) {
                    String nextElement = (String)testElements.next();
                    fail( _description + "End of line expected, found '" + nextElement + "'" );
                }
            }
        }
    }

    protected interface ProtocolLine
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
            throw new Exception("Test Resource '" + fileName + "' not found." );
        }

        addProtocolLinesFromStream( is, protocolLines, fileName );
    }

    private void addProtocolLinesFromStream( InputStream is, List protocolLines, String fileName ) throws Exception
    {
        BufferedReader reader = new BufferedReader( new InputStreamReader( is ) );
        String next;
        int lineNumber = 1;
        while ( ( next = reader.readLine() ) != null ) {
            String location = fileName + ":" + lineNumber;
            if ( next.startsWith( "C: " ) ) {
                String clientMsg = next.substring( 3 );
                protocolLines.add( new ClientRequest( clientMsg ) );
            }
            else if ( next.startsWith( "S: " ) ) {
                String serverMsg = next.substring( 3 );
                if ( serverMsg.endsWith("//") ) {
                    serverMsg = serverMsg.substring( 0, serverMsg.length() - 2 );
                    protocolLines.add( new ServerResponse( serverMsg, location, true ) );
                }
                else {
                    protocolLines.add( new ServerResponse( serverMsg, location, false ) );
                }

            }
            else if ( next.startsWith( "//" )
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
