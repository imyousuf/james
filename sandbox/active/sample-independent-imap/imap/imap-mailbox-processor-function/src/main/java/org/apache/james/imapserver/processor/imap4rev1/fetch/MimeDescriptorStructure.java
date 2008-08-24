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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.mail.MessagingException;

import org.apache.james.api.imap.ImapConstants;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse.Envelope;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse.Structure;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.MessageResult.MimeDescriptor;
import org.apache.james.mime4j.field.address.parser.ParseException;

final class MimeDescriptorStructure implements FetchResponse.Structure {


    
    private final MessageResult.MimeDescriptor descriptor;
    private final String[] parameters;
    private final List parts;
    private final String disposition;
    private final Map dispositionParams;
    private final String location;
    private final String md5;
    private final List languages;
    private final Structure embeddedMessageStructure;
    private final Envelope envelope;
    
    public MimeDescriptorStructure(final boolean allowExtensions, MimeDescriptor descriptor, EnvelopeBuilder builder) 
                throws MessagingException, ParseException {
        super();
        this.descriptor = descriptor;
        parameters = createParameters(descriptor);
        parts = createParts(allowExtensions, descriptor, builder);
        
        languages = descriptor.getLanguages();
        this.dispositionParams = descriptor.getDispositionParams();
        this.disposition = descriptor.getDisposition();
        
        this.md5 = descriptor.getContentMD5();
        this.location = descriptor.getContentLocation();
        
        final MimeDescriptor embeddedMessage = descriptor.embeddedMessage();
        if (embeddedMessage == null) {
            embeddedMessageStructure = null;
            envelope = null;
        } else {
            embeddedMessageStructure = new MimeDescriptorStructure(allowExtensions, embeddedMessage, builder);
            envelope = builder.buildEnvelope(embeddedMessage);
        }
    }

    private static List createParts(final boolean allowExtensions, final MimeDescriptor descriptor, final EnvelopeBuilder builder) throws MessagingException, ParseException {
        final List results = new ArrayList();
        for (Iterator it = descriptor.parts(); it.hasNext();) {
            final MimeDescriptor partDescriptor = (MimeDescriptor) it.next();
            results.add(new MimeDescriptorStructure(allowExtensions, partDescriptor, builder));
        }
        return results;
    }
    
    private static String[] createParameters(MimeDescriptor descriptor) throws MailboxManagerException {
        final List results = new ArrayList();
        // TODO: consider revising this design
        for (Iterator it = descriptor.contentTypeParameters(); it.hasNext();) {
            final MessageResult.Header header = (MessageResult.Header) it.next();
            final String name = header.getName();
            results.add(name);
            final String value = header.getValue();
            results.add(value);
        }

        return (String[]) results.toArray(ImapConstants.EMPTY_STRING_ARRAY);
    }

    public String getDescription() {
        return descriptor.getContentDescription();
    }

    public String getEncoding() {
        return descriptor.getTransferContentEncoding();
    }
    
    public String getId() {
        return descriptor.getContentID();
    }
    
    
    public long getLines() {
        return descriptor.getLines();
    }

    public String getMediaType() {
        return descriptor.getMimeType();
    }
    
    public long getOctets() {
        return descriptor.getBodyOctets();
    }
    
    public String[] getParameters() {
        return parameters;
    }
    
    public String getSubType() {
        return descriptor.getMimeSubType();
    }
    
    public Iterator parts() {
        return parts.iterator();
    }
    
    public String getDisposition() {
        return disposition;
    }
    
    public String getLocation() {
        return location;
    }
    
    public String getMD5() {
        return md5;
    }
    
    public List getLanguages() {
        return languages;
    }

    public Structure getBody() {
        return embeddedMessageStructure;
    }

    public Map getDispositionParams() {
        return dispositionParams;
    }

    public Envelope getEnvelope() {
        return envelope;
    }
    
}