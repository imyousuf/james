/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.StringTokenizer;

/**
 * Wraps the client input reader with a bunch of convenience methods, allowing lookahead=1
 * on the underlying character stream.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
 */
public class ImapRequestLineReader
{
    private Reader reader;

    private boolean nextSeen = false;
    private char nextChar; // unknown

    ImapRequestLineReader( Reader reader )
    {
        this.reader = reader;
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
            // Move to the next line, and throw exception.
            eol();
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
                next = reader.read();
            }
            catch ( IOException e ) {
                throw new ProtocolException( "Error reading from stream." );
            }
            if ( next == -1 ) {
                throw new ProtocolException( "Unexpected end of stream." );
            }
            else {
           }

            nextSeen = true;
            nextChar = ( char ) next;
        }
        return nextChar;
    }

    /**
     * Moves the request line reader to the next line, by consuming any
     * trailing whitespace, and CRLF.
     * @throws ProtocolException If more non-space tokens are found in this line,
     *                           or the end-of-file is reached. All extra tokens are consumed regardless.
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

        // Need to consume until the end-of-line, regardless.
        while ( consume() != '\n' ) {
            // keep on consuming
        }

        // Check if we found extra characters.
        if ( next != '\n' ) {
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
    public void read( char[] holder ) throws ProtocolException
    {
        try {
            reader.read( holder );
        }
        catch ( IOException e ) {
            throw new ProtocolException( "Error reading from stream." );
        }
    }
}
