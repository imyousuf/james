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

import java.io.BufferedReader;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * Tests for the {@link ImapRequestLineReader}.
 * TODO: atom, literal, other (not yet implemented) arguments
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.2 $
 */
public class CommandParserTest
        extends TestCase
{
    private CommandParser parser = new CommandParser();
    private StringWriter writer = new StringWriter();

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
        String test = "{24+}\nA \tsynchronized \nliteral {26}\nAn \tunsynchronized\nliteral";
        ImapRequestLineReader request = getRequest(test );

        assertEquals( "A \tsynchronized \nliteral", parser.astring( request ) );
        // Make sure we didn't get a command continuation response
        assertEquals( "", writer.getBuffer().toString() );

        assertEquals( "An \tunsynchronized\nliteral", parser.astring( request ) );
        // Make sure we got a command continuation response
        assertEquals( "+\n", writer.getBuffer().toString() );

    }

    /**
     * Test handling of astring arguments. More detailed tests for atom,
     * quoted and literal should be in specific argument tests.
     */
    public void testAstring() throws Exception
    {
        String testRequest = "atom at.om \"quoted\" \"\" {6+}\n\"here\"";
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

    private ImapRequestLineReader getRequest( String testRequest )
    {
        BufferedReader reader = new BufferedReader( new StringReader( testRequest ) );
        // Clear the writer.
        writer.getBuffer().setLength(0);
        ImapRequestLineReader request = new ImapRequestLineReader( reader, writer );
        return request;
    }
}
