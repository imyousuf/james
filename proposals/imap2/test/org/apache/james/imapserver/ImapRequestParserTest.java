/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import junit.framework.TestCase;

import java.io.StringReader;
import java.io.BufferedReader;

/**
 * Tests for the {@link ImapRequestParser}.
 * TODO: atom, literal, other (not yet implemented) arguments
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
public class ImapRequestParserTest
        extends TestCase
{
    public ImapRequestParserTest( String s )
    {
        super( s );
    }

    public void testTag() throws Exception
    {
        String testRequest = "a01 a.not.her not+ok";
        ImapRequestParser parser = parse( testRequest );

        assertEquals( "a01", parser.tag() );
        assertEquals( "a.not.her", parser.tag() );

        try {
            String test = parser.tag();
            fail( "Tags may not contain the '+' character." );
        }
        catch ( ProtocolException e ) {}
    }

    /**
     * Tests handling of quoted strings.
     * TODO: special characters, escaped quotes
     */
    public void testQuoted() throws Exception
    {
        String testRequest = "\"word\" \"words with spaces\" \"\"";
        ImapRequestParser parser = parse( testRequest );

        assertEquals( "word", parser.quoted() );
        assertEquals( "words with spaces", parser.quoted() );
        assertEquals( "", parser.quoted() );
    }

    /**
     * Test handling of astring arguments. More detailed tests for atom,
     * quoted and literal should be in specific argument tests.
     * TODO: add literal
     */
    public void testAstring() throws Exception
    {
        String testRequest = "atom at.om \"quoted\" \"\"";
        ImapRequestParser parser = parse( testRequest );

        assertEquals( "atom", parser.astring() );
        assertEquals( "at.om", parser.astring() );
        assertEquals( "quoted", parser.astring() );
        assertEquals( "", parser.astring() );
    }

    private ImapRequestParser parse( String testRequest )
    {
        BufferedReader reader = new BufferedReader( new StringReader( testRequest ) );
        ImapRequestParser parser = new ImapRequestParser( reader );
        assertTrue( parser.nextRequest() );
        return parser;
    }
}
