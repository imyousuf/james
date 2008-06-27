/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
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
