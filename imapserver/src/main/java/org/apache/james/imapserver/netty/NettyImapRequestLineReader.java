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

import org.apache.james.imap.decode.DecodingException;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;

public class NettyImapRequestLineReader extends ImapRequestLineReader{

    private ChannelBuffer buffer;
    private Channel channel;
    private ChannelBuffer cRequest = ChannelBuffers.wrappedBuffer("+\r\n".getBytes());

    public NettyImapRequestLineReader(Channel channel, ChannelBuffer buffer) {
        this.buffer = buffer;
        this.channel = channel;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.decode.ImapRequestLineReader#nextChar()
     */
    public char nextChar() throws DecodingException {
        if (!nextSeen) {
            int next = -1;

            try {
                next = buffer.readByte();
            } catch (IndexOutOfBoundsException e) {
                throw new NotEnoughDataException();
            }

            nextSeen = true;
            nextChar = (char) next;
        }
        return nextChar;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.decode.ImapRequestLineReader#read(int)
     */
    public InputStream read(int size) throws DecodingException {
        if (size > buffer.readableBytes()) {
            throw new NotEnoughDataException(size);
        }
        return new ChannelBufferInputStream(buffer, size);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.imap.decode.ImapRequestLineReader#commandContinuationRequest()
     */
    protected void commandContinuationRequest() throws DecodingException {
        channel.write(cRequest);
    }
}
