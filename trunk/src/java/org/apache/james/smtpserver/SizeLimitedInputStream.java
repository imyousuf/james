/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.smtpserver;

import java.io.IOException;
import java.io.InputStream;

/**
  * Wraps an underlying input stream, limiting the allowable size
  * of incoming data. The size limit is configured in the conf file,
  * and when the limit is reached, a MessageSizeException is thrown.
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

    /**
     * InputStream that will be wrapped.
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
