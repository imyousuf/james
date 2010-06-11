/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.transport.mailets.listservcommands;


import javax.activation.DataSource;
import java.io.*;

/**
 * MailDataSource implements a typed DataSource from :
 *  an InputStream, a byte array, and a string
 *
 * This is used from {@link BaseCommand#generateMail}
 *
 * @version CVS $Revision$ $Date$
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
    public MailDataSource(InputStream inputStream, String contentType) throws IOException {
        this.contentType = contentType;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        copyStream(inputStream, baos);
        data = baos.toByteArray();
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
    public MailDataSource(String data, String contentType) throws UnsupportedEncodingException {
        this.contentType = contentType;
        this.data = data.getBytes(DEFAULT_ENCODING);
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

