/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

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
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.7 $
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
