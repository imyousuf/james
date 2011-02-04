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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.james.imap.api.ImapMessage;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.decode.ImapRequestLineReader;
import org.apache.james.protocols.impl.ChannelAttributeSupport;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelStateEvent;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

/**
 * {@link FrameDecoder} which will decode via and {@link ImapDecoder} instance
 *
 */
public class ImapRequestFrameDecoder extends FrameDecoder implements ChannelAttributeSupport{

    private final ImapDecoder decoder;
    private final int inMemorySizeLimit;
    private final static String NEEDED_DATA = "NEEDED_DATA";
    private final static String STORED_DATA = "STORED_DATA";
    private final static String WRITTEN_DATA = "WRITTEN_DATA";

    public ImapRequestFrameDecoder(ImapDecoder decoder, int inMemorySizeLimit) {
        this.decoder = decoder;
        this.inMemorySizeLimit = inMemorySizeLimit;
    }
    
    @Override
    public void channelOpen(ChannelHandlerContext ctx, ChannelStateEvent e) throws Exception {
        ctx.setAttachment(new HashMap<String, Object>());
        super.channelOpen(ctx, e);
    }

    /*
     * (non-Javadoc)
     * @see org.jboss.netty.handler.codec.frame.FrameDecoder#decode(org.jboss.netty.channel.ChannelHandlerContext, org.jboss.netty.channel.Channel, org.jboss.netty.buffer.ChannelBuffer)
     */
    @SuppressWarnings("unchecked")
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer) throws Exception {
        buffer.markReaderIndex();
        boolean retry = false;
       
        ImapRequestLineReader reader;
        // check if we failed before and if we already know how much data we need to sucess next run
        Map<String,Object> attachment = (Map<String, Object>) ctx.getAttachment();
       
        if (attachment.containsKey(NEEDED_DATA)) {
            retry = true;
            int size = (Integer) attachment.get(NEEDED_DATA);
            // now see if the buffer hold enough data to process.
            if (size != NettyImapRequestLineReader.NotEnoughDataException.UNKNOWN_SIZE && size > buffer.readableBytes()) {
                
                // check if we have a inMemorySize limit and if so if the expected size will fit into it
                if (inMemorySizeLimit > 0 && inMemorySizeLimit < size) {
                    
                    // ok seems like it will not fit in the memory limit so we need to store it in a temporary file
                    final File f;
                    int written;
                    
                    // check if we have created a temporary file already or if we need to create a new one
                    if (attachment.containsKey(STORED_DATA)) {
                        f = (File) attachment.get(STORED_DATA);
                        written = (Integer) attachment.get(WRITTEN_DATA);
                    } else {
                        f = File.createTempFile("imap-literal", ".tmp");
                        attachment.put(STORED_DATA, f);
                        written = 0;
                        attachment.put(WRITTEN_DATA, written);

                    }
                    
                   
                    InputStream bufferIn = null; 
                    OutputStream out = null;
                    try {
                        bufferIn = new ChannelBufferInputStream(buffer);
                        out = new FileOutputStream(f, true);
                        
                        // write the needed data to the file
                        int i = -1;
                        while (written < size && (i = bufferIn.read()) != -1) {
                           out.write(i);
                           written++;
                        }
                        
                       
                        
                    } finally {
                       if (bufferIn != null) {
                           bufferIn.close();
                       }
                       if (out != null) {
                           out.close();
                       }
                    }                    
                    // Check if all needed data was streamed to the file. 
                    if (written == size) {
                        reader = new NettyStreamImapRequestLineReader(channel, new FileInputStream(f) {
                            /**
                             * Delete the File on close too
                             */
                            @Override
                            public void close() throws IOException {
                                super.close();
                                f.delete();
                            }
                            
                        }, retry); 
                    } else {
                        attachment.put(WRITTEN_DATA, written);
                        return null;
                    }
                    
                } else {
                    buffer.resetReaderIndex();
                    return null;
                }
                
            } else {
                
                reader = new NettyImapRequestLineReader(channel, buffer, retry);
            }
        } else {
            reader = new NettyImapRequestLineReader(channel, buffer, retry);
        }
        
        try {      
            ImapMessage message = decoder.decode(reader, (ImapSession) attributes.get(channel));

            attachment.clear();
            return message;
        } catch (NettyImapRequestLineReader.NotEnoughDataException e) {
            // this exception was thrown because we don't have enough data yet 
            int neededData = e.getNeededSize();
            // store the needed data size for later usage
            attachment.put(NEEDED_DATA, neededData);
            buffer.resetReaderIndex();
            return null;
        }
    }

}
