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
package org.jboss.netty.handler.stream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketTimeoutException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.timeout.ReadTimeoutHandler;
import org.jboss.netty.util.Timer;

/**
 * Abstract base class which could be used if you need to use {@link InputStream} and {@link OutputStream} directly in your Handler. 
 * Because of the blocking nature of {@link InputStream} it will spawn a new Thread on every new connected {@link Channel}
 *  
 *  @author Norman Maurer 
 */
public abstract class StreamHandler extends ReadTimeoutHandler{
    
    private final ExecutorService executor;
    
    private static final String KEY_IN = "stream.in";
    private static final String KEY_OUT = "stream.out";
    
    /**
     *  Create a new Instance which use a cached ThreadPool with no limit to perform the stream handling
     *  
     * @param timer
     * @param readerIdleTimeSeconds
     */
    public StreamHandler(Timer timer, int readerIdleTimeSeconds) {
        this(timer, readerIdleTimeSeconds, Executors.newCachedThreadPool());
    }

    /**
     *  Create a new Instance which use a cached ThreadPool with no limit to perform the stream handling
     * 
     * @param timer
     * @param readerIdleTime
     * @param unit
     */
    public StreamHandler(Timer timer, long readerIdleTime, TimeUnit unit) {
        this(timer, readerIdleTime, unit, Executors.newCachedThreadPool());
    }
    
    /**
     *  Create a new Instance which use thre give {@link ExecutorService} to perform the stream handling
     *  
     * @param timer
     * @param readerIdleTimeSeconds
     * @param executor
     */
    public StreamHandler(Timer timer, int readerIdleTimeSeconds, ExecutorService executor) {
        super(timer, readerIdleTimeSeconds);
        this.executor = executor;
    }

    /**
     *  Create a new Instance which use thre give {@link ExecutorService} to perform the stream handling
     * 
     * @param timer
     * @param readerIdleTime
     * @param unit
     * @param executor
     */
    public StreamHandler(Timer timer, long readerIdleTime, TimeUnit unit, ExecutorService executor) {
        super(timer, readerIdleTime, unit);
        this.executor = executor;
    }


    /**
     * Implement this method to execute your stream I/O logic
     * 
     * The method will get executed in a new Thread
     * 
     */
    protected abstract void processStreamIo(final ChannelHandlerContext ctx, final InputStream in,
            OutputStream out);

  

    /**
     * Fire of the {@link #processStreamIo(ChannelHandlerContext, InputStream, OutputStream)} method
     */
    @Override
    public void channelConnected(final ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        
        // Create streams
        final InputStream in = new  BlockingChannelBufferInputStream();
        final OutputStream out = new ChannelOutputStream(ctx.getChannel());
        Map<Object, Object> attachment = getAttachment(ctx);
        attachment.put(KEY_IN, in);
        attachment.put(KEY_OUT, out);
        executor.execute(new Runnable() {
            
            public void run() {
                processStreamIo(ctx, in, out);                
            }
        });
        ctx.setAttachment(attachment);
        super.channelConnected(ctx, e);
    }

    /**
     * Return the Map which is used as Attachment to the {@link ChannelHandlerContext}
     * 
     * You should use this map if you need to store attachments on the {@link ChannelHandlerContext}
     * 
     * @param ctx
     * @return attachmentMap
     */
    @SuppressWarnings("unchecked")
    protected final Map<Object,Object> getAttachment(ChannelHandlerContext ctx) {
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
        if (e.getCause() instanceof ReadTimeOutException) {
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
    
    
    @Override
    protected void readTimedOut(ChannelHandlerContext ctx) throws Exception {
        throw new ReadTimeOutException(new SocketTimeoutException("Read timeout")); 
    }


    /**
     * 
     * Exception thrown on a read timeount
     *
     */
    private static class ReadTimeOutException extends RuntimeException {
        private static final long serialVersionUID = 3976736960742503222L;

        public ReadTimeOutException(IOException cause) {
            super(cause);
        }
    }

}
