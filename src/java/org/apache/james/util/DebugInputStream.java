package org.apache.james.util;

import java.io.*;

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