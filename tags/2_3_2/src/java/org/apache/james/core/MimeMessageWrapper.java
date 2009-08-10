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

import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.james.util.InternetPrintWriter;
import org.apache.james.util.io.IOUtil;

import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetHeaders;
import javax.mail.internet.MimeMessage;
import javax.mail.util.SharedByteArrayInputStream;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Enumeration;

/**
 * This object wraps a MimeMessage, only loading the underlying MimeMessage
 * object when needed.  Also tracks if changes were made to reduce
 * unnecessary saves.
 */
public class MimeMessageWrapper
    extends MimeMessage
    implements Disposable {

    /**
     * Can provide an input stream to the data
     */
    protected MimeMessageSource source = null;
    
    /**
     * This is false until we parse the message 
     */
    protected boolean messageParsed = false;
    
    /**
     * This is false until we parse the message 
     */
    protected boolean headersModified = false;
    
    /**
     * This is false until we parse the message 
     */
    protected boolean bodyModified = false;

    /**
     * Keep a reference to the sourceIn so we can close it
     * only when we dispose the message.
     */
    private InputStream sourceIn;

    private MimeMessageWrapper(Session session) throws MessagingException {
        super(session);
        this.headers = null;
        this.modified = false;
        this.headersModified = false;
        this.bodyModified = false;
    }
    
    /**
     * A constructor that instantiates a MimeMessageWrapper based on
     * a MimeMessageSource
     *
     * @param source the MimeMessageSource
     * @throws MessagingException 
     */
    public MimeMessageWrapper(Session session, MimeMessageSource source) throws MessagingException {
        this(session);
        this.source = source;
    }

    /**
     * A constructor that instantiates a MimeMessageWrapper based on
     * a MimeMessageSource
     *
     * @param source the MimeMessageSource
     * @throws MessagingException 
     * @throws MessagingException 
     */
    public MimeMessageWrapper(MimeMessageSource source) throws MessagingException {
        this(Session.getDefaultInstance(System.getProperties()),source);
    }

    public MimeMessageWrapper(MimeMessage original) throws MessagingException {
        this(Session.getDefaultInstance(System.getProperties()));
        flags = original.getFlags();
        
        // if the original is an unmodified MimeMessageWrapped we clone the headers and
        // take its source.
        /* Temporary commented out because of JAMES-474
        if (original instanceof MimeMessageWrapper && !((MimeMessageWrapper) original).bodyModified) {
            source = ((MimeMessageWrapper) original).source;
            // this probably speed up things
            if (((MimeMessageWrapper) original).headers != null) {
                ByteArrayOutputStream temp = new ByteArrayOutputStream();
                InternetHeaders ih = ((MimeMessageWrapper) original).headers;
                MimeMessageUtil.writeHeadersTo(ih.getAllHeaderLines(),temp);
                headers = createInternetHeaders(new ByteArrayInputStream(temp.toByteArray()));
                headersModified = ((MimeMessageWrapper) original).headersModified;
            }
        }
        */
        
        if (source == null) {
            ByteArrayOutputStream bos;
            int size = original.getSize();
            if (size > 0)
                bos = new ByteArrayOutputStream(size);
            else
                bos = new ByteArrayOutputStream();
            try {
                original.writeTo(bos);
                bos.close();
                SharedByteArrayInputStream bis =
                        new SharedByteArrayInputStream(bos.toByteArray());
                parse(bis);
                bis.close();
                saved = true;
            } catch (IOException ex) {
                // should never happen, but just in case...
                throw new MessagingException("IOException while copying message",
                                ex);
            }
        }
    }
    
    /**
     * Returns the source ID of the MimeMessageSource that is supplying this
     * with data.
     * @see MimeMessageSource
     */
    public synchronized String getSourceId() {
        return source != null ? source.getSourceId() : null;
    }

    /**
     * Load the message headers from the internal source.
     *
     * @throws MessagingException if an error is encountered while
     *                            loading the headers
     */
    protected synchronized void loadHeaders() throws MessagingException {
        if (headers != null) {
            //Another thread has already loaded these headers
            return;
        } else if (source != null) { 
            try {
                InputStream in = source.getInputStream();
                try {
                    headers = createInternetHeaders(in);
                } finally {
                    IOUtil.shutdownStream(in);
                }
            } catch (IOException ioe) {
                throw new MessagingException("Unable to parse headers from stream: " + ioe.getMessage(), ioe);
            }
        } else {
            throw new MessagingException("loadHeaders called for a message with no source, contentStream or stream");
        }
    }

    /**
     * Load the complete MimeMessage from the internal source.
     *
     * @throws MessagingException if an error is encountered while
     *                            loading the message
     */
    protected synchronized void loadMessage() throws MessagingException {
        if (messageParsed) {
            //Another thread has already loaded this message
            return;
        } else if (source != null) {
            sourceIn = null;
            try {
                sourceIn = source.getInputStream();
    
                parse(sourceIn);
                // TODO is it ok?
                saved = true;
                
            } catch (IOException ioe) {
                IOUtil.shutdownStream(sourceIn);
                sourceIn = null;
                throw new MessagingException("Unable to parse stream: " + ioe.getMessage(), ioe);
            }
        } else {
            throw new MessagingException("loadHeaders called for an unparsed message with no source");
        }
    }

    /**
     * Get whether the message has been modified.
     *
     * @return whether the message has been modified
     */
    public synchronized boolean isModified() {
        return headersModified || bodyModified || modified;
    }

    /**
     * Rewritten for optimization purposes
     */
    public synchronized void writeTo(OutputStream os) throws IOException, MessagingException {
        if (source != null && !isModified()) {
            // We do not want to instantiate the message... just read from source
            // and write to this outputstream
            InputStream in = source.getInputStream();
            try {
                MimeMessageUtil.copyStream(in, os);
            } finally {
                IOUtil.shutdownStream(in);
            }
        } else {
            writeTo(os, os);
        }
    }

    /**
     * Rewritten for optimization purposes
     */
    public void writeTo(OutputStream os, String[] ignoreList) throws IOException, MessagingException {
        writeTo(os, os, ignoreList);
    }

    /**
     * Write
     */
    public void writeTo(OutputStream headerOs, OutputStream bodyOs) throws IOException, MessagingException {
        writeTo(headerOs, bodyOs, new String[0]);
    }

    public synchronized void writeTo(OutputStream headerOs, OutputStream bodyOs, String[] ignoreList) throws IOException, MessagingException {
        if (source != null && !isModified()) {
            //We do not want to instantiate the message... just read from source
            //  and write to this outputstream

            //First handle the headers
            InputStream in = source.getInputStream();
            try {
                InternetHeaders headers = new InternetHeaders(in);
                PrintWriter pos = new InternetPrintWriter(new BufferedWriter(new OutputStreamWriter(headerOs), 512), true);
                for (Enumeration e = headers.getNonMatchingHeaderLines(ignoreList); e.hasMoreElements(); ) {
                    String header = (String)e.nextElement();
                    pos.println(header);
                }
                pos.println();
                pos.flush();
                MimeMessageUtil.copyStream(in, bodyOs);
            } finally {
                IOUtil.shutdownStream(in);
            }
        } else {
            MimeMessageUtil.writeToInternal(this, headerOs, bodyOs, ignoreList);
        }
    }

    /**
     * This is the MimeMessage implementation - this should return ONLY the
     * body, not the entire message (should not count headers).  Will have
     * to parse the message.
     */
    public int getSize() throws MessagingException {
        if (!messageParsed) {
            loadMessage();
        }
        return super.getSize();
    }

    /**
     * Corrects JavaMail 1.1 version which always returns -1.
     * Only corrected for content less than 5000 bytes,
     * to avoid memory hogging.
     */
    public int getLineCount() throws MessagingException {
            InputStream in=null;
        try{
            in = getContentStream();
        }catch(Exception e){
            return -1;
        }
        if (in == null) {
            return -1;
        }
        //Wrap input stream in LineNumberReader
        //Not sure what encoding to use really...
        try {
            LineNumberReader counter;
            if (getEncoding() != null) {
                counter = new LineNumberReader(new InputStreamReader(in, getEncoding()));
            } else {
                counter = new LineNumberReader(new InputStreamReader(in));
            }
            //Read through all the data
            char[] block = new char[4096];
            while (counter.read(block) > -1) {
                //Just keep reading
            }
            return counter.getLineNumber();
        } catch (IOException ioe) {
            return -1;
        } finally {
            IOUtil.shutdownStream(in);
        }
    }

    /**
     * Returns size of message, ie headers and content
     */
    public long getMessageSize() throws MessagingException {
        if (source != null && !isModified()) {
            try {
                return source.getMessageSize();
            } catch (IOException ioe) {
                throw new MessagingException("Error retrieving message size", ioe);
            }
        } else {
            return MimeMessageUtil.calculateMessageSize(this);
        }
    }
    
    /**
     * We override all the "headers" access methods to be sure that we
     * loaded the headers 
     */
    
    public String[] getHeader(String name) throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getHeader(name);
    }

    public String getHeader(String name, String delimiter) throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getHeader(name, delimiter);
    }

    public Enumeration getAllHeaders() throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getAllHeaders();
    }

    public Enumeration getMatchingHeaders(String[] names) throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getMatchingHeaders(names);
    }

    public Enumeration getNonMatchingHeaders(String[] names) throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getNonMatchingHeaders(names);
    }

    public Enumeration getAllHeaderLines() throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getAllHeaderLines();
    }

    public Enumeration getMatchingHeaderLines(String[] names) throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getMatchingHeaderLines(names);
    }

    public Enumeration getNonMatchingHeaderLines(String[] names) throws MessagingException {
        if (headers == null) {
            loadHeaders();
        }
        return headers.getNonMatchingHeaderLines(names);
    }


    private synchronized void checkModifyHeaders() throws MessagingException {
        // Disable only-header loading optimizations for JAMES-559
        if (!messageParsed) {
            loadMessage();
        }
        // End JAMES-559
        if (headers == null) {
            loadHeaders();
        }
        modified = true;
        saved = false;
        headersModified = true;
    }

    public void setHeader(String name, String value) throws MessagingException {
        checkModifyHeaders();
        super.setHeader(name, value);
    }

    public void addHeader(String name, String value) throws MessagingException {
        checkModifyHeaders();
        super.addHeader(name, value);
    }

    public void removeHeader(String name) throws MessagingException {
        checkModifyHeaders();
        super.removeHeader(name);
    }

    public void addHeaderLine(String line) throws MessagingException {
        checkModifyHeaders();
        super.addHeaderLine(line);
    }


    /**
     * The message is changed when working with headers and when altering the content.
     * Every method that alter the content will fallback to this one.
     * 
     * @see javax.mail.Part#setDataHandler(javax.activation.DataHandler)
     */
    public synchronized void setDataHandler(DataHandler arg0) throws MessagingException {
        modified = true;
        saved = false;
        bodyModified = true;
        super.setDataHandler(arg0);
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose() {
        if (sourceIn != null) {
            IOUtil.shutdownStream(sourceIn);
        }
        if (source != null) {
            ContainerUtil.dispose(source);
        }
    }

    /**
     * @see javax.mail.internet.MimeMessage#parse(java.io.InputStream)
     */
    protected synchronized void parse(InputStream is) throws MessagingException {
        // the super implementation calls
        // headers = createInternetHeaders(is);
        super.parse(is);
        messageParsed = true;
    }

    /**
     * If we already parsed the headers then we simply return the updated ones.
     * Otherwise we parse
     * 
     * @see javax.mail.internet.MimeMessage#createInternetHeaders(java.io.InputStream)
     */
    protected synchronized InternetHeaders createInternetHeaders(InputStream is) throws MessagingException {
        /* This code is no more needed: see JAMES-570 and new tests
           
         * InternetHeaders can be a bit awkward to work with due to
         * its own internal handling of header order.  This hack may
         * not always be necessary, but for now we are trying to
         * ensure that there is a Return-Path header, even if just a
         * placeholder, so that later, e.g., in LocalDelivery, when we
         * call setHeader, it will remove any other Return-Path
         * headers, and ensure that ours is on the top. addHeader
         * handles header order, but not setHeader. This may change in
         * future JavaMail.  But if there are other Return-Path header
         * values, let's drop our placeholder.

        MailHeaders newHeaders = new MailHeaders(new ByteArrayInputStream((RFC2822Headers.RETURN_PATH + ": placeholder").getBytes()));
        newHeaders.setHeader(RFC2822Headers.RETURN_PATH, null);
        newHeaders.load(is);
        String[] returnPathHeaders = newHeaders.getHeader(RFC2822Headers.RETURN_PATH);
        if (returnPathHeaders.length > 1) newHeaders.setHeader(RFC2822Headers.RETURN_PATH, returnPathHeaders[1]);
        */
        
        // Keep this: skip the headers from the stream
        // we could put that code in the else and simple write an "header" skipping
        // reader for the others.
        MailHeaders newHeaders = new MailHeaders(is);
        
        if (headers != null) {
            return headers;
        } else {
            return newHeaders;
        }
    }

    /**
     * @see javax.mail.internet.MimeMessage#getContentStream()
     */
    protected InputStream getContentStream() throws MessagingException {
        if (!messageParsed) {
            loadMessage();
        }
        return super.getContentStream();
    }

    /**
     * @see javax.mail.internet.MimeMessage#getRawInputStream()
     */
    public InputStream getRawInputStream() throws MessagingException {
        if (!messageParsed && !isModified() && source != null) {
            InputStream is;
            try {
                is = source.getInputStream();
                // skip the headers.
                new MailHeaders(is);
                return is;
            } catch (IOException e) {
                throw new MessagingException("Unable to read the stream: " + e.getMessage(), e);
            }
        } else return super.getRawInputStream();
    }

    /**
     * <p>Overrides standard implementation to ensure JavaMail works 
     * appropriately for an email server.
     * Note that MessageID now needs to be explicitly set on 
     * different cloned instances.</p>
     * <p>See <a href='https://issues.apache.org/jira/browse/JAMES-875'>JAMES-875</a></p>
     * @see javax.mail.internet.MimeMessage#updateMessageID()
     */
    protected void updateMessageID() throws MessagingException {
        if (getMessageID() == null) super.updateMessageID();
    }
}
