/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import org.apache.james.imapserver.commands.CommandParser;

import junit.framework.TestCase;

import java.io.StringReader;
import java.io.BufferedReader;

/**
 * Tests for the {@link ImapRequestLineReader}.
 * TODO: atom, literal, other (not yet implemented) arguments
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
public class CommandParserTest
        extends TestCase
{
    private CommandParser parser = new CommandParser();

    public CommandParserTest( String s )
    {
        super( s );
    }

    public void testTag() throws Exception
    {
        String testRequest = "a01 a.not.her not+ok";
        ImapRequestLineReader request = getRequest( testRequest );

        assertEquals( "a01", parser.tag( request ) );
        assertEquals( "a.not.her", parser.tag( request ) );

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
        String test = "{24}\nThese \tare 24\ncharacters";
        ImapRequestLineReader request = getRequest(test );

        assertEquals( "These \tare 24\ncharacters", parser.astring( request ) );
    }

    /**
     * Test handling of astring arguments. More detailed tests for atom,
     * quoted and literal should be in specific argument tests.
     * TODO: add literal
     */
    public void testAstring() throws Exception
    {
        String testRequest = "atom at.om \"quoted\" \"\" {6}\n\"here\"";
        ImapRequestLineReader request = getRequest( testRequest );

        assertEquals( "atom", parser.astring( request ) );
        assertEquals( "at.om", parser.astring( request ));
        assertEquals( "quoted", parser.astring( request ));
        assertEquals( "", parser.astring( request ));
        assertEquals( "\"here\"", parser.astring( request ));
    }

    private ImapRequestLineReader getRequest( String testRequest )
    {
        BufferedReader reader = new BufferedReader( new StringReader( testRequest ) );
        ImapRequestLineReader request = new ImapRequestLineReader( reader );
        return request;
    }
}
