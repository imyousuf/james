/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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

package org.apache.james.test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * A builder which generates a ProtocolSession from a test file.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.6 $
 */
public class FileProtocolSessionBuilder
{
    private static final String CLIENT_TAG = "C:";
    private static final String SERVER_TAG = "S:";
    private static final String OPEN_UNORDERED_BLOCK_TAG = "SUB {";
    private static final String CLOSE_UNORDERED_BLOCK_TAG = "}";
    private static final String COMMENT_TAG = "#";

    /**
     * Builds a ProtocolSession by reading lines from the test file
     * with the supplied name.
     * @param fileName The name of the protocol session file.
     * @return The ProtocolSession
     */
    public ProtocolSession buildProtocolSession( String fileName )
            throws Exception
    {
        ProtocolSession session = new ProtocolSession();
        addTestFile( fileName, session );
        return session;
    }

    /**
     * Adds all protocol elements from a test file to the ProtocolSession supplied.
     * @param fileName The name of the protocol session file.
     * @param session The ProtocolSession to add the elements to.
     */
    public void addTestFile( String fileName, ProtocolSession session )
            throws Exception
    {
        // Need to find local resource.
        InputStream is = this.getClass().getResourceAsStream( fileName );
        if ( is == null ) {
            throw new Exception( "Test Resource '" + fileName + "' not found." );
        }

        addProtocolLinesFromStream( is, session, fileName );
    }

    /**
     * Reads ProtocolElements from the supplied InputStream and adds
     * them to the ProtocolSession.
     * @param is The input stream containing the protocol definition.
     * @param session The ProtocolSession to add elements to.
     * @param fileName The name of the source file, for error messages.
     */ 
    public void addProtocolLinesFromStream( InputStream is,
                                             ProtocolSession session,
                                             String fileName )
            throws Exception
    {
        BufferedReader reader = new BufferedReader( new InputStreamReader( is ) );
        String next;
        int lineNumber = 1;
        while ( ( next = reader.readLine() ) != null ) {
            String location = fileName + ":" + lineNumber;
            if ( next.startsWith( CLIENT_TAG ) ) {
                String clientMsg = "";
                if ( next.length() > 3 ) {
                    clientMsg = next.substring( 3 );
                }
                session.CL( clientMsg );
            }
            else if ( next.startsWith( SERVER_TAG ) ) {
                String serverMsg = "";
                if ( next.length() > 3 ) {
                    serverMsg = next.substring( 3 );
                }
                session.SL( serverMsg, location );
            }
            else if ( next.startsWith( OPEN_UNORDERED_BLOCK_TAG ) ) {
                List unorderedLines = new ArrayList( 5 );
                next = reader.readLine();

                while ( !next.startsWith( CLOSE_UNORDERED_BLOCK_TAG ) ) {
                    if (! next.startsWith( SERVER_TAG ) ) {
                        throw new Exception( "Only 'S: ' lines are permitted inside a 'SUB {' block.");
                    }
                    String serverMsg = next.substring( 3 );
                    unorderedLines.add( serverMsg );
                    next = reader.readLine();
                    lineNumber++;
                }

                session.SUB( unorderedLines, location );
            }
            else if ( next.startsWith( COMMENT_TAG )
                    || next.trim().length() == 0 ) {
                // ignore these lines.
            }
            else {
                String prefix = next;
                if ( next.length() > 3 ) {
                    prefix = next.substring( 0, 3 );
                }
                throw new Exception( "Invalid line prefix: " + prefix );
            }
            lineNumber++;
        }
    }

}
