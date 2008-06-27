/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.test;

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
            if ( obj instanceof ProtocolLine ) {
            ProtocolLine test = (ProtocolLine) obj;
            test.testProtocol( _out, _in );
            } else if ( obj instanceof List ) {
               //System.err.println("skipping over unordered block");
               List testlist = (List) obj;
               for (int k = 0; k < testlist.size(); k++) {
                  ProtocolLine test = (ProtocolLine) testlist.get(k);
                  test.testProtocolBlock( _out, _in, testlist);
               } 
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
    protected class ClientRequest implements ProtocolLine
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
         * will be thrown.  Implemented because of "ProtocolLine"
         */ 
        public void testProtocolBlock( PrintWriter out, BufferedReader in, List list)
        throws Exception {
            //out.println( _msg ); 
            throw new RuntimeException("Syntax error in test case, CL is not "+
                                       "able to be used in a SUB: block");
        }
    }

    /**
     * This line is what is in the test case file, it is verified against the
     * actual line returned from the server.
     */
    protected class ServerResponse implements ProtocolLine
    {
        private String _msg;
        private List _elementTests;
        private boolean _ignoreExtraCharacters = false;
        private String _location;

        /**
         * Constructs a ServerResponse, builds the tests
         * @param msg the server response line 
         * @param location a string containing the location number for error 
         *        messages to give you a clue of where in the file you where
         * @param ignoreExtraCharacters whether to ignore EndOfLine or not
         */
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

        /**
         * Cheap version of ServerResponse(String, String, boolean) this 
         * assumes you don't want to ignore the end of line
         * @param msg the server response line 
         * @param location a string containing the location number for error 
         *        messages to give you a clue of where in the file you where
         */
        public ServerResponse( String msg,
                           String location )
        {
            this( msg, location, false );
        }

        /**
         * Cheap version of ServerResponse(String, String, boolean) this 
         * sends "null" for location
         * @param msg the server response line 
         * @param ignoreExtraCharacters whether to ignore EndOfLine or not
         */
        public ServerResponse( String msg, boolean ignoreExtraCharacters )
        {
            this( msg, null, ignoreExtraCharacters );
        }

        /**
         * Cheap version of ServerResponse(String, String, boolean) this 
         * sends "null" for location and false for ignore the EOL
         * @param msg the server response line 
         */
        public ServerResponse( String msg )
        {
            this( msg, null, false );
        }

        /**
         * Special method for dealing with anything in the "SUB" block,
         * it ignores the order of whats in the SUB block and only throws
         * an assertion at the end if nothing matched
         * @param out PrintWriter for talking to the server
         * @param in BufferedReader for getting the server response
         * @param testslist List containing the lines of the block, should
         * contain ServerResponse objects
         */
        public void testProtocolBlock( PrintWriter out, BufferedReader in,
                                      List testslist) throws Exception {
            //System.err.println("in new TestProtocol");
            String testLine = readLine( in );
            if ( _ignoreExtraCharacters
                    && ( testLine.length() > _msg.length() ) ) {
                testLine = testLine.substring( 0, _msg.length() );
            }

            ListIterator testTokens = getMessageTokens( testLine ).listIterator();
            Iterator theblock = testslist.iterator();
            boolean assertval = false;
            while (theblock.hasNext()) {
              assertval = testProtocolInBlock( out, in, testTokens, testLine);
              if (assertval = true) {
                  break;
              }
            }
            if (assertval == false)   {
                    System.err.println("returning failure in block");
            }
            assertTrue("Someting in block matched (false)", assertval);
            
        }
 
        /** 
         * Called by testProtocolBlock.  Tests one line and returns true or
         * false.  
         * @param out PrintWriter for talking to the server
         * @param in BufferedReader for getting the server response
         * @param testTokens ListIterator containing a list of the tokens in 
         *        the testLine
         * @param testLine is the response from the server
         */
        public boolean testProtocolInBlock( PrintWriter out, BufferedReader in, ListIterator testTokens, String testLine) throws Exception
        {
            boolean retval = false;
            Iterator tests = _elementTests.iterator();
            while ( tests.hasNext() ) {
                ElementTest test = (ElementTest)tests.next();
                if ( _location != null ) {
                    test.setLocation( _location );
                }
                //System.err.println("testLine="+testLine);
                retval = test.softTest( testTokens, testLine );
                if (retval == false) {
                   break;
                }
            }
            return retval;
        }

        /**
         * Default version of testing.  Tests the response from the server and
         * assumes that every SL line between CL lines is in the same order.
         * @param out PrintWriter for talking to the server
         * @param in BufferedReader for getting the server response
         */
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

        /**
         * Grabs a line from the server and throws an error message if it  
         * doesn't work out
         * @param in BufferedReader for getting the server response
         * @return String of the line from the server
         */
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
  
            boolean softTest( ListIterator testElements, String line) throws Exception {
                return doNonAssertingTest(testElements);
            }

            abstract void doTest( ListIterator testElements ) throws Exception;

            /**
             * non Asserting version of doTest that instead of throwing an
             * assert, just gently retunrs a boolean 
             * @param testElements the elements to test with
             * @return boolean true if success false if failed
             */    
            abstract boolean doNonAssertingTest( ListIterator testElements)
              throws Exception;
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
        
            //comment in ElementTest 
            public boolean doNonAssertingTest( ListIterator testElements ) 
            throws Exception {
                String next;
                if ( testElements.hasNext() ) {
                    next = (String) testElements.next();
                }
                else {
                    next = "No more elements";
                }
                if ( !_elementValue.equals(next) ) { 
                  //System.err.println("emement value="+_elementValue+
                  //" did not =next+"+
                  //next);
                  return false;
                }
                  //System.err.println("emement value="+_elementValue+
                  //" did =next+"+
                  //next);
                return true;
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
            public boolean doNonAssertingTest( ListIterator testElements ) throws Exception
            {
                for ( int i = 0; i < _elementsToConsume; i++ )
                {
                    if ( ! testElements.hasNext() ) {
                        return false;
                    }
                    String ignored = (String)testElements.next();
                }
                return true;
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
        
            public boolean doNonAssertingTest( ListIterator testElements ) 
            throws Exception
            {
                if ( testElements.hasNext() ) {
                    String nextElement = (String)testElements.next();
                    return false;
                }
                return true;
            }
        }
    }

    protected interface ProtocolLine
    {
        void testProtocol( PrintWriter out, BufferedReader in ) throws Exception;
        void testProtocolBlock(PrintWriter out, BufferedReader in, List list)
        throws Exception;
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

            } else if ( next.startsWith("SUB: ") ) {
              //System.err.println("Hit SUB ");
              List unorderedBlock = new ArrayList(5);
              next = reader.readLine();
              //System.err.println("next = " + next);
              String serverMsg = next.substring( 3 );
              while ( !next.startsWith("SUB:") ) {
                   unorderedBlock.add(
                               new ServerResponse( serverMsg, location, false )
                                     );
                   next = reader.readLine();
                   serverMsg = next.substring( 3 );
                   lineNumber++;
                   //System.err.println("next = " + next);
              }
              protocolLines.add(unorderedBlock);  
            } else if ( next.startsWith( "//" )
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
