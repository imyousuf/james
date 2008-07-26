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
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.Flags;

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse.Structure;
import org.apache.james.imapserver.codec.encode.EncoderUtils;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.apache.james.imapserver.codec.encode.base.AbstractChainedImapEncoder;

public class FetchResponseEncoder extends AbstractChainedImapEncoder {

    private static final String[] EMPTY_STRING_ARRAY={};
    
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
            encodeBody(composer, fetchResponse.getBody());
            encodeBodyStructure(composer, fetchResponse.getBodyStructure());
            encodeUid(composer, fetchResponse);
            encodeBodyElements(composer, fetchResponse.getElements());
            composer.closeFetchResponse();
        }
    }
    
    private void encodeBody(ImapResponseComposer composer, Structure body) throws IOException {
        if (body != null) {
            composer.message(ImapConstants.FETCH_BODY);
            encodeStructure(composer, body, false, false);
        }
    }

    private void encodeBodyStructure(ImapResponseComposer composer, Structure bodyStructure) throws IOException {
        if (bodyStructure != null) {
            composer.message(ImapConstants.FETCH_BODY_STRUCTURE);
            encodeStructure(composer, bodyStructure, true, false);
        }
    }
    
    private void encodeStructure(final ImapResponseComposer composer, 
            final Structure structure, final boolean includeExtensions, final boolean isInnerPart) throws IOException {
        
        final String mediaType;
        final String subType;
        final String rawMediaType = structure.getMediaType();
        if (rawMediaType == null) {
            mediaType = ImapConstants.MIME_TYPE_TEXT;
            subType = ImapConstants.MIME_SUBTYPE_PLAIN;
        } else {
            mediaType = rawMediaType;
            subType = structure.getSubType();
        }
        encodeStructure(composer, structure, includeExtensions, mediaType, subType, isInnerPart);
    }

    private void encodeStructure(final ImapResponseComposer composer, final Structure structure, 
            final boolean includeExtensions, final String mediaType, final String subType, boolean isInnerPart) throws IOException {
        if (isInnerPart) {
            composer.skipNextSpace();
        }
        if (ImapConstants.MIME_TYPE_MULTIPART.equalsIgnoreCase(mediaType)) {
            
            encodeMultipart(composer, structure, subType, includeExtensions);
            
        } else {
            if (ImapConstants.MIME_TYPE_MESSAGE.equalsIgnoreCase(mediaType) 
                    && ImapConstants.MIME_SUBTYPE_RFC822.equalsIgnoreCase(subType)) {

                encodeRfc822Message(composer, structure, mediaType, subType, includeExtensions);
            } else {
                encodeBasic(composer, structure, includeExtensions, mediaType, subType);
            }
        }
    }

    private void encodeBasic(final ImapResponseComposer composer, final Structure structure, final boolean includeExtensions, final String mediaType, final String subType) throws IOException {
        if (ImapConstants.MIME_TYPE_TEXT.equalsIgnoreCase(mediaType)) {
            
            final long lines = structure.getLines();
            
            encodeBodyFields(composer, structure, mediaType, subType);
            composer.message(lines);                
        } else {
            encodeBodyFields(composer, structure, mediaType, subType);
        }
        if (includeExtensions) {
            encodeOnePartBodyExtensions(composer, structure);
        }   
        composer.closeParen();
    }

    private void encodeOnePartBodyExtensions(final ImapResponseComposer composer, final Structure structure) throws IOException {
        final String md5 = structure.getMD5();
        final String[] languages = languages(structure);
        final String location = structure.getLocation();
        composer.nillableQuote(md5);
        bodyFldDsp(structure, composer).nillableQuotes(languages).nillableQuote(location);
    }

    private ImapResponseComposer bodyFldDsp(final Structure structure, final ImapResponseComposer composer) throws IOException {
        final String disposition = structure.getDisposition();
        if (disposition == null) {
            composer.nil();
        } else {
            composer.openParen();
            composer.quote(disposition);
            final Map params = structure.getDispositionParams();
            bodyFldParam(params, composer);
            composer.closeParen();
        }
        return composer;
    }
    
    private void bodyFldParam(final Map params, final ImapResponseComposer composer) throws IOException {
        if (params == null || params.isEmpty()) {
            composer.nil();
        } else {
            composer.openParen();
            final Set keySet = params.keySet();
            final Collection names = new TreeSet(keySet);
            for (Iterator iter = names.iterator(); iter.hasNext();) {
                final String name = (String) iter.next();
                final String value = (String) params.get(name);
                if (value == null) {
                    final Logger logger = getLogger();
                    logger.warn("Disposition parameter name has no value.");
                    if (logger.isDebugEnabled()) {
                        logger.debug("Disposition parameter " + name + " has no matching value");
                    }
                } else {
                    composer.quote(name);
                    composer.quote(value);
                }
            }
            composer.closeParen();
        }
    }

    private void encodeBodyFields(final ImapResponseComposer composer, final Structure structure, final String mediaType, final String subType) throws IOException {
        final String[] bodyParams = structure.getParameters();
        final String id = structure.getId();
        final String description = structure.getDescription();
        final String encoding = structure.getEncoding();
        final long octets = structure.getOctets();
        composer.openParen().quoteUpperCaseAscii(mediaType).quoteUpperCaseAscii(subType).nillableQuotes(bodyParams)
                .nillableQuote(id).nillableQuote(description).quoteUpperCaseAscii(encoding).message(octets);
    }

    private void encodeMultipart(ImapResponseComposer composer, Structure structure, 
            final String subType, final boolean includeExtensions) throws IOException {
        composer.openParen();
        
        for (Iterator it = structure.parts(); it.hasNext();) {
            final Structure part = (Structure) it.next();
            encodeStructure(composer, part, includeExtensions, true);
        }
        
        composer.quoteUpperCaseAscii(subType);
        if (includeExtensions) {
            final String[] languages = languages(structure);
            composer.nillableQuotes(structure.getParameters());
            bodyFldDsp(structure, composer).nillableQuotes(languages).nillableQuote(structure.getLocation());
        }
        composer.closeParen();
    }

    private String[] languages(Structure structure) {
        final List languageList = structure.getLanguages();
        final String[] languages;
        if (languageList == null) {
            languages = null;
        } else {
            languages = (String[]) languageList.toArray(EMPTY_STRING_ARRAY);
        }
        return languages;
    }

    private void encodeRfc822Message(ImapResponseComposer composer, Structure structure, 
            final String mediaType, final String subType, final boolean includeExtensions) throws IOException {
        final long lines = structure.getLines();
        final FetchResponse.Envelope envelope = structure.getEnvelope();
        final FetchResponse.Structure embeddedStructure = structure.getBody();
        
        encodeBodyFields(composer, structure, mediaType, subType);
        encodeEnvelope(composer, envelope);
        encodeStructure(composer, embeddedStructure, includeExtensions, true);
        composer.message(lines);
        
        if (includeExtensions) {
            encodeOnePartBodyExtensions(composer, structure);
        }
        composer.closeParen();
    }
    

    private void encodeBodyElements(final ImapResponseComposer composer, final List elements) throws IOException {
        if (elements != null) {
            for (final Iterator it = elements.iterator();it.hasNext();) {
                FetchResponse.BodyElement element = (FetchResponse.BodyElement) it.next();
                final String name = element.getName();
                composer.message(name);
                composer.literal(element);
            }
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
        encodeEnvelope(composer, envelope);
    }

    private void encodeEnvelope(final ImapResponseComposer composer, final FetchResponse.Envelope envelope) throws IOException {
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
