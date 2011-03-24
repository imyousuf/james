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

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

/**
 * Some adapter class which allows to write to {@link Channel} via a {@link WritableByteChannel} interface
 * 
 *
 */
public class ChannelWritableByteChannel implements WritableByteChannel {

    private final Channel channel;

    public ChannelWritableByteChannel(Channel channel) {
        this.channel = channel;
    }
    
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
        if (src.remaining() == 0) return 0;
        byte data[] = new byte[src.remaining()];
        src.get(data);
        
        ChannelFuture future = channel.write(ChannelBuffers.wrappedBuffer(data)).awaitUninterruptibly();
        Throwable t = future.getCause();
        if (t != null) {
            throw new IOException(t);
        }
        return data.length;
    }
   

}
