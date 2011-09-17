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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.FileChannel;

import javax.net.ssl.SSLEngine;

import org.apache.james.pop3server.POP3StreamResponse;
import org.apache.james.protocols.api.ProtocolSession;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.impl.NettyProtocolTransport;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.DefaultFileRegion;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.stream.ChunkedNioFile;
import org.jboss.netty.handler.stream.ChunkedStream;

public class POP3ProtocolTransport extends NettyProtocolTransport{

    private Channel channel;
    private boolean zeroCopy;

    public POP3ProtocolTransport(Channel channel, SSLEngine engine, boolean zeroCopy) {
        super(channel, engine);
        this.channel = channel;
        this.zeroCopy = zeroCopy;
    }

    @Override
    public void writeResponse(Response response, ProtocolSession session) {
        super.writeResponse(response, session);

        if (response instanceof POP3StreamResponse) {

            InputStream stream = ((POP3StreamResponse) response).getStream();
            if (stream != null && channel.isConnected()) {

                if (stream instanceof FileInputStream  && channel.getFactory() instanceof NioServerSocketChannelFactory) {
                    FileChannel fc = ((FileInputStream) stream).getChannel();
                    try {
                        if (zeroCopy) {
                            channel.write(new DefaultFileRegion(fc, fc.position(), fc.size()));

                        } else {
                            channel.write(new ChunkedNioFile(fc, 8192));
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
    }

}
