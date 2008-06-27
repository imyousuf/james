/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.util;

import java.io.IOException;
import java.io.InputStream;

/**
 * Reads data off a stream, printing every byte read to System.err.
 */
public class DebugInputStream extends InputStream {

    /**
     * The input stream being wrapped
     */
    InputStream in = null;

    /**
     * Constructor that takes an InputStream to be wrapped.
     *
     * @param in the InputStream to be wrapped
     */
    public DebugInputStream(InputStream in) {
        this.in = in;
    }

    /**
     * Read a byte off the stream
     *
     * @return the byte read off the stream
     * @throws IOException if an exception is encountered when reading
     */
    public int read() throws IOException {
        int b = in.read();
        System.err.write(b);
        return b;
    }

    /**
     * Close the stream
     *
     * @throws IOException if an exception is encountered when closing
     */
    public void close() throws IOException {
        in.close();
    }
}
