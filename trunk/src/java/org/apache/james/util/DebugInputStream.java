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

public class DebugInputStream extends InputStream {
    InputStream in = null;

    public DebugInputStream(InputStream in) {
        this.in = in;
    }

	public int read() throws IOException {
        int b = in.read();
	    System.err.write(b);
        return b;
	}

    public void close() throws IOException {
        in.close();
    }
}
