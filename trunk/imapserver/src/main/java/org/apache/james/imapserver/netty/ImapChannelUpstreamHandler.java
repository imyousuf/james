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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.main.ImapRequestStreamHandler;
import org.apache.james.imap.main.ImapSessionImpl;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.stream.StreamHandler;
import org.jboss.netty.util.HashedWheelTimer;

/**
 * {@link StreamHandler} which handles IMAP
 * 
 *
 */
public class ImapChannelUpstreamHandler extends StreamHandler{

    private final Log logger;

    private final String hello;

    private final ImapRequestStreamHandler handler;

    private final static String IMAP_SESSION = "IMAP_SESSION"; 
    
    public ImapChannelUpstreamHandler(final String hello, final ImapRequestStreamHandler handler, final Log logger, final long readTimeout) {
        super(new HashedWheelTimer(), readTimeout, TimeUnit.SECONDS);
        this.logger = logger;
        this.hello = hello;
        this.handler = handler;
    }
    
    @Override
    protected void processStreamIo(final ChannelHandlerContext ctx, final InputStream in, final OutputStream out) {
        final ImapSessionImpl imapSession = (ImapSessionImpl) getAttachment(ctx).get(IMAP_SESSION);

        // handle requests in a loop
        while (handler.handleRequest(in, out, imapSession));
        if (imapSession != null) imapSession.logout();
        
        Channel channel = ctx.getChannel();
        logger.debug("Thread execution complete for session " + channel.getId());

        channel.close();
    }
    

    @Override
    public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        
        // create the imap session and store it in the IoSession for later usage
        final ImapSessionImpl imapsession = new ImapSessionImpl();
        imapsession.setLog(logger);
        
        getAttachment(ctx).put(IMAP_SESSION, imapsession);
        super.channelBound(ctx, e);
    }

    
    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        // write hello to client
        ctx.getChannel().write(ChannelBuffers.copiedBuffer((ImapConstants.UNTAGGED + " OK " + hello +" " + new String(ImapConstants.BYTES_LINE_END)).getBytes()));
        
        super.channelConnected(ctx, e);
        
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        logger.debug("Error while processing imap request" ,e.getCause());
        
        // logout on error not sure if that is the best way to handle it
        final ImapSessionImpl imapSession = (ImapSessionImpl) getAttachment(ctx).get(IMAP_SESSION);     
        if (imapSession != null) imapSession.logout();

        // just close the channel now!
        ctx.getChannel().close();
        
        super.exceptionCaught(ctx, e);
    }

    
}
