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


package org.apache.james.util.watchdog;

import java.io.IOException;
import java.io.OutputStream;

/**
 * This will reset the Watchdog each time a certain amount of data has
 * been transferred.  This allows us to keep the timeout settings low, while
 * not timing out during large data transfers.
 */
public class BytesWrittenResetOutputStream extends OutputStream {

    /**
     * The output stream wrapped by this method
     */
    OutputStream out = null;

    /**
     * The Watchdog to be reset every lengthReset bytes
     */
    private Watchdog watchdog;

    /**
     * The number of bytes that need to be written before the counter is reset.
     */
    int lengthReset = 0;

    /**
     * The number of bytes written since the counter was last reset
     */
    int writtenCounter = 0;

    public BytesWrittenResetOutputStream(OutputStream out,
                                         Watchdog watchdog,
                                         int lengthReset) {
        this.out = out;
        this.watchdog = watchdog;
        this.lengthReset = lengthReset;

        writtenCounter = 0;
    }

    /**
     * Write an array of bytes to the stream
     *
     * @param b the array of bytes to write to the stream
     * @param off the index in the array where we start writing
     * @param len the number of bytes of the array to write
     *
     * @throws IOException if an exception is encountered when writing
     */
    public void write(byte[] b, int off, int len) throws IOException {
        out.write(b, off, len);
        writtenCounter += len;

        if (writtenCounter > lengthReset) {
            writtenCounter = 0;
            watchdog.reset();
        }
    }

    /**
     * Write a byte to the stream
     *
     * @param b the byte to write to the stream
     *
     * @throws IOException if an exception is encountered when writing
     */
    public void write(int b) throws IOException {
        out.write(b);
        writtenCounter++;

        if (writtenCounter > lengthReset) {
            writtenCounter = 0;
            watchdog.reset();
        }
    }

    /**
     * Flush the stream
     *
     * @throws IOException if an exception is encountered when flushing
     */
    public void flush() throws IOException {
        out.flush();
    }

    /**
     * Close the stream
     *
     * @throws IOException if an exception is encountered when closing
     */
    public void close() throws IOException {
        out.close();
    }
}
