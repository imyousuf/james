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

package org.apache.james.socket.netty;

import java.util.LinkedList;
import java.util.List;

import org.apache.james.protocols.api.ConnectHandler;
import org.apache.james.protocols.api.LineHandler;
import org.apache.james.protocols.api.ProtocolHandlerChain;
import org.apache.james.protocols.api.ProtocolSession;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipelineCoverage;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.channel.ChannelUpstreamHandler;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;

/**
 * This abstract {@link ChannelUpstreamHandler} handling the calling of ConnectHandler and LineHandlers
 * on the right events.
 * 
 *
 */
@ChannelPipelineCoverage("all")
public abstract class AbstractChannelUpstreamHandler extends SimpleChannelUpstreamHandler implements ChannelAttributeSupport{
    
    private ProtocolHandlerChain chain;

    public AbstractChannelUpstreamHandler(ProtocolHandlerChain chain) {
        this.chain = chain;
    }


    @Override
    public void channelBound(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        attributes.set(ctx.getChannel(),createSession(ctx));
        super.channelBound(ctx, e);
    }



    @SuppressWarnings("unchecked")
    @Override
    public void channelConnected(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        List<ConnectHandler> connectHandlers = chain.getHandlers(ConnectHandler.class);

        if (connectHandlers != null) {
            for (int i = 0; i < connectHandlers.size(); i++) {
                connectHandlers.get(i).onConnect((ProtocolSession) attributes.get(ctx.getChannel()));
            }
        }
        super.channelConnected(ctx, e);
    }



    @SuppressWarnings("unchecked")
    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        ProtocolSession pSession = (ProtocolSession) attributes.get(ctx.getChannel());
        LinkedList<LineHandler> lineHandlers = chain.getHandlers(LineHandler.class);
        
        ChannelBuffer buf = (ChannelBuffer) e.getMessage();      
        byte[] line = new byte[buf.capacity()];
        buf.getBytes(0, line);
        
        if (lineHandlers.size() > 0) {
            
            // Maybe it would be better to use the ByteBuffer here
            ((LineHandler) lineHandlers.getLast()).onLine(pSession,line);
        }
        
        super.messageReceived(ctx, e);
    }


    @Override
    public void channelClosed(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        cleanup(ctx.getChannel());
        
        super.channelClosed(ctx, e);
    }


    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e) throws Exception {
        if ((e.getCause() instanceof TooLongFrameException) == false) {
            cleanup(ctx.getChannel());
        }
    }

    private void cleanup(Channel channel) {
        ProtocolSession session = (ProtocolSession) attributes.get(channel);
        if (session != null) {
            session.resetState();
            session = null;
        }
        attributes.remove(channel);

    }

    /**
     * Create a new "protocol" session 
     * 
     * @param session ioSession
     * @return ctx
     * @throws Exception
     */
    protected abstract ProtocolSession createSession(ChannelHandlerContext ctx) throws Exception;


}
