/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.experimental.imapserver.encode;

import java.io.OutputStream;
import java.io.PrintWriter;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.experimental.imapserver.ImapConstants;
import org.apache.james.experimental.imapserver.ImapResponseWriter;
import org.apache.james.util.InternetPrintWriter;

/**
 * Class providing methods to send response messages from the server
 * to the client.
 */
public class OutputStreamImapResponseWriter extends AbstractLogEnabled implements ImapConstants, ImapResponseWriter {
    
    private PrintWriter writer;

    public OutputStreamImapResponseWriter( OutputStream output )
    {
        this.writer = new InternetPrintWriter( output, true );
    }

    /**
     * Writes the message provided to the client, prepended with the
     * untagged marker "*".
     *
     * @param message The message to write to the client.
     */
    public void untaggedResponse( String message )
    {
        untagged();
        message( message );
        end();
    }
    
    public void byeResponse( String message ) {
        untaggedResponse(BYE + SP + message);
    }

    public void untagged()
    {
        writer.print( UNTAGGED );
    }

    public void tag(String tag)
    {
        writer.print( tag );
    }

    public void message( String message )
    {
        if ( message != null ) {
            writer.print( SP );
            writer.print( message );
        }
    }

    public void message( int number )
    {
        writer.print( SP );
        writer.print( number );
    }

    public void responseCode( String responseCode )
    {
        if ( responseCode != null ) {
            writer.print( " [" );
            writer.print( responseCode );
            writer.print( "]" );
        }
    }

    public void end()
    {
        writer.println();
        writer.flush();
    }

    public void commandName(String commandName) {
        writer.print( SP );
        writer.print( commandName );
    }
}
