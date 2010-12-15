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
package org.apache.james.server.netty;

import java.io.IOException;
import java.io.InputStream;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

/**
 * {@link InputStream} implementation which can be used to write {@link ChannelBuffer} 
 * objects to and read them
 * 
 * TODO: Remove this class when it is included in netty.
 *       See: https://jira.jboss.org/jira/browse/NETTY-307
 *   
 */
public class BlockingChannelBufferInputStream extends InputStream{
    private final Object mutex = new Object();

    private final ChannelBuffer buf;

    private volatile boolean closed;

    private volatile boolean released;

    private IOException exception;

    public BlockingChannelBufferInputStream() {
        buf = ChannelBuffers.dynamicBuffer();
    }

    @Override
    public int available() {
        if (released) {
            return 0;
        }

        synchronized (mutex) {
            return buf.readableBytes();
        }
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        synchronized (mutex) {
            closed = true;
            releaseBuffer();

            mutex.notifyAll();
        }
    }

    @Override
    public int read() throws IOException {
        synchronized (mutex) {
            if (!waitForData()) {
                return -1;
            }

            return buf.readByte() & 0xff;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        synchronized (mutex) {
            if (!waitForData()) {
                return -1;
            }

            int readBytes;

            if (len > buf.readableBytes()) {
                readBytes = buf.readableBytes();
            } else {
                readBytes = len;
            }

            buf.readBytes(b, off, readBytes);

            return readBytes;
        }
    }

    private boolean waitForData() throws IOException {
        if (released) {
            return false;
        }

        synchronized (mutex) {
            while (!released && buf.readableBytes() == 0 && exception == null) {
                try {
                    mutex.wait();
                } catch (InterruptedException e) {
                    IOException ioe = new IOException(
                            "Interrupted while waiting for more data");
                    ioe.initCause(e);
                    throw ioe;
                }
            }
        }

        if (exception != null) {
            releaseBuffer();
            throw exception;
        }

        if (closed && buf.readableBytes() == 0) {
            releaseBuffer();

            return false;
        }

        return true;
    }

    private void releaseBuffer() {
        if (released) {
            return;
        }

        released = true;
    }

    /**
     * Write the {@link ChannelBuffer} to {@link InputStream} and unblock the
     * read methods
     * 
     * @param src buffer
     */
    public void write(ChannelBuffer src) {
        synchronized (mutex) {
            if (closed) {
                return;
            }

            if (buf.readable()) {
                
                this.buf.writeBytes(src);
                //this.buf.readerIndex(0);
            } else {
                this.buf.clear();
                this.buf.writeBytes(src);
                this.buf.readerIndex(0);
                mutex.notifyAll();
            }
        }
    }

    /**
     * Throw the given {@link IOException} on the next read call
     * 
     * @param e
     */
    public void throwException(IOException e) {
        synchronized (mutex) {
            if (exception == null) {
                exception = e;

                mutex.notifyAll();
            }
        }
    }


}
