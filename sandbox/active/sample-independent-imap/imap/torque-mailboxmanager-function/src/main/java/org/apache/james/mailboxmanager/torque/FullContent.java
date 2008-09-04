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

/**
 * 
 */
package org.apache.james.mailboxmanager.torque;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.Iterator;
import java.util.List;

import org.apache.james.mailboxmanager.MessageResult;

final class FullContent implements MessageResult.Content {
    private final byte[] contents;
    private final List headers;
    private final long size;
    
    public FullContent(final byte[] contents, final List headers) {
        this.contents =  contents;
        this.headers = headers;
        this.size = caculateSize();
    }

    private long caculateSize() {
        long result = contents.length + MessageUtils.countUnnormalLines(contents);
        result += 2;
        for (final Iterator it=headers.iterator(); it.hasNext();) {
            final MessageResult.Header header = (MessageResult.Header) it.next();
            if (header != null) {
                result += header.size();
                result += 2;
            }
        }
        return result;
    }

    public void writeTo(StringBuffer buffer) {
        for (final Iterator it=headers.iterator(); it.hasNext();) {
            final MessageResult.Header header = (MessageResult.Header) it.next();
            if (header != null) {
                header.writeTo(buffer);
            }
            buffer.append('\r');
            buffer.append('\n');
        }
        buffer.append('\r');
        buffer.append('\n');
        MessageUtils.normalisedWriteTo(contents, buffer);
    }

    public long size() {
        return size;
    }

    public void writeTo(WritableByteChannel channel) throws IOException {
        ByteBuffer newLine = ByteBuffer.wrap(MessageRowUtils.BYTES_NEW_LINE);
        for (final Iterator it=headers.iterator(); it.hasNext();) {
            final MessageResult.Header header = (MessageResult.Header) it.next();
            if (header != null) {
                header.writeTo(channel);
            }
            newLine.rewind();
            writeAll(channel, newLine);
        }
        newLine.rewind();
        writeAll(channel, newLine);
        final ByteBuffer wrap = ByteBuffer.wrap(contents);
        writeAll(channel, wrap);
    }

    private void writeAll(WritableByteChannel channel, ByteBuffer buffer) throws IOException {
        while (channel.write(buffer) > 0) {
            // write more
        }
    }
}