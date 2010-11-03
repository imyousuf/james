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

package org.apache.james.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.util.stream.CombinedInputStream;

/**
 * Provide an {@link InputStream} over an {@link MimeMessage}
 * 
 */
public class MimeMessageInputStream extends InputStream {
    private InputStream in;

    @SuppressWarnings("unchecked")
    public MimeMessageInputStream(MimeMessage message) throws MessagingException {
        MimeMessage m = message;
       
        // check if we need to use the wrapped message
        if (m instanceof MimeMessageCopyOnWriteProxy) {
            m = ((MimeMessageCopyOnWriteProxy) m).getWrappedMessage();
        }

        // check if we can use optimized operations
        if (m instanceof MimeMessageWrapper) {
            in = ((MimeMessageWrapper) m).getMessageInputStream();
        } else {
            try {
                in = new CombinedInputStream(new InputStream[] { new InternetHeadersInputStream(message.getAllHeaderLines()), message.getRawInputStream() });
            } catch (MessagingException e) {
                // its possible that MimeMessage.getRawInputStream throws an exception when try to access the method on a self constructed MimeMessage.
                // so try to read it in memory 
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try {
                    message.writeTo(out);
                    in = new ByteArrayInputStream(out.toByteArray());
                } catch (IOException e1) {
                    throw new MessagingException("Unable to read message " + message, e);
                }
                
            }
        }

    }

    @Override
    public int read() throws IOException {
        return in.read();
    }

    @Override
    public void close() throws IOException {
       in.close();
    }

    @Override
    public int available() throws IOException {
        return in.available();
    }

    @Override
    public void mark(int readlimit) {
        in.mark(readlimit);
    }

    @Override
    public boolean markSupported() {
        return in.markSupported();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return in.read(b, off, len);
    }

    @Override
    public int read(byte[] b) throws IOException {
        return in.read(b);
    }

    @Override
    public void reset() throws IOException {
        in.reset();
    }

    @Override
    public long skip(long n) throws IOException {
        return in.skip(n);
    }

}
