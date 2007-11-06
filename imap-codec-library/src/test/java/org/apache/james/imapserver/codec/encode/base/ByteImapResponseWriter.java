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

package org.apache.james.imapserver.codec.encode.base;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.imapserver.codec.encode.ImapResponseWriter;

/**
 * Class providing methods to send response messages from the server
 * to the client.
 */
public class ByteImapResponseWriter extends AbstractLogEnabled implements ImapConstants, ImapResponseWriter {
    
    private PrintWriter writer;
    private ByteArrayOutputStream out;
    private boolean skipNextSpace;
    
    public ByteImapResponseWriter(  )
    {
        clear();
    }
    
    public byte[] getBytes() {
        return out.toByteArray();
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
            space();
            writer.print( message );
        }
    }

    public void message( int number )
    {
        space();
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
        space();
        writer.print( commandName );
    }

    public void quote(String message) {
        space();
        writer.print(DQUOTE);
        final int length = message.length();
        for (int i=0;i<length;i++) {
            char character = message.charAt(i);
            if (character == ImapConstants.FORWARD_SLASH || character == DQUOTE) {
                writer.print(ImapConstants.FORWARD_SLASH);
            }
            writer.print(character);
        }
        writer.print(DQUOTE);
    }
    
    public void closeParen() {
        writer.print(CLOSING_PARENTHESIS);
        clearSkipNextSpace();
    }

    public void openParen() {
        space();
        writer.print(OPENING_PARENTHESIS);
        skipNextSpace();
    }
    
    public void clear() {
        this.out = new ByteArrayOutputStream();
        this.writer = new InternetPrintWriter( out, true );
        this.skipNextSpace = false;
    }
    
    private void clearSkipNextSpace() {
        skipNextSpace = false;
    }
    
    private void skipNextSpace() {
        skipNextSpace = true;
    }
    
    private void space() {
        if (skipNextSpace) {
            skipNextSpace = false;
        } else {
            writer.print(SP_CHAR);
        }
    }
}
