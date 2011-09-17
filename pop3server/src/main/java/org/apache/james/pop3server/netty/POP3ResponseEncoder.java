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
package org.apache.james.pop3server.netty;

import static org.jboss.netty.channel.Channels.write;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;

import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3StreamResponse;
import org.apache.james.protocols.impl.ResponseEncoder;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.stream.ChunkedNioFile;
import org.jboss.netty.handler.stream.ChunkedStream;

public class POP3ResponseEncoder extends ResponseEncoder{
   
    private boolean zeroCopy;


    public POP3ResponseEncoder(boolean zeroCopy) {
        super(POP3Response.class, Charset.forName("US-ASCII"));
        this.zeroCopy = zeroCopy;
    }
    
    
    @Override
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {
        if (evt instanceof MessageEvent) {
            MessageEvent e = (MessageEvent) evt;
            Object originalMessage = e.getMessage();

            super.handleDownstream(ctx, evt);
            
            if (originalMessage instanceof POP3StreamResponse) {

                InputStream stream = ((POP3StreamResponse) originalMessage).getStream();
                Channel channel = ctx.getChannel();
                if (stream != null && channel.isConnected()) {

                    if (stream instanceof FileInputStream  && channel.getFactory() instanceof NioServerSocketChannelFactory) {
                        FileChannel fc = ((FileInputStream) stream).getChannel();
                        try {
                            if (zeroCopy) {
                                write(ctx, e.getFuture(), new DefaultFileRegion(fc, fc.position(), fc.size()), e.getRemoteAddress());

                            } else {
                                write(ctx, e.getFuture(), new ChunkedNioFile(fc, 8192), e.getRemoteAddress());
                            }
                        } catch (IOException ex) {
                            // Catch the exception and just pass it so we get the exception later
                            write(ctx, e.getFuture(), new ChunkedStream(stream), e.getRemoteAddress());

                        }
                    } else {
                        write(ctx, e.getFuture(), new ChunkedStream(stream), e.getRemoteAddress());

                    }
                }
                channel.write(ChannelBuffers.wrappedBuffer(".\r\n".getBytes()));
            }
        } else {
            super.handleDownstream(ctx, evt);
        }
    }

}
