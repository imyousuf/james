/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.smtpserver;

import java.io.IOException;
import java.io.InputStream;

/** This class wraps an underlying input stream, limiting the allowable size
  * of an incoming MimeMessage. The size limit is configured in the conf file,
  * and when the limit is reached, a MessageSizeException is thrown.
  * @author Matthew Pangaro <mattp@lokitech.com>
  */
public class SizeLimitedInputStream extends InputStream {
    /**
     * Maximum number of bytes to read.
     */
    private long maxmessagesize = 0;
    /**
     * Running total of bytes read from wrapped stream.
     */
    private long bytesread = 0;

    /** InputStream that will be wrapped.
     */
    private InputStream in = null;

    /**
     * Constructor for the stream. Wraps an underlying stream.
     * @param in InputStream to use as basis for new Stream.
     * @param maxmessagesize Message size limit, in Kilobytes
     */
    public SizeLimitedInputStream(InputStream in, long maxmessagesize) {
        this.in = in;
        this.maxmessagesize = maxmessagesize;
    }

    /**
     * Overrides the read method of InputStream to call the read() method of the
     * wrapped input stream.
     * @throws IOException Throws a MessageSizeException, which is a sub-type of IOException
     * @return Returns the number of bytes read.
     */
    public int read(byte[] b, int off, int len) throws IOException {
        int l = in.read(b, off, len);

        bytesread += l;

        if (maxmessagesize > 0 && bytesread > maxmessagesize) {
            throw new MessageSizeException();
        }

        return l;
    }

    /**
     * Overrides the read method of InputStream to call the read() method of the
     * wrapped input stream.
     * @throws IOException Throws a MessageSizeException, which is a sub-type of IOException.
     * @return Returns the int character value of the byte read.
     */
    public int read() throws IOException {
        if (maxmessagesize > 0 && bytesread <= maxmessagesize) {
            bytesread++;
            return in.read();
        } else {
            throw new MessageSizeException();
        }
    }
}
