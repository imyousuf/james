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

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.main.ImapRequestStreamHandler;
import org.apache.james.imap.main.ImapSessionImpl;
import org.apache.james.protocols.impl.SessionLog;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.stream.StreamHandler;
import org.jboss.netty.util.HashedWheelTimer;

/**
 * {@link StreamHandler} which handles IMAP
 * 
 *
 */
public class ImapStreamChannelUpstreamHandler extends StreamHandler{

    private final Log logger;

    private final String hello;

    private final ImapRequestStreamHandler handler;

    private String[] enabledCipherSuites;

    private SSLContext context;

    private final static String IMAP_SESSION = "IMAP_SESSION"; 
    private final static String BUFFERED_OUT = "BUFFERED_OUT";
    
    public ImapStreamChannelUpstreamHandler(final String hello, final ImapRequestStreamHandler handler, final Log logger, final long readTimeout) {
        this(hello, handler, logger, readTimeout, null, null);
    }
    

    public ImapStreamChannelUpstreamHandler(final String hello, final ImapRequestStreamHandler handler, final Log logger, final long readTimeout, SSLContext context, String[] enabledCipherSuites) {
        super(new HashedWheelTimer(), readTimeout, TimeUnit.SECONDS);
        this.logger = logger;
        this.hello = hello;
        this.handler = handler;
        this.context = context;
        this.enabledCipherSuites = enabledCipherSuites;
    }
    
    private Log getLogger(Channel channel) {
        return new SessionLog(""+channel.getId(), logger);
    }
    @Override
    protected void processStreamIo(final ChannelHandlerContext ctx, final InputStream in, final OutputStream out) {
        final ImapSessionImpl imapSession = (ImapSessionImpl) getAttachment(ctx).get(IMAP_SESSION);
        Channel channel = ctx.getChannel();
        
        // Store the stream as attachment
        OutputStream bufferedOut = new StartTLSOutputStream(out);
        getAttachment(ctx).put(BUFFERED_OUT, bufferedOut);
        
        // handle requests in a loop
        while (channel.isConnected() && handler.handleRequest(in, bufferedOut, imapSession));
        
        if (imapSession != null) imapSession.logout();
        
        getLogger(ctx.getChannel()).debug("Thread execution complete for session " + channel.getId());

        channel.close();
    }
    

    @Override
    public void channelBound(final ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        
        // create the imap session and store it in the IoSession for later usage
        ImapSessionImpl imapsession = new ImapSessionImpl() {

            @Override
            public boolean startTLS() {
                if (supportStartTLS() == false) return false; 
                
                // enable buffering of the stream
                ((StartTLSOutputStream)getAttachment(ctx).get(BUFFERED_OUT)).bufferTillCRLF();

                SslHandler filter = new SslHandler(context.createSSLEngine(), true);
                filter.getEngine().setUseClientMode(false);
                if (enabledCipherSuites != null && enabledCipherSuites.length > 0) {
                    filter.getEngine().setEnabledCipherSuites(enabledCipherSuites);
                }
                ctx.getPipeline().addFirst("sslHandler", filter);

                return true;
            }

            @Override
            public boolean supportStartTLS() {
                 return context != null;
            }
            
        };
        imapsession.setLog(getLogger(ctx.getChannel()));
        
        getAttachment(ctx).put(IMAP_SESSION, imapsession);
        super.channelBound(ctx, e);
    }

    
    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        InetSocketAddress address = (InetSocketAddress) ctx.getChannel().getRemoteAddress();
        getLogger(ctx.getChannel()).info("Connection closed for " + address.getHostName() + " (" + address.getAddress().getHostAddress()+ ")");

        super.channelClosed(ctx, e);
    }


    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        InetSocketAddress address = (InetSocketAddress) ctx.getChannel().getRemoteAddress();
        getLogger(ctx.getChannel()).info("Connection established from " + address.getHostName() + " (" + address.getAddress().getHostAddress()+ ")");

        // write hello to client
        ctx.getChannel().write(ChannelBuffers.copiedBuffer((ImapConstants.UNTAGGED + " OK " + hello +" " + new String(ImapConstants.BYTES_LINE_END)).getBytes()));
        
        super.channelConnected(ctx, e);
        
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        getLogger(ctx.getChannel()).debug("Error while processing imap request" ,e.getCause());
        
        // logout on error not sure if that is the best way to handle it
        final ImapSession imapSession = (ImapSessionImpl) getAttachment(ctx).get(IMAP_SESSION);     
        if (imapSession != null) imapSession.logout();

        // just close the channel now!
        ctx.getChannel().close();
        
        super.exceptionCaught(ctx, e);
    }

    /**
     * Because Netty {@link SslHandler} need to NOT encrypt the first response send to client this {@link FilterOutputStream} is needed. It
     * buffer the data till the complete response was written to the stream (searching for the CRLF). 
     * 
     * Once this was done it just pass the data to the wrapped {@link OutputStream} without doing any more buffering
     *
     */
    private final class StartTLSOutputStream extends FilterOutputStream {
        private int lastChar;
        private boolean bufferData = false;
        private final ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        
        public StartTLSOutputStream(OutputStream out) {
            super(out);   
        }
        
        /**
         * Buffer the data till the next CLRF was found
         */
        public synchronized final void bufferTillCRLF() {
            bufferData = true;
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            if (bufferData) {
                for (int i = off; i < len; i++) {
                    write(b[i]);
                }
            } else {
                out.write(b, off, len);
            }
        }

        @Override
        public void write(byte[] b) throws IOException {
            write(b, 0, b.length);
        }

        @Override
        public synchronized void write(int b) throws IOException {
            if (bufferData) {
                buffer.writeByte((byte)b);
                // check for CLRF and if found write the data and disable buffering
                if (b == '\n' && lastChar == '\r') {
                    byte[] line = new byte[buffer.capacity()];
                    buffer.getBytes(0, line);
                    out.write(line);
                    bufferData = false;
                }
                lastChar = b;

            } else {
                out.write(b);
            }
        }
        
        
    }
}
