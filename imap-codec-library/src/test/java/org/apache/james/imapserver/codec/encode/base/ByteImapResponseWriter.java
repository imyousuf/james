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
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.imap.message.response.imap4rev1.Literal;
import org.apache.james.imapserver.codec.encode.ImapResponseWriter;

/**
 * Class providing methods to send response messages from the server
 * to the client.
 */
public class ByteImapResponseWriter extends AbstractLogEnabled implements ImapConstants, ImapResponseWriter {
    
    private static final int LOWER_CASE_OFFSET = 'a' - 'A';
    
    private PrintWriter writer;
    private ByteArrayOutputStream out;
    private boolean skipNextSpace;
    
    public ByteImapResponseWriter(  )
    {
        clear();
    }
    
    public byte[] getBytes() throws Exception {
        writer.flush();
        out.flush();
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

    public void message( long number )
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
            if (character == ImapConstants.BACK_SLASH || character == DQUOTE) {
                writer.print(ImapConstants.BACK_SLASH);
            }
            writer.print(character);
        }
        writer.print(DQUOTE);
    }
    
    public void closeParen() {
        closeBracket(CLOSING_PARENTHESIS);
    }

    private void closeBracket(final char bracket) {
        writer.print(bracket);
        clearSkipNextSpace();
    }

    public void openParen() {
        openBracket(OPENING_PARENTHESIS);
    }

    private void openBracket(final char bracket) {
        space();
        writer.print(bracket);
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
    
    public void skipNextSpace() {
        skipNextSpace = true;
    }
    
    public void space() {
        if (skipNextSpace) {
            skipNextSpace = false;
        } else {
            writer.print(SP_CHAR);
        }
    }

    public void literal(Literal literal) throws IOException {
        space();
        writer.flush();
        WritableByteChannel channel = Channels.newChannel(out);
        literal.writeTo(channel);
        writer.flush();
    }

    public void closeSquareBracket() throws IOException {
        closeBracket(CLOSING_SQUARE_BRACKET);
    }

    public void openSquareBracket() throws IOException {
        openBracket(OPENING_SQUARE_BRACKET);
    }

    public void upperCaseAscii(String message) throws IOException {
        upperCaseAscii(message, false);
    }

    private void upperCaseAscii(String message, boolean quote) {
        space();
        if (quote) writer.print(DQUOTE);
        // message is ASCII
        final int length = message.length();
        for (int i=0;i<length;i++) {
            final char next = message.charAt(i);
            if (next >= 'a' && next <= 'z') {
                writer.print(next + LOWER_CASE_OFFSET);
            } else {
                writer.print(next);
            }
        }
        if (quote) writer.print(DQUOTE);
    }

    public void quoteUpperCaseAscii(String message) {
        upperCaseAscii(message, true);
    }
}
