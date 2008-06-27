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
 * @version $Revision: 1.8 $
 */
public class FileProtocolSessionBuilder
{
    private static final String CLIENT_TAG = "C:";
    private static final String SERVER_TAG = "S:";
    private static final String OPEN_UNORDERED_BLOCK_TAG = "SUB {";
    private static final String CLOSE_UNORDERED_BLOCK_TAG = "}";
    private static final String COMMENT_TAG = "#";
    private static final String SESSION_TAG = "SESSION:";

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
        int sessionNumber = -1;
        BufferedReader reader = new BufferedReader( new InputStreamReader( is ) );
        String next;
        int lineNumber = -1;
        while ( ( next = reader.readLine() ) != null ) {
            String location = fileName + ":" + lineNumber;
            if ( next.startsWith( CLIENT_TAG ) ) {
                String clientMsg = "";
                if ( next.length() > 3 ) {
                    clientMsg = next.substring( 3 );
                }
                session.CL( sessionNumber, clientMsg );
            }
            else if ( next.startsWith( SERVER_TAG ) ) {
                String serverMsg = "";
                if ( next.length() > 3 ) {
                    serverMsg = next.substring( 3 );
                }
                session.SL( sessionNumber, serverMsg, location );
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

                session.SUB( sessionNumber, unorderedLines, location );
            }
            else if ( next.startsWith( COMMENT_TAG )
                    || next.trim().length() == 0 ) {
                // ignore these lines.
            }
            else if ( next.startsWith(SESSION_TAG)) {
                String number = next.substring(SESSION_TAG.length()).trim();
                if (number.length() == 0) {
                    throw new Exception("No session number specified");
                }
                sessionNumber = Integer.parseInt(number);
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
