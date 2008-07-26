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

package org.apache.james.mailboxmanager.torque;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.MessageResult.MimeDescriptor;
import org.apache.james.mime4j.MaximalBodyDescriptor;
import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.MimeTokenStream;

public class MimeDescriptorImpl implements MessageResult.MimeDescriptor {
    
    public static MimeDescriptorImpl build(final InputStream stream) throws IOException {
        final MimeTokenStream parser = MimeTokenStream.createMaximalDescriptorStream();
        parser.parse(stream);
        return createDescriptor(parser);
    }

    private static MimeDescriptorImpl createDescriptor(
            final MimeTokenStream parser) throws IOException, MimeException {
        int next = parser.next();
        while (next != MimeTokenStream.T_BODY && next != MimeTokenStream.T_END_OF_STREAM
                && next != MimeTokenStream.T_START_MULTIPART) { 
            next = parser.next();
        }        
        
        final MimeDescriptorImpl mimeDescriptorImpl;
        switch (next) {
            case MimeTokenStream.T_BODY:
                mimeDescriptorImpl = simplePartDescriptor(parser);
                break;
            case MimeTokenStream.T_START_MULTIPART:
                mimeDescriptorImpl = compositePartDescriptor(parser);
                break;
            case MimeTokenStream.T_END_OF_STREAM:
                throw new MimeException("Premature end of stream");
            default:
                throw new MimeException("Unexpected parse state");
        }
        return mimeDescriptorImpl;
    }

    private static MimeDescriptorImpl compositePartDescriptor(
            final MimeTokenStream parser) throws IOException {
        MaximalBodyDescriptor descriptor = (MaximalBodyDescriptor) parser.getBodyDescriptor();
        MimeDescriptorImpl mimeDescriptor = createDescriptor(0, 0, descriptor);
        int next = parser.next();
        while (next != MimeTokenStream.T_END_MULTIPART && next != MimeTokenStream.T_END_OF_STREAM) {
            if (next == MimeTokenStream.T_START_BODYPART) {
                mimeDescriptor.addPart(createDescriptor(parser));
            }
            next = parser.next();
        }
        return mimeDescriptor;
    }
    
    private static MimeDescriptorImpl simplePartDescriptor(
            final MimeTokenStream parser) throws IOException {
        final InputStream body = parser.getInputStream();
        long bodyOctets = 0;
        long lines = 0;
        for (int n=body.read();n>=0;n=body.read())  {
            if (n == '\r') {
                lines++;
            }
            bodyOctets++;
        }
        
        MaximalBodyDescriptor descriptor = (MaximalBodyDescriptor) parser.getBodyDescriptor();
        final MimeDescriptorImpl mimeDescriptorImpl = createDescriptor(
                bodyOctets, lines, descriptor);
        return mimeDescriptorImpl;
    }

    private static MimeDescriptorImpl createDescriptor(long bodyOctets,
            long lines, MaximalBodyDescriptor descriptor) {
        final String contentDescription = descriptor.getContentDescription();
        final String contentId = descriptor.getContentId();
        
        final String subType = descriptor.getSubType();
        final String type = descriptor.getMediaType();
        final String transferEncoding = descriptor.getTransferEncoding();
        final Collection headers = new ArrayList();
        final Collection contentTypeParameters = new ArrayList();
        final Map valuesByName = descriptor.getContentTypeParameters();
        for (final Iterator it=valuesByName.keySet().iterator(); it.hasNext(); ) {
            final String name = (String) it.next();
            final String value = (String) valuesByName.get(name);
            contentTypeParameters.add(new Header(name, value));
        }
        final String codeset = descriptor.getCharset();
        Header header;
        if (codeset == null) {
            if ("TEXT".equals(type)) { 
                header = new Header("charset", "us-ascii");
            } else {
                header = null;
            }
        } else {
            header = new Header("charset", codeset);
        }
        if (header != null) {
            contentTypeParameters.add(header);
        }
        final String boundary = descriptor.getBoundary();
        if (boundary != null) {
            contentTypeParameters.add(new Header("boundary", boundary));
        }
        final List languages = descriptor.getContentLanguage();
        final String disposition = descriptor.getContentDispositionType();
        final Map dispositionParams = descriptor.getContentDispositionParameters();
        final MessageResult.MimeDescriptor embeddedMessage = null;
        final Collection parts = new ArrayList();
        final String location = descriptor.getContentLocation();
        final String md5 = descriptor.getContentMD5Raw();
        final MimeDescriptorImpl mimeDescriptorImpl = new MimeDescriptorImpl(bodyOctets, contentDescription, contentId, lines, subType, type, transferEncoding,
                headers, contentTypeParameters, languages, disposition, dispositionParams, embeddedMessage, parts,
                location, md5);
        return mimeDescriptorImpl;
    }
    
    private final long bodyOctets;
    private final String contentDescription;
    private final String contentId;
    private final long lines;
    private final String subType;
    private final String type;
    private final String transferEncoding;
    private final List languages;
    private final Collection headers;
    private final Collection contentTypeParameters;
    private final String disposition;
    private final Map dispositionParams;
    private final MessageResult.MimeDescriptor embeddedMessage;
    private final Collection parts;
    private final String location;
    private final String md5;
    
    public MimeDescriptorImpl(final long bodyOctets, final String contentDescription, final String contentId, 
            final long lines, final String subType, final String type, final String transferEncoding, final Collection headers, 
            final Collection contentTypeParameters, final List languages, String disposition, Map dispositionParams,
            final MimeDescriptor embeddedMessage, final Collection parts, final String location,
            final String md5) {
        super();
        this.type = type;
        this.bodyOctets = bodyOctets;
        this.contentDescription = contentDescription;
        this.contentId = contentId;
        this.lines = lines;
        this.subType = subType;
        this.transferEncoding = transferEncoding;
        this.headers = headers;
        this.contentTypeParameters = contentTypeParameters;
        this.embeddedMessage = embeddedMessage;
        this.parts = parts;
        this.languages = languages;
        this.disposition = disposition;
        this.dispositionParams = dispositionParams;
        this.location = location;
        this.md5 = md5;
    }

    public Iterator contentTypeParameters() {
        return contentTypeParameters.iterator();
    }

    public MessageResult.MimeDescriptor embeddedMessage() {
        return embeddedMessage;
    }

    public long getBodyOctets() {
        return bodyOctets;
    }

    public String getContentDescription() {
        return contentDescription;
    }

    public String getContentID() {
        return contentId;
    }

    public long getLines() {
        return lines;
    }

    public String getMimeSubType() {
        return subType;
    }

    public String getMimeType() {
        return type;
    }

    public String getTransferContentEncoding() {
        return transferEncoding;
    }

    public Iterator headers() {
        return headers.iterator();
    }

    public Iterator parts() {
        return parts.iterator();
    }

    private void addPart(MimeDescriptor descriptor) {
        parts.add(descriptor);
    }
    
    public List getLanguages() {
        return languages;
    }

    public String getDisposition() {
        return disposition;
    }

    public Map getDispositionParams() {
        return dispositionParams;
    }

    public String getContentLocation() {
        return location;
    }

    public String getContentMD5() {
        return md5;
    }

}
