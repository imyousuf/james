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

import org.apache.oro.text.perl.Perl5Util;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A protocol session which can be run against a reader and writer, which checks
 * the server response against the expected values.
 * TODO make ProtocolSession itself be a permissible ProtocolElement,
 * so that we can nest and reuse sessions.
 *
 * @version $Revision: 1.4.2.3 $
 */
public class ProtocolSession
{
    protected List testElements = new ArrayList();
    private static final Perl5Util perl = new Perl5Util();

    /**
     * Executes the ProtocolSession in real time against the reader and writer
     * supplied, writing client requests and reading server responses in the order
     * that they appear in the test elements.
     * If an exception occurs, no more test elements are executed.
     * @param out The client requests are written to here.
     * @param in The server responses are read from here.
     */
    public void runLiveSession( PrintWriter out, BufferedReader in ) throws Exception
    {
        for ( Iterator iter = testElements.iterator(); iter.hasNext(); ) {
            Object obj = iter.next();
            if ( obj instanceof ProtocolElement ) {
                ProtocolElement test = ( ProtocolElement ) obj;
                test.testProtocol( out, in );
            }
        }
    }

    /**
     * Write an entire client session to the specified PrintWriter. Server
     * responses are not collected, but clients may collect themfor later
     * testing with {@link #testResponse}.
     * @param out The client requests are written to here.
     */
    public void writeClient( PrintWriter out ) throws Exception
    {
        Iterator iterator = testElements.iterator();
        while ( iterator.hasNext() ) {
            ProtocolElement element = (ProtocolElement) iterator.next();
            if ( element instanceof ClientRequest ) {
                element.testProtocol( out, null );
            }
        }
    }

    /**
     * Reads Server responses from the supplied Buffered reader, ensuring that
     * they match the expected responses for the protocol session. This permits
     * clients to run a session asynchronously, by first writing the client requests
     * with {@link #writeClient} and later testing the responses.
     * @param in The server responses are read from here.
     */
    public void testResponse( BufferedReader in ) throws Exception
    {
        Iterator iterator = testElements.iterator();
        while ( iterator.hasNext() ) {
            ProtocolElement element = (ProtocolElement) iterator.next();
            if ( element instanceof ServerResponse ) {
                element.testProtocol( null, in );
            }
        }
    }

    /**
     * adds a new Client request line to the test elements
     */
    public void CL( String clientLine )
    {
        testElements.add( new ClientRequest( clientLine ) );
    }

    /**
     * adds a new Server Response line to the test elements, with the specified location.
     */
    public void SL( String serverLine, String location )
    {
        testElements.add( new ServerResponse( serverLine, location ) );
    }

    /**
     * adds a new Server Unordered Block to the test elements.
     */
    public void SUB( List serverLines, String location )
    {
        testElements.add( new ServerUnorderedBlockResponse( serverLines, location ) );
    }

    /**
     * Adds a ProtocolElement to the test elements.
     */
    public void addProtocolElement( ProtocolElement element )
    {
        testElements.add( element );
    }

    /**
     * A client request, which write the specified message to a Writer.
     */
    private class ClientRequest implements ProtocolElement
    {
        private String message;

        /**
         * Initialises the ClientRequest with the supplied message.
         */
        public ClientRequest( String message )
        {
            this.message = message;
        }

        /**
         * Writes the request message to the PrintWriter.
         */
        public void testProtocol( PrintWriter out, BufferedReader in )
        {
            out.write( message );
            out.write( '\r' );
            out.write( '\n' );
            out.flush();
        }
    }

    /**
     * Represents a single-line server response, which reads a line
     * from a reader, and compares it with the defined regular expression
     * definition of this line.
     */
    private class ServerResponse implements ProtocolElement
    {
        private String expectedLine;
        protected String location;

        /**
         * Sets up a server response.
         * @param expectedPattern A Perl regular expression pattern used to test
         *                        the line recieved.
         * @param location A descriptive value to use in error messages.
         */
        public ServerResponse( String expectedPattern, String location )
        {
            this.expectedLine = expectedPattern;
            this.location = location;
        }

        /**
         * Reads a line from the supplied reader, and tests that it matches
         * the expected regular expression.
         * @param out Is ignored.
         * @param in The server response is read from here.
         * @throws InvalidServerResponseException If the actual server response didn't
         *          match the regular expression expected.
         */
        public void testProtocol( PrintWriter out, BufferedReader in )
                throws InvalidServerResponseException
        {
            String testLine = readLine( in );
            if ( ! match( expectedLine, testLine ) ) {
                String errMsg = "\nLocation: " + location +
                        "\nExcpected: " + expectedLine +
                        "\nActual   : " + testLine;
                throw new InvalidServerResponseException( errMsg );
            }
        }

        /**
         * A convenience method which returns true if the actual string
         * matches the expected regular expression.
         * @param expected The regular expression used for matching.
         * @param actual The actual message to match.
         * @return <code>true</code> if the actual matches the expected.
         */
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
        protected String readLine( BufferedReader in )
                throws InvalidServerResponseException
        {
            try {
                return in.readLine();
            }
            catch ( IOException e ) {
                String errMsg = "\nLocation: " + location +
                        "\nExpected: " + expectedLine +
                        "\nReason: Server Timeout.";
                throw new InvalidServerResponseException( errMsg );
            }
        }
    }

    /**
     * Represents a set of lines which must be recieved from the server,
     * in a non-specified order.
     */
    private class ServerUnorderedBlockResponse extends ServerResponse
    {
        private List expectedLines = new ArrayList();

        /**
         * Sets up a ServerUnorderedBlockResponse with the list of expected lines.
         * @param expectedLines A list containing a reqular expression for each
         *                      expected line.
         * @param location A descriptive location string for error messages.
         */
        public ServerUnorderedBlockResponse( List expectedLines, String location )
        {
            super( "<Unordered Block>", location );
            this.expectedLines = expectedLines;
        }

        /**
         * Reads lines from the server response and matches them against the
         * list of expected regular expressions. Each regular expression in the
         * expected list must be matched by only one server response line.
         * @param out Is ignored.
         * @param in Server responses are read from here.
         * @throws InvalidServerResponseException If a line is encountered which doesn't
         *              match one of the expected lines.
         */
        public void testProtocol( PrintWriter out, BufferedReader in )
                throws InvalidServerResponseException
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

                    throw new InvalidServerResponseException( errMsg.toString() );
                }
            }
        }
    }

    /**
     * Represents a generic protocol element, which may write requests to the server,
     * read responses from the server, or both. Implementations should test the server
     * response against an expected response, and throw an exception on mismatch.
     */
    interface ProtocolElement
    {
        /**
         * Executes the ProtocolElement against the supplied read and writer.
         * @param out Client requests are written to here.
         * @param in Server responses are read from here.
         * @throws InvalidServerResponseException If the actual server response
         *              doesn't match the one expected.
         */
        void testProtocol( PrintWriter out, BufferedReader in )
                throws InvalidServerResponseException;
    }

    /**
     * An exception which is thrown when the actual response from a server
     * is different from that expected.
     */
    public class InvalidServerResponseException extends Exception
    {
        public InvalidServerResponseException( String message )
        {
            super( message );
        }
    }

}
