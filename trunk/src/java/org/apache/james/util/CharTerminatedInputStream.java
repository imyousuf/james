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

package org.apache.james.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * An InputStream class that terminates the stream when it encounters a
 * particular byte sequence.
 *
 * @version 1.0.0, 24/04/1999
 */
public class CharTerminatedInputStream
    extends InputStream {

    /**
     * The wrapped input stream
     */
    private InputStream in;

    /**
     * The terminating character array
     */
    private int match[];

    /**
     * An array containing the last N characters read from the stream, where
     * N is the length of the terminating character array
     */
    private int buffer[];

    /**
     * The number of bytes that have been read that have not been placed
     * in the internal buffer.
     */
    private int pos = 0;

    /**
     * Whether the terminating sequence has been read from the stream
     */
    private boolean endFound = false;

    /**
     * A constructor for this object that takes a stream to be wrapped
     * and a terminating character sequence.
     *
     * @param in the <code>InputStream</code> to be wrapped
     * @param terminator the array of characters that will terminate the stream.
     *
     * @throws IllegalArgumentException if the terminator array is null or empty
     */
    public CharTerminatedInputStream(InputStream in, char[] terminator) {
        if (terminator == null) {
            throw new IllegalArgumentException("The terminating character array cannot be null.");
        }
        if (terminator.length == 0) {
            throw new IllegalArgumentException("The terminating character array cannot be of zero length.");
        }
        match = new int[terminator.length];
        buffer = new int[terminator.length];
        for (int i = 0; i < terminator.length; i++) {
            match[i] = (int)terminator[i];
            buffer[i] = (int)terminator[i];
        }
        this.in = in;
    }

    /**
     * Read a byte off this stream.
     *
     * @return the byte read off the stream
     * @throws IOException if an IOException is encountered while reading off the stream
     */
    public int read() throws IOException {
        if (endFound) {
            //We've found the match to the terminator
            return -1;
        }
        if (pos == 0) {
            //We have no data... read in a record
            int b = in.read();
            if (b == -1) {
                //End of stream reached
                endFound = true;
                return -1;
            }
            if (b != match[0]) {
                //this char is not the first char of the match
                return b;
            }
            //this is a match...put this in the first byte of the buffer,
            // and fall through to matching logic
            buffer[0] = b;
            pos++;
        } else {
            if (buffer[0] != match[0]) {
                //Maybe from a previous scan, there is existing data,
                // and the first available char does not match the
                // beginning of the terminating string.
                return topChar();
            }
            //we have a match... fall through to matching logic.
        }
        //MATCHING LOGIC

        //The first character is a match... scan for complete match,
        // reading extra chars as needed, until complete match is found
        for (int i = 0; i < match.length; i++) {
            if (i >= pos) {
                int b = in.read();
                if (b == -1) {
                    //end of stream found, so match cannot be fulfilled.
                    // note we don't set endFound, because otherwise
                    // remaining part of buffer won't be returned.
                    return topChar();
                }
                //put the read char in the buffer
                buffer[pos] = b;
                pos++;
            }
            if (buffer[i] != match[i]) {
                //we did not find a match... return the top char
                return topChar();
            }
        }
        //A complete match was made...
        endFound = true;
        return -1;
    }

    /**
     * Private helper method to update the internal buffer of last read characters
     *
     * @return the byte that was previously at the front of the internal buffer
     */
    private int topChar() {
        int b = buffer[0];
        if (pos > 1) {
            //copy down the buffer to keep the fresh data at top
            System.arraycopy(buffer, 1, buffer, 0, pos - 1);
        }
        pos--;
        return b;
    }
}

