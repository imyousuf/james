/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
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
package org.apache.james.transport.mailets.listservcommands;


import javax.activation.DataSource;
import java.io.*;

/**
 * MailDataSource implements a typed DataSource from :
 *  an InputStream, a byte array, and a string
 *
 * This is used from {@link BaseCommand#generateMail}
 *
 * @version CVS $Revision: 1.1.2.2 $ $Date: 2003/07/06 11:53:56 $
 * @since 2.2.0
 */
public class MailDataSource implements DataSource {

    protected static final int DEFAULT_BUF_SIZE = 0x2000;

    protected static final String DEFAULT_ENCODING = "iso-8859-1";
    protected static final String DEFAULT_NAME = "HtmlMailDataSource";

    protected byte[] data; // data
    protected String contentType; // content-type

    /**
     * Create a datasource from an input stream
     */
    public MailDataSource(InputStream inputStream, String contentType) {
        this.contentType = contentType;
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            copyStream(inputStream, baos);
            data = baos.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Create a datasource from a byte array
     */
    public MailDataSource(byte[] data, String contentType) {
        this.contentType = contentType;
        this.data = data;
    }

    /**
     * Create a datasource from a String
     */
    public MailDataSource(String data, String contentType) {
        this.contentType = contentType;
        try {
            this.data = data.getBytes(DEFAULT_ENCODING);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    /**
     * returns the inputStream
     */
    public InputStream getInputStream() throws IOException {
        if (data == null)
            throw new IOException("no data");
        return new ByteArrayInputStream(data);
    }

    /**
     * Not implemented
     */
    public OutputStream getOutputStream() throws IOException {
        throw new IOException("getOutputStream() isn't implemented");
    }

    /**
     * returns the contentType for this data source
     */
    public String getContentType() {
        return contentType;
    }

    /**
     * returns a static moniker
     */
    public String getName() {
        return DEFAULT_NAME;
    }

    protected static int copyStream(InputStream inputStream, OutputStream outputStream) throws IOException {
        inputStream = new BufferedInputStream(inputStream);
        outputStream = new BufferedOutputStream(outputStream);

        byte[] bbuf = new byte[DEFAULT_BUF_SIZE];
        int len;
        int totalBytes = 0;
        while ((len = inputStream.read(bbuf)) != -1) {
            outputStream.write(bbuf, 0, len);
            totalBytes += len;
        }
        outputStream.flush();
        return totalBytes;
    }
}

