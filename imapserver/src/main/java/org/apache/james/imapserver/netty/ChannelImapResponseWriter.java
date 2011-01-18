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
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

import org.apache.james.imap.main.AbstractImapResponseWriter;
import org.apache.james.imap.message.response.Literal;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

public class ChannelImapResponseWriter extends AbstractImapResponseWriter{

    private Channel channel;
    public ChannelImapResponseWriter(Channel channel) {
        this.channel = channel;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.main.AbstractImapResponseWriter#write(java.nio.ByteBuffer)
     */
    protected void write(ByteBuffer buffer) throws IOException {
        channel.write(ChannelBuffers.wrappedBuffer(buffer));
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.main.AbstractImapResponseWriter#write(org.apache.james.imap.message.response.Literal)
     */
    protected void write(Literal literal) throws IOException {
        ChannelBuffer buf = ChannelBuffers.buffer((int)literal.size());
        literal.writeTo(Channels.newChannel(new ChannelBufferOutputStream(buf)));
        channel.write(buf);
    }

    
    private final class ChannelWritableByteChannel implements WritableByteChannel {

        public void close() throws IOException {
            // do nothing
        }

        public boolean isOpen() {
            return channel.isOpen();
        }

        /*
         * (non-Javadoc)
         * @see java.nio.channels.WritableByteChannel#write(java.nio.ByteBuffer)
         */
        public int write(ByteBuffer src) throws IOException {
            int size = src.limit();
            channel.write(ChannelBuffers.wrappedBuffer(src));
            return size;
        }
        
    }
}
