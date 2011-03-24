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
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

import org.apache.james.imap.main.AbstractImapResponseWriter;
import org.apache.james.imap.message.response.Literal;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

/**
 * {@link AbstractImapResponseWriter} implementation which writes the data to a {@link Channel}
 * 
 *
 */
public class ChannelImapResponseWriter extends AbstractImapResponseWriter{

    private Channel channel;
    private WritableByteChannel wChannel;
    public ChannelImapResponseWriter(Channel channel) {
        this.channel = channel;
        this.wChannel = new ChannelWritableByteChannel(channel);
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.main.AbstractImapResponseWriter#write(java.nio.ByteBuffer)
     */
    protected void write(ByteBuffer buffer) throws IOException {
        ChannelFuture f = channel.write(ChannelBuffers.wrappedBuffer(buffer)).awaitUninterruptibly();
        Throwable t = f.getCause();
        if (t != null) {
            throw new IOException(t);
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.main.AbstractImapResponseWriter#write(org.apache.james.imap.message.response.Literal)
     */
    protected void write(Literal literal) throws IOException {
        literal.writeTo(wChannel);
    }

   
}
