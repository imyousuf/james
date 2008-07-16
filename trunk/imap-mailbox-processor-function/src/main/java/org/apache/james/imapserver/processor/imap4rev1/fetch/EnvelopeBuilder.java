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
import java.util.List;

import javax.mail.MessagingException;

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse;
import org.apache.james.mailboxmanager.Headers;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.util.MessageResultUtils;
import org.apache.james.mime4j.field.address.Address;
import org.apache.james.mime4j.field.address.AddressList;
import org.apache.james.mime4j.field.address.DomainList;
import org.apache.james.mime4j.field.address.Group;
import org.apache.james.mime4j.field.address.MailboxList;
import org.apache.james.mime4j.field.address.NamedMailbox;
import org.apache.james.mime4j.field.address.parser.ParseException;
import org.apache.mailet.RFC2822Headers;

final class EnvelopeBuilder {
    private final Logger logger;
    
    public EnvelopeBuilder(final Logger logger) {
        super();
        this.logger = logger;
    }
   
    public FetchResponse.Envelope buildEnvelope(final Headers headers) throws MessagingException, ParseException {
        final String date = headerValue(headers, RFC2822Headers.DATE);
        final String subject = headerValue(headers, RFC2822Headers.SUBJECT);
        final FetchResponse.Envelope.Address[] fromAddresses 
                    = buildAddresses(headers, RFC2822Headers.FROM);
        final FetchResponse.Envelope.Address[] senderAddresses
                    = buildAddresses(headers, RFC2822Headers.SENDER, fromAddresses);
        final FetchResponse.Envelope.Address[] replyToAddresses
                    = buildAddresses(headers, RFC2822Headers.REPLY_TO, fromAddresses);
        final FetchResponse.Envelope.Address[] toAddresses 
                    = buildAddresses(headers, RFC2822Headers.TO);
        final FetchResponse.Envelope.Address[] ccAddresses 
                    = buildAddresses(headers, RFC2822Headers.CC);
        final FetchResponse.Envelope.Address[] bccAddresses 
                    = buildAddresses(headers, RFC2822Headers.BCC);
        final String inReplyTo = headerValue(headers, RFC2822Headers.IN_REPLY_TO);
        final String messageId = headerValue(headers, RFC2822Headers.MESSAGE_ID);
        final FetchResponse.Envelope envelope = new EnvelopeImpl(date, subject, fromAddresses, senderAddresses, 
                replyToAddresses, toAddresses, ccAddresses, bccAddresses, inReplyTo, messageId);
        return envelope;
    }
    
    private String headerValue(final Headers message, final String headerName) throws MessagingException, MailboxManagerException {
        final MessageResult.Header header 
            = MessageResultUtils.getMatching(headerName, message.headers());
        final String result;
        if (header == null) {
            result = null;
        } else {
            final String value = header.getValue();
            if (value == null || "".equals(value)) {
                result = null;
            } else {
                result = value;
            }
        }
        return result;
    }

    private FetchResponse.Envelope.Address[] buildAddresses(final Headers message, final String headerName, 
            final FetchResponse.Envelope.Address[] defaults) throws ParseException, MessagingException {
        final FetchResponse.Envelope.Address[] results;
        final FetchResponse.Envelope.Address[] addresses = buildAddresses(message, headerName);
        if (addresses == null) {
            results = defaults;
        } else {
            results = addresses;
        }
        return results;
    }
    
    private FetchResponse.Envelope.Address[] buildAddresses(final Headers message, final String headerName) 
                throws ParseException, MessagingException {
        final MessageResult.Header header = MessageResultUtils.getMatching(headerName, message.headers());
        final FetchResponse.Envelope.Address[] results;
        if (header == null) {
            results = null;
        } else {
            final String value = header.getValue();
            if ("".equals(value.trim())) {
                results  = null;
            } else {
                final AddressList addressList = AddressList.parse(value);
                final int size = addressList.size();
                final List addresses = new ArrayList(size);
                for (int i=0;i<size;i++) {
                    final Address address = addressList.get(i);
                    if (address instanceof Group) {
                        final Group group = (Group) address;
                        addAddresses(group, addresses);

                    } else if (address instanceof org.apache.james.mime4j.field.address.Mailbox) {
                        final org.apache.james.mime4j.field.address.Mailbox mailbox 
                            = (org.apache.james.mime4j.field.address.Mailbox) address;
                        final FetchResponse.Envelope.Address mailboxAddress = buildMailboxAddress(mailbox);
                        addresses.add(mailboxAddress);

                    } else {
                        logger.warn("Unknown address type");
                    }
                }

                results = (FetchResponse.Envelope.Address[]) 
                addresses.toArray(FetchResponse.Envelope.Address.EMPTY);
            }
        }
        return results;
    }
    
    private FetchResponse.Envelope.Address buildMailboxAddress(final org.apache.james.mime4j.field.address.Mailbox mailbox) {
        final String name;
        if (mailbox instanceof NamedMailbox) {
            final NamedMailbox namedMailbox = (NamedMailbox) mailbox;
            name = namedMailbox.getName();
        } else {
            name = null;
        }
        final String domain = mailbox.getDomain();
        final DomainList route = mailbox.getRoute();
        final String atDomainList;
        if (route == null) {
            atDomainList = null;
        } else {
            atDomainList = route.toRouteString();
        }
        final String localPart = mailbox.getLocalPart();
        final FetchResponse.Envelope.Address result = buildMailboxAddress(name, atDomainList, localPart, domain);
        return result;
    }
    
    private void addAddresses(final Group group, final List addresses) {
        final String groupName = group.getName();
        final FetchResponse.Envelope.Address start = startGroup(groupName);
        addresses.add(start);
        final MailboxList mailboxList = group.getMailboxes();
        for (int i=0;i<mailboxList.size();i++) {
            final org.apache.james.mime4j.field.address.Mailbox mailbox = mailboxList.get(i);
            final FetchResponse.Envelope.Address address = buildMailboxAddress(mailbox);
            addresses.add(address);
        }
        final FetchResponse.Envelope.Address end = endGroup();
        addresses.add(end);
    }
    
    private FetchResponse.Envelope.Address startGroup(String groupName) {
        final FetchResponse.Envelope.Address result 
        = new AddressImpl(null, null, groupName, null);
        return result;
    }
    
    private FetchResponse.Envelope.Address endGroup() {
        final FetchResponse.Envelope.Address result 
        = new AddressImpl(null, null, null, null);
        return result;
    }
                
    private FetchResponse.Envelope.Address buildMailboxAddress(String name, String atDomainList, 
                                                        String mailbox, String domain) {
        final FetchResponse.Envelope.Address result 
            = new AddressImpl(atDomainList, domain, mailbox, name);
        return result;
    }
}