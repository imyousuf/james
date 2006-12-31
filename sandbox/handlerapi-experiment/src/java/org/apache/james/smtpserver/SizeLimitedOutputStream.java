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



package org.apache.james.smtpserver;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/** 
  * Wraps an underlying input stream, limiting the allowable size
  * of incoming data. The size limit is configured in the conf file,
  * and when the limit is reached, a MessageSizeException is thrown.
  */
public class SizeLimitedOutputStream extends FilterOutputStream {
    /**
     * Maximum number of bytes to read.
     */
    private long maxmessagesize = 0;
    /**
     * Running total of bytes written to the wrapped stream.
     */
    private long byteswritten = 0;

    /**
     * Constructor for the stream. Wraps an underlying stream.
     * @param in InputStream to use as basis for new Stream.
     * @param maxmessagesize Message size limit, in Kilobytes
     */
    public SizeLimitedOutputStream(OutputStream out, long maxmessagesize) {
        super(out);
        this.maxmessagesize = maxmessagesize;
    }

    /**
     * @see java.io.FilterOutputStream#write(byte[], int, int)
     */
    public void write(byte[] b, int off, int len) throws IOException {
        byteswritten+=len;
        if (maxmessagesize > 0 && byteswritten > maxmessagesize) {
            throw new MessageSizeException();
        }
        out.write(b, off, len);
    }

    /**
     * @see java.io.FilterOutputStream#write(int)
     */
    public void write(int b) throws IOException {
        byteswritten++;
        if (maxmessagesize > 0 && byteswritten > maxmessagesize) {
            throw new MessageSizeException();
        }
        out.write(b);
    }

}
