/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
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
