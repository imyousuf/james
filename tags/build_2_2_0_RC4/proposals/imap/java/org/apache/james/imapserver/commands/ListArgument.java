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
