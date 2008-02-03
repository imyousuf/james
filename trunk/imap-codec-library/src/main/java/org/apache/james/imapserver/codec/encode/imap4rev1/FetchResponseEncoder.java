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

package org.apache.james.imapserver.codec.encode.imap4rev1;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;

import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse;
import org.apache.james.imapserver.codec.encode.EncoderUtils;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.apache.james.imapserver.codec.encode.base.AbstractChainedImapEncoder;

public class FetchResponseEncoder extends AbstractChainedImapEncoder {

    public FetchResponseEncoder(final ImapEncoder next) {
        super(next);
    }

    public boolean isAcceptable(final ImapMessage message) {
        return (message instanceof FetchResponse);
    }

    protected void doEncode(ImapMessage acceptableMessage, ImapResponseComposer composer) throws IOException {
        if (acceptableMessage instanceof FetchResponse) {
            final FetchResponse fetchResponse = (FetchResponse) acceptableMessage;
            final long messageNumber = fetchResponse.getMessageNumber();
            composer.openFetchResponse(messageNumber);
            encodeFlags(composer, fetchResponse);
            encodeInternalDate(composer, fetchResponse);
            encodeSize(composer, fetchResponse);
            encodeEnvelope(composer, fetchResponse);
            encode(composer, fetchResponse.getMisc());
            encodeUid(composer, fetchResponse);
            encodeBodyElements(composer, fetchResponse.getElements());
            composer.closeFetchResponse();
        }
    }
    
    private void encodeBodyElements(final ImapResponseComposer composer, final List elements) throws IOException {
        if (elements != null) {
            for (final Iterator it = elements.iterator();it.hasNext();) {
                FetchResponse.BodyElement element = (FetchResponse.BodyElement) it.next();
                composer.message(element.getName());
                composer.literal(element);
            }
        }
    }

    private void encode(ImapResponseComposer composer, StringBuffer buffer) throws IOException {
        if (buffer != null && buffer.length() > 0) {
            composer.message(buffer.substring(1));
        }
    }

    private void encodeSize(ImapResponseComposer composer, final FetchResponse fetchResponse) throws IOException {
        final Integer size = fetchResponse.getSize();
        if (size != null) {
            // TODO: add method to composer
            composer.message("RFC822.SIZE");
            composer.message(size.intValue());
        }
    }

    private void encodeInternalDate(ImapResponseComposer composer, final FetchResponse fetchResponse) throws IOException {
        final Date internalDate = fetchResponse.getInternalDate();
        if (internalDate != null) {
            // TODO: add method to composer
            composer.message("INTERNALDATE");
            composer.quote(EncoderUtils.encodeDateTime(internalDate));
        }
    }

    private void encodeUid(ImapResponseComposer composer, final FetchResponse fetchResponse) throws IOException {
        final Long uid = fetchResponse.getUid();
        if (uid != null) {
            composer.message(ImapConstants.UID);
            composer.message(uid.longValue());
        }
    }

    private void encodeFlags(ImapResponseComposer composer, final FetchResponse fetchResponse) throws IOException {
        final Flags flags = fetchResponse.getFlags();
        if (flags != null) {
            composer.flags(flags);
        }
    }

    private void encodeEnvelope(final ImapResponseComposer composer, final FetchResponse fetchResponse) throws IOException {
        final FetchResponse.Envelope envelope = fetchResponse.getEnvelope();
        if (envelope != null) {
            final String date = envelope.getDate();
            final String subject = envelope.getSubject();
            final FetchResponse.Envelope.Address[] from = envelope.getFrom();
            final FetchResponse.Envelope.Address[] sender = envelope.getSender();
            final FetchResponse.Envelope.Address[] replyTo = envelope.getReplyTo();
            final FetchResponse.Envelope.Address[] to = envelope.getTo();
            final FetchResponse.Envelope.Address[] cc = envelope.getCc();
            final FetchResponse.Envelope.Address[] bcc = envelope.getBcc();
            final String inReplyTo = envelope.getInReplyTo();
            final String messageId = envelope.getMessageId();
            
            composer.startEnvelope(date, subject);
            encodeAddresses(composer, from);
            encodeAddresses(composer, sender);
            encodeAddresses(composer, replyTo);
            encodeAddresses(composer, to);
            encodeAddresses(composer, cc);
            encodeAddresses(composer, bcc);
            composer.endEnvelope(inReplyTo, messageId);
        }
    }
    
    private void encodeAddresses(final ImapResponseComposer composer, final FetchResponse.Envelope.Address[] addresses) throws IOException {
        if (addresses == null || addresses.length == 0) {
            composer.nil();
        } else {
            composer.startAddresses();
            final int length = addresses.length;
            for (int i=0;i<length;i++) {
                final FetchResponse.Envelope.Address address = addresses[i];
                encodeAddress(composer, address);
            }
            composer.endAddresses();
        }
    }

    private void encodeAddress(final ImapResponseComposer composer, final FetchResponse.Envelope.Address address) throws IOException {
        final String name = address.getPersonalName();
        final String domainList = address.getAtDomainList();
        final String mailbox = address.getMailboxName();
        final String host = address.getHostName();
        composer.address(name, domainList, mailbox, host);
    }
}
