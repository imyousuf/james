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

import org.apache.james.util.Assert;

import java.util.StringTokenizer;

class AstringArgument implements ImapArgument
{
    private String name;
    private boolean isFinal;

    public AstringArgument( String name )
    {
        this.name = name;
    }

    public Object parse( StringTokenizer tokens )
            throws Exception
    {
        // TODO: do this properly - need to check for disallowed characters.

        if ( ! tokens.hasMoreTokens() ) {
            throw new Exception( "Missing argument <" + this.name + ">");
        }
        String token = tokens.nextToken();
        Assert.isTrue( token.length() > 0 );

        StringBuffer astring = new StringBuffer( token );

        if ( astring.charAt(0) == '\"' ) {
            while ( astring.length() == 1 ||
                    astring.charAt( astring.length() - 1 ) != '\"' ) {
                if ( tokens.hasMoreTokens() ) {
                    astring.append( " " );
                    astring.append( tokens.nextToken() );
                }
                else {
                    throw new Exception( "Missing closing quote for <" + this.name + ">" );
                }
            }
            astring.deleteCharAt( 0 );
            astring.deleteCharAt( astring.length() - 1 );
        }

        return astring.toString();
    }

    public String format()
    {
        return "<" + this.name + ">";
    }

}
