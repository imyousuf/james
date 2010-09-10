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
import java.util.Enumeration;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

/**
 * {@link InputStream} which contains the headers and the Body of the
 * wrapped {@link MimeMessage}
 * 
 */
public class MimeMessageInputStream extends InputStream {
    private final InputStream headersInputStream;
    private final InputStream bodyInputStream;
    private int cStream = 0;

    boolean nextCR = false;
    boolean nextLF = false;

    @SuppressWarnings("unchecked")
    public MimeMessageInputStream(MimeMessage message) throws IOException {
        try {
            ByteArrayOutputStream headersOut = new ByteArrayOutputStream();
            Enumeration headers = message.getAllHeaderLines();
            while (headers.hasMoreElements()) {
                headersOut.write(headers.nextElement().toString().getBytes("US-ASCII"));
                headersOut.write("\r\n".getBytes());
            }
            headersInputStream = new ByteArrayInputStream(headersOut.toByteArray());
            
            // use the raw InputStream because we want to have no conversion here and just obtain the original message body
            this.bodyInputStream = message.getRawInputStream();
        } catch (MessagingException e) {
            throw new IOException("Unable to read MimeMessage: " + e.getMessage());
        }
    }

    @Override
    public int read() throws IOException {
        if (nextCR) {
            nextCR = false;
            nextLF = true;
            return '\r';
        } else if (nextLF) {
            nextLF = false;
            return '\n';
        } else {
            int i = -1;
            if (cStream == 0) {
                i = headersInputStream.read();
            } else {
                i = bodyInputStream.read();
            }

            if (i == -1 && cStream == 0) {
                cStream++;
                nextCR = true;
                return read();
            }
            return i;
        }

    }

    /** Closes all streams */
    public void close() throws IOException {
        headersInputStream.close();
        bodyInputStream.close();
    }

    /** Is there more data to read */
    public int available() throws IOException {
        if (cStream == 0) {
            return headersInputStream.available() + bodyInputStream.available() + 2;
        } else {
            return bodyInputStream.available();
        }
    }

}
