/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.test;

import org.apache.oro.text.perl.Perl5Util;

import java.io.BufferedReader;
import java.io.InterruptedIOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * A protocol session which can be run against a reader and writer, which checks
 * the server response against the expected values.
 * TODO extract the FileProtocolSession from this generic one, so we can build
 * protocol sessions from files with different formats.
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
public class ProtocolSession
{
    protected List _testElements = new ArrayList();
    private static final Perl5Util perl = new Perl5Util();

    // comment in TestCase
    public void runSession( BufferedReader in, PrintWriter out ) throws Exception
    {
        for ( Iterator iter = _testElements.iterator(); iter.hasNext(); ) {
            Object obj = iter.next();
            if ( obj instanceof ProtocolElement ) {
                ProtocolElement test = ( ProtocolElement ) obj;
                test.testProtocol( out, in );
            }
        }
    }

    /**
     * Write an entire client session to the specified PrintWriter. Server
     * responses are ignored, but may be collected for later testing with
     * {@link #testResponse}.
     */
    public void writeClient( PrintWriter out ) throws Exception
    {
        Iterator iterator = _testElements.iterator();
        while ( iterator.hasNext() ) {
            ProtocolElement element = (ProtocolElement) iterator.next();
            if ( element instanceof ClientRequest ) {
                element.testProtocol( out, null );
            }
        }
    }

    public void testResponse( BufferedReader in ) throws Exception
    {
        Iterator iterator = _testElements.iterator();
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
        _testElements.add( new ClientRequest( clientLine ) );
    }

    /**
     * adds a new Server Response line to the test elements
     */
    public void SL( String serverLine, String location )
    {
        _testElements.add( new ServerResponse( serverLine, location ) );
    }

    /**
     * adds a new Server Response line to the test elements
     */
    public void SL( String serverLine )
    {
        _testElements.add( new ServerResponse( serverLine, "Location unknown.") );
    }

    /**
     * adds a new Server Unordered Block to the test elements.
     */
    public void SUB( List serverLines, String location )
    {
        _testElements.add( new UnorderedBlockResponse( serverLines, location ) );
    }

    /**
     * Adds a ProtocolElement to the test elements.
     */
    public void addProtocolElement( ProtocolElement element )
    {
        _testElements.add( element );
    }

    /**
     * This Line is sent to the server (everything after "CL: ") in expectation
     * that the server will respond.
     */
    private class ClientRequest implements ProtocolElement
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
    }

    private class ServerResponse implements ProtocolElement
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
                throw new InvalidServerResponseException( errMsg );
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
                throw new InvalidServerResponseException( errMsg );
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

        public void testProtocol( PrintWriter out, BufferedReader in )
                throws Exception
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

    interface ProtocolElement
    {
        void testProtocol( PrintWriter out, BufferedReader in ) throws Exception;
    }
}
