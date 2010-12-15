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
package org.apache.james.imapserver.netty;

import java.io.IOException;
import java.io.OutputStream;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

/**
 * {@link OutputStream} which write data to the wrapped {@link Channel}
 *
 * TODO: Remove this class when it is included in netty.
 *       See: https://jira.jboss.org/jira/browse/NETTY-307
 */
public class ChannelOutputStream extends OutputStream{

    private final Channel channel;

    private ChannelFuture lastChannelFuture;

    public ChannelOutputStream(Channel channel) {
        this.channel = channel;
    }

    /**
     * As this use {@link ChannelFuture#awaitUninterruptibly()} it should not be called from an I/O Thread! If so it will deadlock!
     */
    @Override
    public void close() throws IOException {
        try {
            flush();
        } finally {
            channel.close().awaitUninterruptibly();
        }
    }

    private void checkClosed() throws IOException {
        if (!channel.isConnected()) {
            throw new IOException("The session has been closed.");
        }
    }

    private synchronized void write(ChannelBuffer buf) throws IOException {
        checkClosed();
        ChannelFuture future = channel.write(buf);
        lastChannelFuture = future;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        write(ChannelBuffers.wrappedBuffer(b.clone(), off, len));
    }

    @Override
    public void write(int b) throws IOException {
        ChannelBuffer buf = ChannelBuffers.buffer(1);
        buf.writeByte((byte) b);
        write(buf);
    }

    /**
     * As this use {@link ChannelFuture#awaitUninterruptibly()} it should not be called from an I/O Thread! If so it will deadlock!
     */
    @Override
    public synchronized void flush() throws IOException {
        if (lastChannelFuture == null) {
            return;
        }

        lastChannelFuture.awaitUninterruptibly();
        if (!lastChannelFuture.isSuccess()) {
            throw new IOException(
                    "The bytes could not be written to the session");
        }
    }
    
    /**
     * Return the Channel
     * 
     * @return channel
     */
    public Channel getChannel() {
        return channel;
    }

}
