package org.apache.james.core;

import java.io.*;

public abstract class JamesMimeMessageInputStream extends InputStream {
    InputStream stream = null;

    //Loads the underlying input stream...
    protected abstract InputStream openStream() throws IOException;

    public int available() throws IOException {
        if (stream == null) {
            return 0;
        } else {
            return stream.available();
        }
    }

    public int read() throws IOException {
        if (stream == null) {
            synchronized (this) {
                if (stream == null) {
                    stream = openStream();
                } else {
                    //Another thread has already opened this stream
                }
            }
        }
        return stream.read();
    }

    public synchronized void close() throws IOException {
        if (stream != null) {
            stream.close();
        }
        stream = null;
    }

    public synchronized void reset() throws IOException {
        close();
    }
}