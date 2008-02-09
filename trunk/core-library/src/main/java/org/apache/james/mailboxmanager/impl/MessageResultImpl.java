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

package org.apache.james.mailboxmanager.impl;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import org.apache.commons.collections.IteratorUtils;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.MessageResultUtils;

/**
 * Bean based implementation.
 * {@link #getIncludedResults()} is updated when setters are called.
 */
public class MessageResultImpl implements MessageResult {
    
    private MimeMessage mimeMessage;
    private long uid;
    private Flags flags;
    private int size;
    private Date internalDate;
    private String key;
    private List headers;
    private Content body;
    private Content fullContent;
    private int includedResults = FetchGroup.MINIMAL;
    
    public MessageResultImpl(long uid) {
        setUid(uid);
    }

    public MessageResultImpl() {
    }

    public MessageResultImpl(long uid, Flags flags) {
        setUid(uid);
        setFlags(flags);
    }

    public MessageResultImpl(MessageResult result) throws MailboxManagerException {
        setUid(result.getUid()); 
        if (MessageResultUtils.isFlagsIncluded(result)) {
            setFlags(result.getFlags());
        }
        if (MessageResultUtils.isMimeMessageIncluded(result)) {
            setMimeMessage(result.getMimeMessage());
        }
        if (MessageResultUtils.isSizeIncluded(result)) {
            setSize(result.getSize());
        }
        if (MessageResultUtils.isInternalDateIncluded(result)) {
            setInternalDate(result.getInternalDate());
        }
        if (MessageResultUtils.isKeyIncluded(result)) {
            setKey(result.getKey());
        }
        if (MessageResultUtils.isHeadersIncluded(result)) {
            setHeaders(IteratorUtils.toList(result.iterateHeaders()));
        }
        if (MessageResultUtils.isFullContentIncluded(result)) {
            setFullContent(result.getFullContent());
        }
        if (MessageResultUtils.isBodyContentIncluded(result)) {
            setBody(result.getBody());
        }
    }

    public MessageResult.FetchGroup getIncludedResults() {
        return new FetchGroupImpl(includedResults);
    }

    public MimeMessage getMimeMessage() {
        return mimeMessage;
    }

    public void setMimeMessage(MimeMessage mimeMessage) {
        this.mimeMessage=mimeMessage;
        if (mimeMessage != null) {
            includedResults |= FetchGroup.MIME_MESSAGE;
        }
    }
    
    public long getUid() {
        return uid;
    }

    public long getUidValidity() {
        // TODO implement or remove
        return 0;
    }

    public Date getInternalDate() {
        return internalDate;
    }

    public Flags getFlags() {
        return flags;
    }

    public String getKey() {
        return key;
    }

    public void setUid(long uid) {
        this.uid=uid;
    }
    
    public int getSize() {
        return size;
    }

    public void setFlags(Flags flags) {
        this.flags=flags;
        if (flags != null) {
            includedResults |= FetchGroup.FLAGS;
        }
    }

    public int compareTo(Object o) {
        MessageResult that=(MessageResult)o;
        if (this.uid>0 && that.getUid()>0) {
            // TODO: this seems inefficient
            return new Long(uid).compareTo(new Long(that.getUid()));    
        } else {
            // TODO: throwing an undocumented untyped runtime seems wrong
            // TODO: if uids must be greater than zero then this should be enforced
            // TODO: on the way in
            // TODO: probably an IllegalArgumentException would be better
            throw new RuntimeException("can't compare");
        }
        
    }

    public void setSize(int size) {
        this.size=size;
        includedResults |= FetchGroup.SIZE;
    }

    public void setInternalDate(Date internalDate) {
        this.internalDate = internalDate;
        if (internalDate != null) {
            includedResults |= FetchGroup.INTERNAL_DATE;
        }
    }

    public void setKey(String key) {
        this.key=key;
        if (key != null) {
            includedResults |= FetchGroup.KEY;
        }
    }

    public Iterator iterateHeaders() {
        return headers.iterator();
    }
    
    public List getHeaders() {
        return headers;
    }

    public void setHeaders(List headers) {
        this.headers = headers;
        if (headers != null) {
            includedResults |= FetchGroup.HEADERS;
        }
    }

    public final Content getFullContent() {
        return fullContent;
    }

    public final void setFullContent(Content fullMessage) {
        this.fullContent = fullMessage;
        if (fullMessage != null) {
            includedResults |= FetchGroup.FULL_CONTENT;
        }
    }

    public final Content getBody() {
        return body;
    }

    public final void setBody(Content messageBody) {
        this.body = messageBody;
        if (messageBody != null) {
            includedResults |= FetchGroup.BODY_CONTENT;
        }
    }

    /**
     * Renders suitably for logging.
     *
     * @return a <code>String</code> representation 
     * of this object.
     */
    public String toString()
    {
        final String TAB = " ";
        
        String retValue = "MessageResultImpl ( "
            + "uid = " + this.uid + TAB
            + "flags = " + this.flags + TAB
            + "size = " + this.size + TAB
            + "internalDate = " + this.internalDate + TAB
            + "key = " + this.key + TAB
            + "includedResults = " + this.includedResults + TAB
            + " )";
    
        return retValue;
    }

    public Content getBody(MimePath path) throws MailboxManagerException {
        throw new MailboxManagerException("Unsupported operation");
    }

    public Content getFullContent(MimePath path) throws MailboxManagerException {
        throw new MailboxManagerException("Unsupported operation");
    }

    public Iterator iterateHeaders(MimePath path) throws MailboxManagerException {
        throw new MailboxManagerException("Unsupported operation");
    }
    
    
}
