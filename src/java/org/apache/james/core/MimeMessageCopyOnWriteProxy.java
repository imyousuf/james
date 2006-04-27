/***********************************************************************
 * Copyright (c) 2000-2006 The Apache Software Foundation.             *
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

package org.apache.james.core;

import org.apache.avalon.framework.activity.Disposable;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Enumeration;

/**
 * This object wraps a "possibly shared" MimeMessage tracking copies and
 * automatically cloning it (if shared) when a write operation is invoked.
 */
public class MimeMessageCopyOnWriteProxy extends MimeMessage implements
        Disposable {

    /**
     * Used internally to track the reference count
     */
    protected static class ReferenceCounter {

        /**
         * reference counter
         */
        private int referenceCount = 1;

        /**
         * @param original
         *            MimeMessageWrapper
         * @throws MessagingException
         */
        public ReferenceCounter() throws MessagingException {
            this(0);
        }

        /**
         * @param original
         *            MimeMessage to wrap
         * @param writable
         *            if true we can alter the message itself, otherwise copy on
         *            write it.
         * @throws MessagingException
         */
        public ReferenceCounter(int startCounter) throws MessagingException {
            referenceCount = startCounter;
        }

        protected synchronized void incrementReferenceCount() {
            referenceCount++;
        }

        protected synchronized void decrementReferenceCount() {
            referenceCount--;
        }

        protected synchronized int getReferenceCount() {
            return referenceCount;
        }

    }

    protected ReferenceCounter refCount;

    /**
     * @param original
     *            MimeMessageWrapper
     * @throws MessagingException
     */
    public MimeMessageCopyOnWriteProxy(MimeMessage original)
            throws MessagingException {
        this(original, false);
    }

    /**
     * @param original
     *            MimeMessageSource
     * @throws MessagingException
     */
    public MimeMessageCopyOnWriteProxy(MimeMessageSource original)
            throws MessagingException {
        this(new MimeMessageWrapper(original), true);
    }

    /**
     * Private constructor providing an external reference counter.
     * 
     * @param original
     * @param refCount
     * @throws MessagingException
     */
    private MimeMessageCopyOnWriteProxy(MimeMessage original,
            boolean writeable)
            throws MessagingException {
        super(Session.getDefaultInstance(System.getProperties(), null));

        this.wrapped = original;
        if (wrapped instanceof MimeMessageCopyOnWriteProxy) {
            refCount = ((MimeMessageCopyOnWriteProxy) wrapped).refCount;
            wrapped = ((MimeMessageCopyOnWriteProxy) wrapped).getWrappedMessage();
        } else {
            refCount = new ReferenceCounter();
        }
        
        if (!writeable) {
            refCount.incrementReferenceCount();
        }
    }

    /**
     * Check the number of references over the MimeMessage and clone it if
     * needed.
     * 
     * @throws MessagingException
     *             exception
     */
    protected void checkCopyOnWrite() throws MessagingException {
        synchronized (refCount) {
            if (refCount.getReferenceCount() > 1) {
                refCount.decrementReferenceCount();
                refCount = new ReferenceCounter(1);
                wrapped = new MimeMessageWrapper(wrapped);
            }
        }
    }

    /**
     * The mime message in memory
     */
    protected MimeMessage wrapped = null;

    /**
     * Rewritten for optimization purposes
     */
    public void writeTo(OutputStream os) throws IOException, MessagingException {
        wrapped.writeTo(os);
    }

    /**
     * Rewritten for optimization purposes
     */
    public void writeTo(OutputStream os, String[] ignoreList)
            throws IOException, MessagingException {
        wrapped.writeTo(os, ignoreList);
    }

    /**
     * Various reader methods
     */
    
    /**
     * @see javax.mail.Message#getFrom()
     */
    public Address[] getFrom() throws MessagingException {
        return wrapped.getFrom();
    }

    /**
     * @see javax.mail.Message#getRecipients(javax.mail.Message.RecipientType)
     */
    public Address[] getRecipients(Message.RecipientType type)
            throws MessagingException {
        return wrapped.getRecipients(type);
    }

    /**
     * @see javax.mail.Message#getAllRecipients()
     */
    public Address[] getAllRecipients() throws MessagingException {
        return wrapped.getAllRecipients();
    }

    /**
     * @see javax.mail.Message#getReplyTo()
     */
    public Address[] getReplyTo() throws MessagingException {
        return wrapped.getReplyTo();
    }

    /**
     * @see javax.mail.Message#getSubject()
     */
    public String getSubject() throws MessagingException {
        return wrapped.getSubject();
    }

    /**
     * @see javax.mail.Message#getSentDate()
     */
    public Date getSentDate() throws MessagingException {
        return wrapped.getSentDate();
    }

    /**
     * @see javax.mail.Message#getReceivedDate()
     */
    public Date getReceivedDate() throws MessagingException {
        return wrapped.getReceivedDate();
    }

    /**
     * @see javax.mail.Part#getSize()
     */
    public int getSize() throws MessagingException {
        return wrapped.getSize();
    }

    /**
     * @see javax.mail.Part#getLineCount()
     */
    public int getLineCount() throws MessagingException {
        return wrapped.getLineCount();
    }

    /**
     * @see javax.mail.Part#getContentType()
     */
    public String getContentType() throws MessagingException {
        return wrapped.getContentType();
    }

    /**
     * @see javax.mail.Part#isMimeType(java.lang.String)
     */
    public boolean isMimeType(String mimeType) throws MessagingException {
        return wrapped.isMimeType(mimeType);
    }

    /**
     * @see javax.mail.Part#getDisposition()
     */
    public String getDisposition() throws MessagingException {
        return wrapped.getDisposition();
    }

    /**
     * @see javax.mail.internet.MimePart#getEncoding()
     */
    public String getEncoding() throws MessagingException {
        return wrapped.getEncoding();
    }

    /**
     * @see javax.mail.internet.MimePart#getContentID()
     */
    public String getContentID() throws MessagingException {
        return wrapped.getContentID();
    }

    /**
     * @see javax.mail.internet.MimePart#getContentMD5()
     */
    public String getContentMD5() throws MessagingException {
        return wrapped.getContentMD5();
    }

    /**
     * @see javax.mail.Part#getDescription()
     */
    public String getDescription() throws MessagingException {
        return wrapped.getDescription();
    }

    /**
     * @see javax.mail.internet.MimePart#getContentLanguage()
     */
    public String[] getContentLanguage() throws MessagingException {
        return wrapped.getContentLanguage();
    }

    /**
     * @see javax.mail.internet.MimeMessage#getMessageID()
     */
    public String getMessageID() throws MessagingException {
        return wrapped.getMessageID();
    }

    /**
     * @see javax.mail.Part#getFileName()
     */
    public String getFileName() throws MessagingException {
        return wrapped.getFileName();
    }

    /**
     * @see javax.mail.Part#getInputStream()
     */
    public InputStream getInputStream() throws IOException, MessagingException {
        return wrapped.getInputStream();
    }

    /**
     * @see javax.mail.Part#getDataHandler()
     */
    public DataHandler getDataHandler() throws MessagingException {
        return wrapped.getDataHandler();
    }

    /**
     * @see javax.mail.Part#getContent()
     */
    public Object getContent() throws IOException, MessagingException {
        return wrapped.getContent();
    }

    /**
     * @see javax.mail.Part#getHeader(java.lang.String)
     */
    public String[] getHeader(String name) throws MessagingException {
        return wrapped.getHeader(name);
    }

    /**
     * @see javax.mail.internet.MimePart#getHeader(java.lang.String, java.lang.String)
     */
    public String getHeader(String name, String delimiter)
            throws MessagingException {
        return wrapped.getHeader(name, delimiter);
    }

    /**
     * @see javax.mail.Part#getAllHeaders()
     */
    public Enumeration getAllHeaders() throws MessagingException {
        return wrapped.getAllHeaders();
    }

    /**
     * @see javax.mail.Part#getMatchingHeaders(java.lang.String[])
     */
    public Enumeration getMatchingHeaders(String[] names)
            throws MessagingException {
        return wrapped.getMatchingHeaders(names);
    }

    /**
     * @see javax.mail.Part#getNonMatchingHeaders(java.lang.String[])
     */
    public Enumeration getNonMatchingHeaders(String[] names)
            throws MessagingException {
        return wrapped.getNonMatchingHeaders(names);
    }

    /**
     * @see javax.mail.internet.MimePart#getAllHeaderLines()
     */
    public Enumeration getAllHeaderLines() throws MessagingException {
        return wrapped.getAllHeaderLines();
    }

    /**
     * @see javax.mail.internet.MimePart#getMatchingHeaderLines(java.lang.String[])
     */
    public Enumeration getMatchingHeaderLines(String[] names)
            throws MessagingException {
        return wrapped.getMatchingHeaderLines(names);
    }

    /**
     * @see javax.mail.internet.MimePart#getNonMatchingHeaderLines(java.lang.String[])
     */
    public Enumeration getNonMatchingHeaderLines(String[] names)
            throws MessagingException {
        return wrapped.getNonMatchingHeaderLines(names);
    }

    /**
     * @see javax.mail.Message#getFlags()
     */
    public Flags getFlags() throws MessagingException {
        return wrapped.getFlags();
    }

    /**
     * @see javax.mail.Message#isSet(javax.mail.Flags.Flag)
     */
    public boolean isSet(Flags.Flag flag) throws MessagingException {
        return wrapped.isSet(flag);
    }

    /**
     * @see javax.mail.internet.MimeMessage#getSender()
     */
    public Address getSender() throws MessagingException {
        return wrapped.getSender();
    }

    /**
     * @see javax.mail.Message#match(javax.mail.search.SearchTerm)
     */
    public boolean match(SearchTerm arg0) throws MessagingException {
        return wrapped.match(arg0);
    }

    /**
     * @see javax.mail.internet.MimeMessage#getRawInputStream()
     */
    public InputStream getRawInputStream() throws MessagingException {
        return wrapped.getRawInputStream();
    }

    /**
     * @see javax.mail.Message#getFolder()
     */
    public Folder getFolder() {
        return wrapped.getFolder();
    }

    /**
     * @see javax.mail.Message#getMessageNumber()
     */
    public int getMessageNumber() {
        return wrapped.getMessageNumber();
    }

    /**
     * @see javax.mail.Message#isExpunged()
     */
    public boolean isExpunged() {
        return wrapped.isExpunged();
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object arg0) {
        return wrapped.equals(arg0);
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
        return wrapped.hashCode();
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return wrapped.toString();
    }

    /*
     * Various writer methods
     */

    /**
     * @see javax.mail.Message#setFrom(javax.mail.Address)
     */
    public void setFrom(Address address) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setFrom(address);
    }

    /**
     * @see javax.mail.Message#setFrom()
     */
    public void setFrom() throws MessagingException {
        checkCopyOnWrite();
        wrapped.setFrom();
    }

    /**
     * @see javax.mail.Message#addFrom(javax.mail.Address[])
     */
    public void addFrom(Address[] addresses) throws MessagingException {
        checkCopyOnWrite();
        wrapped.addFrom(addresses);
    }

    /**
     * @see javax.mail.Message#setRecipients(javax.mail.Message.RecipientType, javax.mail.Address[])
     */
    public void setRecipients(Message.RecipientType type, Address[] addresses)
            throws MessagingException {
        checkCopyOnWrite();
        wrapped.setRecipients(type, addresses);
    }

    /**
     * @see javax.mail.Message#addRecipients(javax.mail.Message.RecipientType, javax.mail.Address[])
     */
    public void addRecipients(Message.RecipientType type, Address[] addresses)
            throws MessagingException {
        checkCopyOnWrite();
        wrapped.addRecipients(type, addresses);
    }

    /**
     * @see javax.mail.Message#setReplyTo(javax.mail.Address[])
     */
    public void setReplyTo(Address[] addresses) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setReplyTo(addresses);
    }

    /**
     * @see javax.mail.Message#setSubject(java.lang.String)
     */
    public void setSubject(String subject) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setSubject(subject);
    }

    /**
     * @see javax.mail.internet.MimeMessage#setSubject(java.lang.String, java.lang.String)
     */
    public void setSubject(String subject, String charset)
            throws MessagingException {
        checkCopyOnWrite();
        wrapped.setSubject(subject, charset);
    }

    /**
     * @see javax.mail.Message#setSentDate(java.util.Date)
     */
    public void setSentDate(Date d) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setSentDate(d);
    }

    /**
     * @see javax.mail.Part#setDisposition(java.lang.String)
     */
    public void setDisposition(String disposition) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setDisposition(disposition);
    }

    /**
     * @see javax.mail.internet.MimeMessage#setContentID(java.lang.String)
     */
    public void setContentID(String cid) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setContentID(cid);
    }

    /**
     * @see javax.mail.internet.MimePart#setContentMD5(java.lang.String)
     */
    public void setContentMD5(String md5) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setContentMD5(md5);
    }

    /**
     * @see javax.mail.Part#setDescription(java.lang.String)
     */
    public void setDescription(String description) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setDescription(description);
    }

    /**
     * @see javax.mail.internet.MimeMessage#setDescription(java.lang.String, java.lang.String)
     */
    public void setDescription(String description, String charset)
            throws MessagingException {
        checkCopyOnWrite();
        wrapped.setDescription(description, charset);
    }

    /**
     * @see javax.mail.internet.MimePart#setContentLanguage(java.lang.String[])
     */
    public void setContentLanguage(String[] languages)
            throws MessagingException {
        checkCopyOnWrite();
        wrapped.setContentLanguage(languages);
    }

    /**
     * @see javax.mail.Part#setFileName(java.lang.String)
     */
    public void setFileName(String filename) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setFileName(filename);
    }

    /**
     * @see javax.mail.Part#setDataHandler(javax.activation.DataHandler)
     */
    public void setDataHandler(DataHandler dh) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setDataHandler(dh);
    }

    /**
     * @see javax.mail.Part#setContent(java.lang.Object, java.lang.String)
     */
    public void setContent(Object o, String type) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setContent(o, type);
    }

    /**
     * @see javax.mail.Part#setText(java.lang.String)
     */
    public void setText(String text) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setText(text);
    }

    /**
     * @see javax.mail.internet.MimePart#setText(java.lang.String, java.lang.String)
     */
    public void setText(String text, String charset) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setText(text, charset);
    }

    /**
     * @see javax.mail.Part#setContent(javax.mail.Multipart)
     */
    public void setContent(Multipart mp) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setContent(mp);
    }

    public Message reply(boolean replyToAll) throws MessagingException {
        checkCopyOnWrite();
        return wrapped.reply(replyToAll);
    }

    /**
     * @see javax.mail.Part#setHeader(java.lang.String, java.lang.String)
     */
    public void setHeader(String name, String value) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setHeader(name, value);
    }

    /**
     * @see javax.mail.Part#addHeader(java.lang.String, java.lang.String)
     */
    public void addHeader(String name, String value) throws MessagingException {
        checkCopyOnWrite();
        wrapped.addHeader(name, value);
    }

    /**
     * @see javax.mail.Part#removeHeader(java.lang.String)
     */
    public void removeHeader(String name) throws MessagingException {
        checkCopyOnWrite();
        wrapped.removeHeader(name);
    }

    /**
     * @see javax.mail.internet.MimePart#addHeaderLine(java.lang.String)
     */
    public void addHeaderLine(String line) throws MessagingException {
        checkCopyOnWrite();
        wrapped.addHeaderLine(line);
    }

    /**
     * @see javax.mail.Message#setFlags(javax.mail.Flags, boolean)
     */
    public void setFlags(Flags flag, boolean set) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setFlags(flag, set);
    }

    /**
     * @see javax.mail.Message#saveChanges()
     */
    public void saveChanges() throws MessagingException {
        checkCopyOnWrite();
        wrapped.saveChanges();
    }

    /*
     * Since JavaMail 1.2
     */

    /**
     * @see javax.mail.internet.MimeMessage#addRecipients(javax.mail.Message.RecipientType, java.lang.String)
     */
    public void addRecipients(Message.RecipientType type, String addresses)
            throws MessagingException {
        checkCopyOnWrite();
        wrapped.addRecipients(type, addresses);
    }

    /**
     * @see javax.mail.internet.MimeMessage#setRecipients(javax.mail.Message.RecipientType, java.lang.String)
     */
    public void setRecipients(Message.RecipientType type, String addresses)
            throws MessagingException {
        checkCopyOnWrite();
        wrapped.setRecipients(type, addresses);
    }

    /**
     * @see javax.mail.internet.MimeMessage#setSender(javax.mail.Address)
     */
    public void setSender(Address arg0) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setSender(arg0);
    }

    /**
     * @see javax.mail.Message#addRecipient(javax.mail.Message.RecipientType, javax.mail.Address)
     */
    public void addRecipient(RecipientType arg0, Address arg1)
            throws MessagingException {
        checkCopyOnWrite();
        wrapped.addRecipient(arg0, arg1);
    }

    /**
     * @see javax.mail.Message#setFlag(javax.mail.Flags.Flag, boolean)
     */
    public void setFlag(Flag arg0, boolean arg1) throws MessagingException {
        checkCopyOnWrite();
        wrapped.setFlag(arg0, arg1);
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public synchronized void dispose() {
        if (wrapped != null) {
            refCount.decrementReferenceCount();
            if (refCount.getReferenceCount()<=0) {
                if (wrapped instanceof Disposable) {
                    ((Disposable) wrapped).dispose();
                }
            }
            wrapped = null;
        }
    }

    /**
     * @see java.lang.Object#finalize()
     */
    protected void finalize() throws Throwable {
        dispose();
        super.finalize();
    }

    /**
     * @return the message size
     * @throws MessagingException 
     */
    public long getMessageSize() throws MessagingException {
        return MimeMessageUtil.getMessageSize(wrapped);
    }

    /**
     * @return
     */
    public MimeMessage getWrappedMessage() {
        return wrapped;
    }

}
