/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
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
