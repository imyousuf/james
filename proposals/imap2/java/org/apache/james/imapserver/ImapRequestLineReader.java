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

package org.apache.james.imapserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Wraps the client input reader with a bunch of convenience methods, allowing lookahead=1
 * on the underlying character stream.
 * TODO need to look at encoding, and whether we should be wrapping an InputStream instead.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.4.2.1 $
 */
public class ImapRequestLineReader
{
    private InputStream input;
    private OutputStream output;

    private boolean nextSeen = false;
    private char nextChar; // unknown

    ImapRequestLineReader( InputStream input, OutputStream output )
    {
        this.input = input;
        this.output = output;
    }

    /**
     * Reads the next regular, non-space character in the current line. Spaces are skipped
     * over, but end-of-line characters will cause a {@link ProtocolException} to be thrown.
     * This method will continue to return
     * the same character until the {@link #consume()} method is called.
     * @return The next non-space character.
     * @throws ProtocolException If the end-of-line or end-of-stream is reached.
     */
    public char nextWordChar() throws ProtocolException
    {
        char next = nextChar();
        while ( next == ' ' ) {
            consume();
            next = nextChar();
        }

        if ( next == '\r' || next == '\n' ) {
            throw new ProtocolException( "Missing argument." );
        }

        return next;
    }

    /**
     * Reads the next character in the current line. This method will continue to return
     * the same character until the {@link #consume()} method is called.
     * @return The next character.
     * @throws ProtocolException If the end-of-stream is reached.
     */
    public char nextChar() throws ProtocolException
    {
        if ( ! nextSeen ) {
            int next = -1;

            try {
                next = input.read();
            }
            catch ( IOException e ) {
                throw new ProtocolException( "Error reading from stream." );
            }
            if ( next == -1 ) {
                throw new ProtocolException( "Unexpected end of stream." );
            }

            nextSeen = true;
            nextChar = ( char ) next;
//            System.out.println( "Read '" + nextChar + "'" );
        }
        return nextChar;
    }

    /**
     * Moves the request line reader to end of the line, checking that no non-space
     * character are found.
     * @throws ProtocolException If more non-space tokens are found in this line,
     *                           or the end-of-file is reached.
     */
    public void eol() throws ProtocolException
    {
        char next = nextChar();

        // Ignore trailing spaces.
        while ( next == ' ' ) {
            consume();
            next = nextChar();
        }

        // handle DOS and unix end-of-lines
        if ( next == '\r' ) {
            consume();
            next = nextChar();
        }

        // Check if we found extra characters.
        if ( next != '\n' ) {
            // TODO debug log here and other exceptions
            throw new ProtocolException( "Expected end-of-line, found more characters.");
        }
    }

    /**
     * Consumes the current character in the reader, so that subsequent calls to the request will
     * provide a new character. This method does *not* read the new character, or check if
     * such a character exists. If no current character has been seen, the method moves to
     * the next character, consumes it, and moves on to the subsequent one.
     * @throws ProtocolException if a the current character can't be obtained (eg we're at
     *                            end-of-file).
     */
    public char consume() throws ProtocolException
    {
        char current = nextChar();
        nextSeen = false;
        nextChar = 0;
        return current;
    }


    /**
     * Reads and consumes a number of characters from the underlying reader,
     * filling the char array provided.
     * @param holder A char array which will be filled with chars read from the underlying reader.
     * @throws ProtocolException If a char can't be read into each array element.
     */
    public void read( byte[] holder ) throws ProtocolException
    {
        int readTotal = 0;
        try
        {
            while ( readTotal < holder.length )
            {
                int count = 0;
                count = input.read( holder, readTotal, holder.length - readTotal );
                if ( count == -1 ) {
                    throw new ProtocolException( "Unexpectd end of stream." );
                }
                readTotal += count;
            }
            // Unset the next char.
            nextSeen = false;
            nextChar = 0;
        }
        catch ( IOException e ) {
            throw new ProtocolException( "Error reading from stream." );
        }

    }

    /**
     * Sends a server command continuation request '+' back to the client,
     * requesting more data to be sent.
     */
    public void commandContinuationRequest()
            throws ProtocolException
    {
        try {
            output.write( '+' );
            output.write( '\r' );
            output.write( '\n' );
            output.flush();
        }
        catch ( IOException e ) {
            throw new ProtocolException("Unexpected exception in sending command continuation request.");
        }
    }

    public void consumeLine()
            throws ProtocolException
    {
        char next = nextChar();
        while ( next != '\n' ) {
            consume();
            next = nextChar();
        }
        consume();
    }
}
