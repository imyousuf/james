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

package org.apache.james.imapserver.processor.imap4rev1.fetch;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;

/**
 * Filters a {@link WritableByteChannel} 
 * to supply a limited byte range
 */
class PartialWritableByteChannel implements WritableByteChannel {

    private final WritableByteChannel delegate;
    private final long firstOctet;
    private final long numberOfOctets;
    private long bytesWritten;
    
    public PartialWritableByteChannel(final WritableByteChannel delegate, final long firstOctet, final long numberOfOctets) {
        super();
        this.delegate = delegate;
        this.firstOctet = firstOctet;
        this.numberOfOctets = numberOfOctets;
        bytesWritten = 0;
    }

    public int write(ByteBuffer src) throws IOException {
        final int result;
        final long bytesToIgnore = firstOctet - bytesWritten;
        if (bytesToIgnore > 0) {
            final int remaining = src.remaining();
            if (remaining <= bytesToIgnore) {
                result = ignoreRemaining(src);
            } else {
                final int remainingBytesToIgnore = (int) bytesToIgnore;
                src.position(src.position() + remainingBytesToIgnore);
                result = writeRemaining(src, numberOfOctets) + remainingBytesToIgnore;
            }
        } else {
            final long bytesToWrite = numberOfOctets - bytesWritten + firstOctet;
            result = writeRemaining(src, bytesToWrite);
        }
        bytesWritten += result;
        return result;
    }

    private int writeRemaining(ByteBuffer src, final long bytesToWrite) throws IOException {
        final int remaining = src.remaining();
        final int result;
        if (bytesToWrite <= 0) {
            result = ignoreRemaining(src);
        } else if (remaining < bytesToWrite ) {
            result = delegate.write(src);
        } else {
            final ByteBuffer slice = src.asReadOnlyBuffer();
            slice.limit(slice.position() + (int) bytesToWrite);
            delegate.write(slice);
            result = ignoreRemaining(src);
        }
        return result;
    }

    private int ignoreRemaining(ByteBuffer src) {
        final int result;
        result = src.remaining();
        src.position(src.limit());
        return result;
    }

    public void close() throws IOException {
        delegate.close();
    }

    public boolean isOpen() {
        return delegate.isOpen();
    }

}
