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

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

final class SetArgument implements ImapArgument
{
    public Object parse( StringTokenizer tokens ) throws Exception
    {
        if ( !tokens.hasMoreTokens() ) {
            throw new Exception( "Missing argument " + format() );
        }

        return tokens.nextToken();
    }

    private List parseSet( String rawSet )
    {
        List response = new ArrayList();

        int checkComma = rawSet.indexOf( "," );
        if ( checkComma == -1 ) {
            // No comma present
            int checkColon = rawSet.indexOf( ":" );
            if ( checkColon == -1 ) {
                // No colon present (single integer)
                Integer seqNum;
                if ( rawSet.equals( "*" ) ) {
                    seqNum = new Integer( -1 );
                }
                else {
                    seqNum = new Integer( rawSet.trim() );
                    if ( seqNum.intValue() < 1 ) {
                        throw new IllegalArgumentException( "Not a positive integer" );
                    }
                }
                response.add( seqNum );
            }
            else {
                // Simple sequence

                // Add the first number in the range.
                Integer firstNum = new Integer( rawSet.substring( 0, checkColon ) );
                int first = firstNum.intValue();
                if ( first < 1 ) {
                    throw new IllegalArgumentException( "Not a positive integer" );
                }
                response.add( firstNum );

                Integer lastNum;
                int last;
                if ( rawSet.indexOf( "*" ) != -1 ) {
                    // Range from firstNum to '*'
                    // Add -1, to indicate unended range.
                    lastNum = new Integer( -1 );
                }
                else {
                    // Get the final num, and add all numbers in range.
                    lastNum = new Integer( rawSet.substring( checkColon + 1 ) );
                    last = lastNum.intValue();
                    if ( last < 1 ) {
                        throw new IllegalArgumentException( "Not a positive integer" );
                    }
                    if ( last < first ) {
                        throw new IllegalArgumentException( "Not an increasing range" );
                    }

                    for ( int i = (first + 1); i <= last; i++ ) {
                        response.add( new Integer( i ) );
                    }
                }
            }

        }
        else {
            // Comma present, compound range.
            try {
                String firstRawSet = rawSet.substring( 0, checkComma );
                String secondRawSet = rawSet.substring( checkComma + 1 );
                response.addAll( parseSet( firstRawSet ) );
                response.addAll( parseSet( secondRawSet ) );
            }
            catch ( IllegalArgumentException e ) {
                throw e;
            }
        }
        return response;

    }

    public String format()
    {
        return "<set>";
    }

}
