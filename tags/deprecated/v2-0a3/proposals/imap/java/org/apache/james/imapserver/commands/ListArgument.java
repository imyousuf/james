/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver.commands;

import java.util.StringTokenizer;
import java.util.ArrayList;
import java.util.List;

final class ListArgument implements ImapArgument
{
    private String type;

    public ListArgument( String type )
    {
        this.type = type;
    }

    public Object parse( StringTokenizer tokens ) throws Exception
    {
        // TODO: implement this properly.
        String attr = tokens.nextToken();
        List dataNames = new ArrayList();

        if ( !attr.startsWith( "(" ) ) {
            throw new Exception( "Missing '(': " );
        }
        else if ( attr.endsWith( ")" ) ) { //single attr in paranthesis
            dataNames.add( attr.substring( 1, attr.length() - 1 ) );
        }
        else { // multiple attrs
            dataNames.add( attr.substring( 1 ).trim() );
            while ( tokens.hasMoreTokens() ) {
                attr = tokens.nextToken();
                if ( attr.endsWith( ")" ) ) {
                    dataNames.add( attr.substring( 0, attr.length() - 1 ) );
                }
                else {
                    dataNames.add( attr );
                }
            }
        }

        return dataNames;
    }

    public String format()
    {
        return "( <" + this.type + ">+ )";
    }
}
