/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
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
