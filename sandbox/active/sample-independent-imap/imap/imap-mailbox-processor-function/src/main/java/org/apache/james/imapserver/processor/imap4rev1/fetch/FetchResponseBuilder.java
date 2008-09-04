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

/**
 * 
 */
package org.apache.james.imapserver.processor.imap4rev1.fetch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;
import javax.mail.MessagingException;

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.message.BodyFetchElement;
import org.apache.james.api.imap.message.FetchData;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.api.imap.process.SelectedImapMailbox;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse;
import org.apache.james.imapserver.processor.base.ImapSessionUtils;
import org.apache.james.mailboxmanager.Headers;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.impl.MessageRangeImpl;
import org.apache.james.mailboxmanager.mailbox.Mailbox;
import org.apache.james.mailboxmanager.util.MessageResultUtils;
import org.apache.james.mime4j.field.address.parser.ParseException;

final class FetchResponseBuilder {
    private final Logger logger;
    private final EnvelopeBuilder envelopeBuilder;
    
    private int msn;
    private Long uid;
    private Flags flags;
    private Date internalDate;
    private Integer size;
    private List elements;
    private FetchResponse.Envelope envelope;
    private FetchResponse.Structure body;
    private FetchResponse.Structure bodystructure;
    
    public FetchResponseBuilder(final Logger logger,final EnvelopeBuilder envelopeBuilder) {
        super();
        this.logger = logger;
        this.envelopeBuilder = envelopeBuilder;
    }

    public void reset(int msn) {
        this.msn = msn;
        uid = null;
        flags = null;
        internalDate = null;
        size = null;    
        body = null;
        bodystructure = null;
        elements = null;
    }
    
    public void setUid(long uid) {
        this.uid = new Long(uid);
    }
    
    public void setFlags(Flags flags) {
        this.flags = flags;
    }
    
    public FetchResponse build() {
        final FetchResponse result = new FetchResponse(msn, flags, uid, internalDate, 
                size, envelope, body, bodystructure, elements);
        return result;
    }

