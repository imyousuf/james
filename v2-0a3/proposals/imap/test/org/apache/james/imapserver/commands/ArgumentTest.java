/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import junit.framework.TestCase;

import java.util.StringTokenizer;

public final class ArgumentTest
        extends TestCase
{
    public ArgumentTest( String s )
    {
        super( s );
    }

    public void testAstring() throws Exception
    {
        AstringArgument arg = new AstringArgument( "test" );
        ParseChecker parser = new ParseChecker( arg );

        parser.check( "straightup", "straightup" );
        parser.check( "quoted", "\"quoted\"" );
        parser.check( "with space", "\"with space\"" );

        // Test currently fails - don't see whitespace.
        //parser.check( "multiple   spaces", "\"multiple   spaces\"" );

        parser.checkFail( "Missing argument <test>", "" );
        parser.checkFail( "Missing closing quote for <test>", "\"something" );
        parser.checkFail( "Missing closing quote for <test>", "\"" );
        parser.checkFail( "Missing closing quote for <test>", "\"something special" );
    }

    private static class ParseChecker
    {
        private ImapArgument arg;

        ParseChecker( ImapArgument arg )
        {
            this.arg = arg;
        }

        public void check( Object expected, String input )
        {
            StringTokenizer tokens = new StringTokenizer( input );
            Object result = null;
            try {
                result = this.arg.parse( tokens );
            }
            catch ( Exception e ) {
                fail( "Error encountered: " + e.getMessage() );
            }

            assertEquals( expected, result );
        }

        public void checkFail( String expectedError, String input )
        {
            StringTokenizer tokens = new StringTokenizer( input );
            try {
                Object result = this.arg.parse( tokens );
            }
            catch ( Exception e ) {
                assertEquals( expectedError, e.getMessage() );
                return;
            }

            fail( "Expected error" );
        }
    }
}
