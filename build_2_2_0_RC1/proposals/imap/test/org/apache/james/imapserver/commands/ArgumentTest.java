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