    public FetchResponse build(FetchData fetch, MessageResult result,
            ImapSession session, boolean useUids) throws MessagingException, ParseException {
        Mailbox mailbox = ImapSessionUtils.getMailbox(session);
        final SelectedImapMailbox selected = session.getSelected();
        final long resultUid = result.getUid();
        final int resultMsn = selected.msn(resultUid);
        setMsn(resultMsn);

        // Check if this fetch will cause the "SEEN" flag to be set on this
        // message
        // If so, update the flags, and ensure that a flags response is included
        // in the response.
        final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
        boolean ensureFlagsResponse = false;
        final Flags resultFlags = result.getFlags();
        if (fetch.isSetSeen()
                && !resultFlags.contains(Flags.Flag.SEEN)) {
            mailbox.setFlags(new Flags(Flags.Flag.SEEN), true, false,
                    MessageRangeImpl.oneUid(resultUid), FetchGroupImpl.MINIMAL, mailboxSession);
            resultFlags.add(Flags.Flag.SEEN);
            ensureFlagsResponse = true;
        }

        // FLAGS response
        if (fetch.isFlags() || ensureFlagsResponse) {
            if (selected.isRecent(resultUid)) {
                resultFlags.add(Flags.Flag.RECENT);
            }
            setFlags(resultFlags);
        }

        // INTERNALDATE response
        if (fetch.isInternalDate()) {
            setInternalDate(result
                    .getInternalDate());
        }

        // RFC822.SIZE response
        if (fetch.isSize()) {
            setSize(result.getSize());
        }

        if (fetch.isEnvelope()) {
            this.envelope = buildEnvelope(result);
        }

        // Only create when needed
        if (fetch.isBody() || fetch.isBodyStructure()) {
            final MessageResult.MimeDescriptor descriptor = result.getMimeDescriptor();

            // BODY response
            if (fetch.isBody()) {
                body = new MimeDescriptorStructure(false, descriptor, envelopeBuilder);
            }

            // BODYSTRUCTURE response
            if (fetch.isBodyStructure()) {
                bodystructure = new MimeDescriptorStructure(true, descriptor, envelopeBuilder);
            }
        }
        // UID response
        if (fetch.isUid()) {
            setUid(resultUid);
        }

        // BODY part responses.
        Collection elements = fetch.getBodyElements();
        this.elements = new ArrayList();
        for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
            BodyFetchElement fetchElement = (BodyFetchElement) iterator.next();
            final FetchResponse.BodyElement element = bodyFetch(result, fetchElement);
            if (element != null) {
                this.elements.add(element);
            }
        }
        return build();
    }

    private FetchResponse.Envelope buildEnvelope(final Headers headers) throws MessagingException, ParseException {
        return envelopeBuilder.buildEnvelope(headers);
    }
    
    private void setSize(int size) {
        this.size = new Integer(size);
    }

    public void setInternalDate(Date internalDate) {
        this.internalDate = internalDate;
    }

    private void setMsn(int msn) {
        reset(msn);
    }

    private FetchResponse.BodyElement bodyFetch(final MessageResult messageResult,
            BodyFetchElement fetchElement) throws MessagingException {
        
        final Long firstOctet = fetchElement.getFirstOctet();
        final Long numberOfOctets = fetchElement.getNumberOfOctets();
        final String name = fetchElement.getResponseName();
        final int specifier = fetchElement.getSectionType();
        final int[] path = fetchElement.getPath();
        final Collection names = fetchElement.getFieldNames();
        final boolean isBase = (path == null || path.length == 0);
        final FetchResponse.BodyElement fullResult 
            = bodyContent(messageResult, name, specifier, path, names, isBase);
        final FetchResponse.BodyElement result 
            = wrapIfPartialFetch(firstOctet, numberOfOctets, fullResult);
        return result;

    }

    private FetchResponse.BodyElement bodyContent(final MessageResult messageResult, final String name, 
            final int specifier, final int[] path, final Collection names, final boolean isBase) throws MessagingException {
        final FetchResponse.BodyElement fullResult;
        switch (specifier) {
            case BodyFetchElement.CONTENT:
                fullResult = content(messageResult, name, path, isBase);
                break;
                
            case BodyFetchElement.HEADER_FIELDS:
                fullResult = fields(messageResult, name, path, names, isBase);
                break;
                
            case BodyFetchElement.HEADER_NOT_FIELDS:
                fullResult = fieldsNot(messageResult, name, path, names, isBase);
                break;
                
            case BodyFetchElement.MIME:
                fullResult = mimeHeaders(messageResult, name, path, isBase);
                break;
            case BodyFetchElement.HEADER:
                fullResult = headers(messageResult, name, path, isBase);
                break;
                
            case BodyFetchElement.TEXT:
                fullResult = text(messageResult, name, path, isBase);
                break;
                
            default:
                fullResult = null;
            break;
        }
        return fullResult;
    }

    private FetchResponse.BodyElement wrapIfPartialFetch(final Long firstOctet, final Long numberOfOctets, 
            final FetchResponse.BodyElement fullResult) {
        final FetchResponse.BodyElement result;
        if (firstOctet == null) {
            result = fullResult;
        } else {
            final long numberOfOctetsAsLong;
            if (numberOfOctets == null) {
                numberOfOctetsAsLong = Long.MAX_VALUE;
            } else {
                numberOfOctetsAsLong = numberOfOctets.longValue();
            }
            final long firstOctetAsLong = firstOctet.longValue();
            result = new PartialFetchBodyElement(fullResult, 
                    firstOctetAsLong, numberOfOctetsAsLong); 
        }
        return result;
    }

    private FetchResponse.BodyElement text(final MessageResult messageResult, String name, final int[] path, final boolean isBase) throws MailboxManagerException {
        final FetchResponse.BodyElement result;
        final MessageResult.Content body;
        if (isBase) {
            body = messageResult.getBody();
        } else {
            MessageResult.MimePath mimePath = new MimePathImpl(path);
            body = messageResult.getBody(mimePath);
        }
        result = new ContentBodyElement(name, body);
        return result;
    }

    private FetchResponse.BodyElement mimeHeaders(final MessageResult messageResult, 
            String name, final int[] path, final boolean isBase) throws MessagingException {
        final FetchResponse.BodyElement result;
        final Iterator headers = getMimeHeaders(messageResult, path, isBase);
        List lines = MessageResultUtils.getAll(headers);
        result = new HeaderBodyElement(name, lines);
        return result;
    }
    
    private FetchResponse.BodyElement headers(final MessageResult messageResult, String name, final int[] path, final boolean isBase) throws MailboxManagerException, MessagingException {
        final FetchResponse.BodyElement result;
        final Iterator headers = getHeaders(messageResult, path, isBase);
        List lines = MessageResultUtils.getAll(headers);
        result = new HeaderBodyElement(name, lines);
        return result;
    }

    private FetchResponse.BodyElement fieldsNot(final MessageResult messageResult, String name, 
            final int[] path, Collection names, final boolean isBase) throws MailboxManagerException, MessagingException {
        final FetchResponse.BodyElement result;
        
        final Iterator headers = getHeaders(messageResult, path, isBase);
        
        List lines = MessageResultUtils.getNotMatching(names, headers);
        result = new HeaderBodyElement(name, lines);
        return result;
    }

    private FetchResponse.BodyElement fields(final MessageResult messageResult, String name, 
            final int[] path, Collection names, final boolean isBase) throws MailboxManagerException, MessagingException {
        final FetchResponse.BodyElement result;
        final Iterator headers = getHeaders(messageResult, path, isBase);
        List lines = MessageResultUtils.getMatching(names, headers);
        result = new HeaderBodyElement(name, lines);
        return result;
    }

    private Iterator getHeaders(final MessageResult messageResult, final int[] path, final boolean isBase) throws MailboxManagerException {
        final Iterator headers;
        if (isBase) {
            headers = messageResult.headers();
        } else {
            MessageResult.MimePath mimePath = new MimePathImpl(path);
            headers = messageResult.iterateHeaders(mimePath);
        }
        return headers;
    }
    
    private Iterator getMimeHeaders(final MessageResult messageResult, final int[] path, 
            final boolean isBase) throws MessagingException {
        MessageResult.MimePath mimePath = new MimePathImpl(path);
        final Iterator headers = messageResult.iterateMimeHeaders(mimePath);
        return headers;
    }

    private FetchResponse.BodyElement content(final MessageResult messageResult, String name, 
            final int[] path, final boolean isBase) throws MessagingException {
        final FetchResponse.BodyElement result;
        final MessageResult.Content full;
        if (isBase) {
            full = messageResult.getFullContent();
        } else {
            MessageResult.MimePath mimePath = new MimePathImpl(path);
            full = messageResult.getMimeBody(mimePath);
        }
        result = new ContentBodyElement(name, full);
        return result;
    }
}