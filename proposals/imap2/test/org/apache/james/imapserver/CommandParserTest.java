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

package org.apache.james.imapserver;

import org.apache.james.imapserver.commands.CommandParser;
import org.apache.james.imapserver.commands.IdSet;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.Calendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import java.text.DateFormat;

/**
 * Tests for the {@link ImapRequestLineReader}.
 * TODO: atom, literal, other (not yet implemented) arguments
 *
 * @version $Revision: 1.5.2.3 $
 */
public class CommandParserTest
        extends TestCase
{
    private CommandParser parser = new CommandParser();
    private ByteArrayOutputStream output = new ByteArrayOutputStream();

    public CommandParserTest( String s )
    {
        super( s );
    }

    /**
     * Tests handling of the "atom" argument type.
     * TODO: check special characters.
     * @throws Exception
     */
    public void testAtom() throws Exception
    {
        String testRequest = "a01 a.not.her one\ntwo ";
        ImapRequestLineReader request = getRequest( testRequest );

        assertEquals( "a01", parser.atom( request ) );
        assertEquals( "a.not.her", parser.atom( request ) );
        assertEquals( "one", parser.atom( request ) );
        request.eol();
        request.consumeLine();
        assertEquals( "two", parser.atom( request ) );

        // Must have \n or " " after atom.
        testRequest = "a01";
        request = getRequest( testRequest );
        try {
            String test = parser.atom( request );
            fail( "shouldn't get here" );
        }
        catch ( ProtocolException e ) {}
    }

    /**
     * Test handling of the "tag" argument type.
     */
    public void testTag() throws Exception
    {
        String testRequest = "this-is-ok this+is+not+ok";
        ImapRequestLineReader request = getRequest( testRequest );

        assertEquals( "this-is-ok", parser.tag( request ));
        try {
            String test = parser.tag( request );
            fail( "Tags may not contain the '+' character." );
        }
        catch ( ProtocolException e ) {}
    }

    /**
     * Tests handling of quoted strings.
     * TODO: illegal characters. illegal escapes
     */
    public void testQuoted() throws Exception
    {
        String testRequest = "\"word\" \"words with spaces\" \"\" " +
                "\"esca\\\\ped \\\" chars\"";
        ImapRequestLineReader request = getRequest( testRequest );

        assertEquals( "word", parser.astring( request) );
        assertEquals( "words with spaces", parser.astring( request)  );
        assertEquals( "", parser.astring( request)  );
        assertEquals( "esca\\ped \" chars", parser.astring( request ) );
    }

    /**
     * Tests handling of "literal" arguments.
     * TODO: test this thoroughly
     */
    public void testLiteral() throws Exception
    {
        // Synchronized literal.
        String test = "{24+}\r\nA \tsynchronized \nliteral {27}\r\nThe \tunsynchronized\nliteral";
        ImapRequestLineReader request = getRequest(test );

        assertEquals( "A \tsynchronized \nliteral", parser.astring( request ) );
        // Make sure we didn't get a command continuation response
        assertEquals( "", getServerResponse() );

        assertEquals( "The \tunsynchronized\nliteral", parser.astring( request ) );
        // Make sure we got a command continuation response
        assertEquals( "+\r\n", getServerResponse() );

    }

    /**
     * Test handling of astring arguments. More detailed tests for atom,
     * quoted and literal should be in specific argument tests.
     */
    public void testAstring() throws Exception
    {
        String testRequest = "atom at.om \"quoted\" \"\" {6+}\r\n\"here\"";
        ImapRequestLineReader request = getRequest( testRequest );

        assertEquals( "atom", parser.astring( request ) );
        assertEquals( "at.om", parser.astring( request ));
        assertEquals( "quoted", parser.astring( request ));
        assertEquals( "", parser.astring( request ));
        assertEquals( "\"here\"", parser.astring( request ));
    }

    /**
     * Tests for reading "mailbox" arguments. This is simply an "astring", where the
     * special name "INBOX" is treated case insensitive.
     */
    public void testMailbox() throws Exception
    {
        String testRequest = "mailboxName \"quotedName\" {11+}\nliteralName iNbOx ";
        ImapRequestLineReader request = getRequest( testRequest );

        assertEquals( "mailboxName", parser.mailbox( request ) );
        assertEquals( "quotedName", parser.mailbox( request ));
        assertEquals( "literalName", parser.mailbox( request ));
        assertEquals( "INBOX", parser.mailbox( request ));
    }

    /**
     * Tests for reading "date-time" arguments.
     * TODO this test fails, as timezones aren't handled properly - need to investigate.
     */
    public void xtestDateTime() throws Exception
    {
        String testRequest = "\"20-Mar-1971 00:23:02 +0000\"";
        ImapRequestLineReader request = getRequest( testRequest );

        SimpleDateFormat formatter
            = new SimpleDateFormat ("yyyyMMdd hh:mm:ss");
        formatter.setTimeZone( TimeZone.getTimeZone( "UTC" ));
        String actual = formatter.format( parser.dateTime( request ) );
        assertEquals( "19710320 00:23:02", actual );
    }

    /**
     * Tests parsing of "set" arguments.
     */ 
    public void testIdSet() throws Exception
    {
        String testRequest = "8 25 1:4 33:* 2,3,4 1,4:6,8:* ";
        ImapRequestLineReader request = getRequest( testRequest );

        IdSet idSet;
        idSet = parser.set( request );
        checkSet( idSet, new long[]{8}, new long[]{0, 2, 7, 9, 20, Long.MAX_VALUE } );

        idSet = parser.set( request );
        checkSet( idSet, new long[]{ 25 }, new long[]{ 0, 5, 20, 30, Long.MAX_VALUE } );

        idSet = parser.set( request );
        checkSet( idSet, new long[]{ 1, 2, 3, 4 }, new long[]{0, 5, 10 } );

        idSet = parser.set( request );
        checkSet( idSet, new long[]{ 33, 35, 100, 1000, Long.MAX_VALUE}, new long[]{0, 1, 32});

        idSet = parser.set( request );
        checkSet( idSet, new long[]{ 2,3,4}, new long[]{0, 1, 5,8 });

        idSet = parser.set( request );
        checkSet( idSet, new long[]{ 1,4,5,6,8,100,1000,Long.MAX_VALUE}, new long[]{0,2,3,7});

    }

    private void checkSet( IdSet idSet, long[] includes, long[] excludes )
    {
        for ( int i = 0; i < includes.length; i++ ) {
            assertTrue( idSet.includes( includes[i] ));
        }
        for ( int i = 0; i < excludes.length; i++ ) {
            assertTrue( ! idSet.includes( excludes[i] ));
        }
    }

    /**
     * Builds and ImapRequestLineReader with the specified string, using {@link #output}
     * as the server writer for command continuation requests
     * @param testRequest A string containing client requests.
     * @return An initialised ImapRequestLineReader
     */
    private ImapRequestLineReader getRequest( String testRequest ) throws Exception
    {
        InputStream input = new ByteArrayInputStream( testRequest.getBytes( "US-ASCII" ) );
        // Clear the writer.
        output.reset();
        ImapRequestLineReader request = new ImapRequestLineReader( input, output );
        return request;
    }

    private String getServerResponse() throws UnsupportedEncodingException
    {
        byte[] bytesOut = output.toByteArray();
        output.reset();
        return new String( bytesOut, "US-ASCII" );
    }


}
