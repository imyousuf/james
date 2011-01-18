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

import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.protocols.impl.ChannelAttributeSupport;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 * {@link FrameDecoder} which will decode via and {@link ImapDecoder} instance
 *
 */
public class ImapRequestFrameDecoder extends FrameDecoder implements ChannelAttributeSupport{

    private ImapDecoder decoder;

    public ImapRequestFrameDecoder(ImapDecoder decoder) {
        this.decoder = decoder;
    }
    
    /*
     * (non-Javadoc)
     * @see org.jboss.netty.handler.codec.frame.FrameDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer)
     */
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        buffer.markReaderIndex();
        
        // check if we failed before and if we already know how much data we need to sucess next run
        Object attachment = ctx.getAttachment();
        if (attachment != null) {
            int size = (Integer) attachment;
            // now see if the buffer hold enough data to process.
            if (size != NettyImapRequestLineReader.NotEnoughDataException.UNKNOWN_SIZE && size > buffer.readableBytes()) {
                buffer.resetReaderIndex();
                
                return null;
            }
        }
        
        try {
            
            ImapRequestLineReader reader = new NettyImapRequestLineReader(channel, buffer);
            ImapMessage message = decoder.decode(reader, (ImapSession) attributes.get(channel));

            // ok no errors found consume the rest of the line
            reader.consumeLine();

            ctx.setAttachment(null);
            return message;
        } catch (NettyImapRequestLineReader.NotEnoughDataException e) {
            // this exception was thrown because we don't have enough data yet 
            int neededData = e.getNeededSize();
            // store the needed data size for later usage
            ctx.setAttachment(neededData);
            buffer.resetReaderIndex();
            return null;
        }
    }

}
