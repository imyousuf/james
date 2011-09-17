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

import static org.jboss.netty.buffer.ChannelBuffers.copiedBuffer;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3StreamResponse;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelEvent;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelDownstreamHandler;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.stream.ChunkedNioFile;
import org.jboss.netty.handler.stream.ChunkedStream;

public class POP3ResponseEncoder extends SimpleChannelDownstreamHandler{
   
    private boolean zeroCopy;
    private Charset charset = Charset.forName("US-ASCII");

    public POP3ResponseEncoder(boolean zeroCopy) {
        this.zeroCopy = zeroCopy;
    }
    
    
    @Override
    public void handleDownstream(ChannelHandlerContext ctx, ChannelEvent evt) throws Exception {
        if (evt instanceof MessageEvent) {
            MessageEvent e = (MessageEvent) evt;
            Object originalMessage = e.getMessage();
            Channel channel = ctx.getChannel();

            if (originalMessage instanceof POP3Response) {
                    StringBuilder builder = new StringBuilder();
                    POP3Response response = (POP3Response) originalMessage;
                    List<CharSequence> lines = response.getLines();
                    for (int i = 0; i < lines.size(); i++) {
                        builder.append(lines.get(i));
                        if (i < lines.size()) {
                            builder.append("\r\n");
                        }
                    }
                    channel.write(copiedBuffer(builder.toString(), charset));                
            }
            if (originalMessage instanceof POP3StreamResponse) {

                InputStream stream = ((POP3StreamResponse) originalMessage).getStream();
                if (stream != null && channel.isConnected()) {

                    if (stream instanceof FileInputStream  && channel.getFactory() instanceof NioServerSocketChannelFactory) {
                        FileChannel fc = ((FileInputStream) stream).getChannel();
                        try {
                            if (zeroCopy) {
                                channel.write(new DefaultFileRegion(fc, fc.position(), fc.size()));

                            } else {
                                channel.write(new ChunkedNioFile(fc, 8192), e.getRemoteAddress());
                            }
                        } catch (IOException ex) {
                            // Catch the exception and just pass it so we get the exception later
                            channel.write(new ChunkedStream(stream));

                        }
                    } else {
                        channel.write(new ChunkedStream(stream));

                    }
                }
                channel.write(ChannelBuffers.wrappedBuffer(".\r\n".getBytes()));
            }
        } else {
            super.handleDownstream(ctx, evt);
        }
    }

}
