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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;

public abstract class StreamChannelUpstreamHandler extends SimpleChannelUpstreamHandler{
    
    private static final String KEY_IN = "stream.in";
    private static final String KEY_OUT = "stream.out";

    private int readTimeout;

    private int writeTimeout;

    protected StreamChannelUpstreamHandler() {
        // Do nothing
    }

    /**
     * Implement this method to execute your stream I/O logic;
     * <b>please note that you must forward the process request to other
     * thread or thread pool.</b>
     */
    protected abstract void processStreamIo(ChannelHandlerContext ctx, InputStream in,
            OutputStream out);

    /**
     * Returns read timeout in seconds.
     * The default value is <tt>0</tt> (disabled).
     */
    public int getReadTimeout() {
        return readTimeout;
    }

    /**
     * Sets read timeout in seconds.
     * The default value is <tt>0</tt> (disabled).
     */
    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    /**
     * Returns write timeout in seconds.
     * The default value is <tt>0</tt> (disabled).
     */
    public int getWriteTimeout() {
        return writeTimeout;
    }

    /**
     * Sets write timeout in seconds.
     * The default value is <tt>0</tt> (disabled).
     */
    public void setWriteTimeout(int writeTimeout) {
        this.writeTimeout = writeTimeout;
    }

    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        
        // Create streams
        InputStream in = new  BlockingChannelBufferInputStream();
        OutputStream out = new ChannelBufferOutputStream(ctx.getChannel());
        Map<Object, Object> attachment = getAttachment(ctx);
        attachment.put(KEY_IN, in);
        attachment.put(KEY_OUT, out);
        processStreamIo(ctx, in, out);
        ctx.setAttachment(attachment);
        super.channelOpen(ctx, e);
    }

    @SuppressWarnings("unchecked")
    protected Map<Object,Object> getAttachment(ChannelHandlerContext ctx) {
        Map<Object,Object> attachment = (Map<Object, Object>) ctx.getAttachment();
        if (attachment == null) {
            attachment = new HashMap<Object, Object>();
            ctx.setAttachment(attachment);
        }
        return attachment;
    }
    /**
     * Closes streams
     */
    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        Map<Object, Object> attachment = getAttachment(ctx);

        final InputStream in = (InputStream) attachment.get(KEY_IN);
        final OutputStream out = (OutputStream) attachment.get(KEY_OUT);
        try {
            in.close();
        } finally {
            out.close();
        }
        super.channelClosed(ctx, e);
    }
    

    /**
     * Forwards read data to input stream.
     */
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        final BlockingChannelBufferInputStream in = (BlockingChannelBufferInputStream) getAttachment(ctx).get(KEY_IN);
        in.write((ChannelBuffer) e.getMessage());
        super.messageReceived(ctx, e);
    }

    /**
     * Forwards caught exceptions to input stream.
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        final BlockingChannelBufferInputStream in = (BlockingChannelBufferInputStream) getAttachment(ctx).get(KEY_IN);
        IOException ex = null;
        if (e.getCause() instanceof StreamIoException) {
            ex = (IOException) e.getCause().getCause();
        } else if (e.getCause() instanceof IOException) {
            ex = (IOException) e.getCause();
        }

        if (e != null && in != null) {
            in.throwException(ex);
        } else {
            ctx.getChannel().close();
        }
    }
    
    private static class StreamIoException extends RuntimeException {
        private static final long serialVersionUID = 3976736960742503222L;

        public StreamIoException(IOException cause) {
            super(cause);
        }
    }

}
