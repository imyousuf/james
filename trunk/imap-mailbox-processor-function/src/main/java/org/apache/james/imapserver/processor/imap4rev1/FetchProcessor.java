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

package org.apache.james.imapserver.processor.imap4rev1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.mail.Flags;
import javax.mail.MessagingException;

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.BodyFetchElement;
import org.apache.james.api.imap.message.FetchData;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.imap.message.request.imap4rev1.FetchRequest;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse.BodyElement;
import org.apache.james.imapserver.processor.base.AbstractImapRequestProcessor;
import org.apache.james.imapserver.processor.base.AuthorizationException;
import org.apache.james.imapserver.processor.base.ImapSessionUtils;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.store.SimpleMessageAttributes;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.MessageResultUtils;
import org.apache.james.mailboxmanager.UnsupportedCriteriaException;
import org.apache.james.mailboxmanager.MessageResult.Content;
import org.apache.james.mailboxmanager.MessageResult.FetchGroup;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;
import org.apache.james.mime4j.field.address.Address;
import org.apache.james.mime4j.field.address.AddressList;
import org.apache.james.mime4j.field.address.DomainList;
import org.apache.james.mime4j.field.address.Group;
import org.apache.james.mime4j.field.address.Mailbox;
import org.apache.james.mime4j.field.address.MailboxList;
import org.apache.james.mime4j.field.address.NamedMailbox;
import org.apache.james.mime4j.field.address.parser.ParseException;
import org.apache.mailet.RFC2822Headers;

public class FetchProcessor extends AbstractImapRequestProcessor {

    public FetchProcessor(final ImapProcessor next,
            final StatusResponseFactory factory) {
        super(next, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof FetchRequest);
    }

    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder)
            throws MailboxException, AuthorizationException, ProtocolException {
        final FetchRequest request = (FetchRequest) message;
        final boolean useUids = request.isUseUids();
        final IdRange[] idSet = request.getIdSet();
        final FetchData fetch = request.getFetch();
        doProcess(useUids, idSet, fetch, session, tag, command, responder);
    }

    private void doProcess(final boolean useUids,
            final IdRange[] idSet, final FetchData fetch, ImapSession session,
            String tag, ImapCommand command, Responder responder) throws MailboxException,
            AuthorizationException, ProtocolException {
        try
        {
            FetchGroup resultToFetch = getFetchGroup(fetch);
            ImapMailbox mailbox = ImapSessionUtils.getMailbox(session);
            for (int i = 0; i < idSet.length; i++) {
                final FetchResponseBuilder builder = new FetchResponseBuilder(getLogger());
                final long highVal;
                final long lowVal;
                if (useUids) {
                    highVal = idSet[i].getHighVal();
                    lowVal = idSet[i].getLowVal();      
                } else {
                    highVal = session.getSelected().uid((int)idSet[i].getHighVal());
                    lowVal = session.getSelected().uid((int) idSet[i].getLowVal()); 
                }
                GeneralMessageSet messageSet = GeneralMessageSetImpl.uidRange(lowVal, highVal);
                final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
                final Iterator it = mailbox.getMessages(messageSet, resultToFetch, mailboxSession);
                while (it.hasNext()) {
                    final MessageResult result = (MessageResult) it.next();
                    final FetchResponse response = builder.build(fetch, result, session, useUids);
                    responder.respond(response);
                }
            }
            unsolicitedResponses(session, responder, useUids);
            okComplete(command, tag, responder);
        } catch (UnsupportedCriteriaException e) {
            no(command, tag, responder, HumanReadableTextKey.UNSUPPORTED_SEARCH_CRITERIA);
        } catch (MailboxManagerException e) {
            throw new MailboxException(e);
        }
    }

    private FetchGroup getFetchGroup(FetchData fetch) {
        int result = FetchGroup.MINIMAL;
        if (fetch.isFlags() || fetch.isSetSeen()) {
            result |= FetchGroup.FLAGS;
        }
        if (fetch.isInternalDate()) {
            result |= FetchGroup.INTERNAL_DATE;
        }
        if (fetch.isSize()) {
            result |= FetchGroup.SIZE;
        }
        if (fetch.isEnvelope()) {
            result |= FetchGroup.HEADERS;
        }
        if (fetch.isBody() || fetch.isBodyStructure()) {
            // TODO: structure
            result |= FetchGroup.MIME_MESSAGE;
        }

        result |= fetchForBodyElements(fetch.getBodyElements());

        return new FetchGroupImpl(result);
    }

    private int fetchForBodyElements(final Collection bodyElements) {
        int result = 0;
        if (bodyElements != null) {
            for (final Iterator it = bodyElements.iterator(); it.hasNext();) {
                final BodyFetchElement element = (BodyFetchElement) it.next();
                final String section = element.getParameters();
                if ("HEADER".equalsIgnoreCase(section)) {
                    result |= FetchGroup.HEADERS;
                } else if (section.startsWith("HEADER.FIELDS.NOT ")) {
                    result |= FetchGroup.HEADERS;
                } else if (section.startsWith("HEADER.FIELDS ")) {
                    result |= FetchGroup.HEADERS;
                } else if (section.equalsIgnoreCase("TEXT")) {
                    ;
                    result |= FetchGroup.BODY_CONTENT;
                } else if (section.length() == 0) {
                    result |= FetchGroup.FULL_CONTENT;
                }
            }
        }
        return result;
    }

    private static final class FetchResponseBuilder {
        private final Logger logger;
        private int msn;
        private Long uid;
        private Flags flags;
        private Date internalDate;
        private Integer size;
        private StringBuffer misc;
        private List elements;
        private FetchResponse.Envelope envelope;
        
        public FetchResponseBuilder(final Logger logger) {
            super();
            this.logger = logger;
        }

        public void reset(int msn) {
            this.msn = msn;
            uid = null;
            flags = null;
            internalDate = null;
            size = null;    
            misc = null;
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
                    size, envelope, misc, elements);
            return result;
        }

        public FetchResponse build(FetchData fetch, MessageResult result,
                ImapSession session, boolean useUids)
                throws MailboxException, ProtocolException {
            ImapMailbox mailbox = ImapSessionUtils.getMailbox(session);
            setMsn(session.getSelected().msn(result.getUid()));
            
            // Check if this fetch will cause the "SEEN" flag to be set on this
            // message
            // If so, update the flags, and ensure that a flags response is included
            // in the response.
            try {
                final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
                boolean ensureFlagsResponse = false;
                if (fetch.isSetSeen()
                        && !result.getFlags().contains(Flags.Flag.SEEN)) {
                    mailbox.setFlags(new Flags(Flags.Flag.SEEN), true, false,
                            GeneralMessageSetImpl.oneUid(result.getUid()), FetchGroupImpl.MINIMAL, mailboxSession);
                    result.getFlags().add(Flags.Flag.SEEN);
                    ensureFlagsResponse = true;
                }

                

                // FLAGS response
                if (fetch.isFlags() || ensureFlagsResponse) {
                    setFlags(result.getFlags());
                }

                // INTERNALDATE response
                if (fetch.isInternalDate()) {
                    setInternalDate(result
                            .getInternalDate());
                }

                // TODO: RFC822.HEADER

                // RFC822.SIZE response
                if (fetch.isSize()) {
                    setSize(result.getSize());
                }

                if (fetch.isEnvelope()) {
                    this.envelope = buildEnvelope(result);
                }
                
                // Only create when needed
                if (fetch.isBody() || fetch.isBodyStructure()) {
                    misc = new StringBuffer();
                    // TODO: replace SimpleMessageAttributes
                    final SimpleMessageAttributes attrs = new SimpleMessageAttributes(
                            result.getMimeMessage(), logger);

                    // BODY response
                    if (fetch.isBody()) {
                        misc.append(" BODY ");
                        misc.append(attrs.getBodyStructure(false));
                    }

                    // BODYSTRUCTURE response
                    if (fetch.isBodyStructure()) {
                        misc.append(" BODYSTRUCTURE ");
                        misc.append(attrs.getBodyStructure(true));
                    }
                }
                // UID response
                if (fetch.isUid()) {
                    setUid(result.getUid());
                }

                // BODY part responses.
                Collection elements = fetch.getBodyElements();
                this.elements = new ArrayList();
                for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
                    BodyFetchElement fetchElement = (BodyFetchElement) iterator.next();
                    final String sectionSpecifier = fetchElement.getParameters();
                    final String responseName = fetchElement.getResponseName();
                    final FetchResponse.BodyElement element 
                        = bodyFetch(result, sectionSpecifier, responseName);
                    if (element != null) {
                        this.elements.add(element);
                    }
                }
                return build();
            } catch (MailboxManagerException mme) {
                throw new MailboxException(mme);
            } catch (MessagingException me) {
                throw new MailboxException(me);
            } catch (ParseException e) {
                logger.debug("Cannot parse header address", e);
                throw new MailboxException("Cannot parse address");
            }
        }

        private FetchResponse.Envelope buildEnvelope(final MessageResult messageResult) throws MessagingException, ParseException {
            final String date = headerValue(messageResult, RFC2822Headers.DATE);
            final String subject = headerValue(messageResult, RFC2822Headers.SUBJECT);
            final FetchResponse.Envelope.Address[] fromAddresses 
                        = buildAddresses(messageResult, RFC2822Headers.FROM);
            final FetchResponse.Envelope.Address[] senderAddresses
                        = buildAddresses(messageResult, RFC2822Headers.SENDER, fromAddresses);
            final FetchResponse.Envelope.Address[] replyToAddresses
                        = buildAddresses(messageResult, RFC2822Headers.REPLY_TO, fromAddresses);
            final FetchResponse.Envelope.Address[] toAddresses 
                        = buildAddresses(messageResult, RFC2822Headers.TO);
            final FetchResponse.Envelope.Address[] ccAddresses 
                        = buildAddresses(messageResult, RFC2822Headers.CC);
            final FetchResponse.Envelope.Address[] bccAddresses 
                        = buildAddresses(messageResult, RFC2822Headers.BCC);
            final String inReplyTo = headerValue(messageResult, RFC2822Headers.IN_REPLY_TO);
            final String messageId = headerValue(messageResult, RFC2822Headers.MESSAGE_ID);
            final FetchResponse.Envelope envelope = new EnvelopeImpl(date, subject, fromAddresses, senderAddresses, 
                    replyToAddresses, toAddresses, ccAddresses, bccAddresses, inReplyTo, messageId);
            return envelope;
        }
        
        private String headerValue(final MessageResult message, final String headerName) throws MessagingException, MailboxManagerException {
            final MessageResult.Header header 
                = MessageResultUtils.getMatching(headerName, message.iterateHeaders());
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

        private FetchResponse.Envelope.Address[] buildAddresses(final MessageResult message, final String headerName, 
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
        
        private FetchResponse.Envelope.Address[] buildAddresses(final MessageResult message, final String headerName) 
                    throws ParseException, MessagingException {
            final MessageResult.Header header = MessageResultUtils.getMatching(headerName, message.iterateHeaders());
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

                        } else if (address instanceof Mailbox) {
                            final Mailbox mailbox = (Mailbox) address;
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
        
        private FetchResponse.Envelope.Address buildMailboxAddress(final Mailbox mailbox) {
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
                final Mailbox mailbox = mailboxList.get(i);
                final FetchResponse.Envelope.Address address = buildMailboxAddress(mailbox);
                addresses.add(address);
            }
            final FetchResponse.Envelope.Address end = endGroup();
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
                String sectionSpecifier, String name) throws MessagingException {
            final FetchResponse.BodyElement result;
            // TODO: section specifier should be fully parsed during parsing phase
            if (sectionSpecifier.length() == 0) {
                final MessageResult.Content fullMessage = messageResult.getFullMessage();
                result = new ContentBodyElement(name, fullMessage);
                
            } else if (sectionSpecifier.equalsIgnoreCase("HEADER")) {
                final Iterator headers = messageResult.iterateHeaders();
                List lines = MessageResultUtils.getAll(headers);
                result = new HeaderBodyElement(name, lines);
                
            } else if (sectionSpecifier.startsWith("HEADER.FIELDS.NOT ")) {
                String[] excludeNames = extractHeaderList(sectionSpecifier,
                        "HEADER.FIELDS.NOT ".length());
                final Iterator headers = messageResult.iterateHeaders();
                List lines = MessageResultUtils.getNotMatching(excludeNames, headers);
                result = new HeaderBodyElement(name, lines);
                
            } else if (sectionSpecifier.startsWith("HEADER.FIELDS ")) {
                String[] includeNames = extractHeaderList(sectionSpecifier,
                        "HEADER.FIELDS ".length());
                final Iterator headers = messageResult.iterateHeaders();
                List lines = MessageResultUtils.getMatching(includeNames, headers);
                result = new HeaderBodyElement(name, lines);
                
            } else if (sectionSpecifier.equalsIgnoreCase("MIME")) {
                // TODO implement
                throw new MailboxManagerException("MIME not yet implemented.");

            } else if (sectionSpecifier.equalsIgnoreCase("TEXT")) {
                final MessageResult.Content messageBody = messageResult.getMessageBody();
                result = new ContentBodyElement(name, messageBody);

            } else {
                // Should be a part specifier followed by a section specifier.
                // See if there's a leading part specifier.
                // If so, get the number, get the part, and call this recursively.
                int dotPos = sectionSpecifier.indexOf('.');
                if (dotPos == -1) {
                    throw new MailboxManagerException("Malformed fetch attribute: "
                            + sectionSpecifier);
                }
                int partNumber = Integer.parseInt(sectionSpecifier.substring(0,
                        dotPos));
                String partSectionSpecifier = sectionSpecifier
                        .substring(dotPos + 1);

                // TODO - get the MimePart of the mimeMessage, and call this method
                // with the new partSectionSpecifier.
                // MimeMessage part;
                // handleBodyFetch( part, partSectionSpecifier, response );
                throw new MailboxManagerException(
                        "Mime parts not yet implemented for fetch.");
            }
            return result;

        }

        // TODO should do this at parse time.
        private String[] extractHeaderList(String headerList, int prefixLen) {
            // Remove the trailing and leading ')('
            String tmp = headerList.substring(prefixLen + 1,
                    headerList.length() - 1);
            String[] headerNames = split(tmp, " ");
            return headerNames;
        }

        private String[] split(String value, String delimiter) {
            ArrayList strings = new ArrayList();
            int startPos = 0;
            int delimPos;
            while ((delimPos = value.indexOf(delimiter, startPos)) != -1) {
                String sub = value.substring(startPos, delimPos);
                strings.add(sub);
                startPos = delimPos + 1;
            }
            String sub = value.substring(startPos);
            strings.add(sub);

            return (String[]) strings.toArray(new String[0]);
        }
    }
    
    private static final class EnvelopeImpl implements FetchResponse.Envelope {

        private final Address[] bcc;
        private final Address[] cc;
        private final String date;
        private final Address[] from;
        private final String inReplyTo;
        private final String messageId;
        private final Address[] replyTo;
        private final Address[] sender;
        private final String subject;                  
        private final Address[] to;
        
        public EnvelopeImpl(final String date, final String subject, final Address[] from, 
                final Address[] sender, final Address[] replyTo, final Address[] to, 
                final Address[] cc, final Address[] bcc, final String inReplyTo, 
                final String messageId) {
            super();
            this.bcc = bcc;
            this.cc = cc;
            this.date = date;
            this.from = from;
            this.inReplyTo = inReplyTo;
            this.messageId = messageId;
            this.replyTo = replyTo;
            this.sender = sender;
            this.subject = subject;
            this.to = to;
        }

        public Address[] getBcc() {
            return bcc;
        }

        public Address[] getCc() {
            return cc;
        }

        public String getDate() {
            return date;
        }

        public Address[] getFrom() {
            return from;
        }

        public String getInReplyTo() {
            return inReplyTo;
        }

        public String getMessageId() {
            return messageId;
        }

        public Address[] getReplyTo() {
            return replyTo;
        }

        public Address[] getSender() {
            return sender;
        }

        public String getSubject() {
            return subject;
        }

        public Address[] getTo() {
            return to;
        }
    }
    
    private static final class AddressImpl implements FetchResponse.Envelope.Address {
        private final String atDomainList;
        private final String hostName;
        private final String mailboxName;
        private final String personalName;

        public AddressImpl(final String atDomainList, final String hostName, final String mailboxName, final String personalName) {
            super();
            this.atDomainList = atDomainList;
            this.hostName = hostName;
            this.mailboxName = mailboxName;
            this.personalName = personalName;
        }

        public String getAtDomainList() {
            return atDomainList;
        }

        public String getHostName() {
            return hostName;
        }

        public String getMailboxName() {
            return mailboxName;
        }

        public String getPersonalName() {
            return personalName;
        }
    }
     
    
    private static final class HeaderBodyElement implements BodyElement {
        private final String name;
        private final List headers;
        private final long size;
        
        public HeaderBodyElement(final String name, final List headers) {
            super();
            this.name = name;
            this.headers = headers;
            size = calculateSize(headers);
        }

        public String getName() {
            return name;
        }
        
        private long calculateSize(List headers) {
            int count = 0;
            for (final Iterator it = headers.iterator(); it.hasNext();) {
                MessageResult.Header header = (MessageResult.Header) it.next();
                count += header.size() + 2;
            }
            return count + 2;
        }

        public long size() {
            return size;
        }

        public void writeTo(WritableByteChannel channel) throws IOException {
            ByteBuffer endLine = ByteBuffer.wrap(ImapConstants.BYTES_LINE_END);
            endLine.rewind();
            for (final Iterator it = headers.iterator(); it.hasNext();) {
                MessageResult.Header header = (MessageResult.Header) it.next();
                header.writeTo(channel);
                while (channel.write(endLine) > 0) {}
                endLine.rewind();
            }
            while (channel.write(endLine) > 0) {}
        }
        
    }
    
    private static final class ContentBodyElement implements BodyElement {
        private final String name;
        private final MessageResult.Content content;
        
        public ContentBodyElement(final String name, final Content content) {
            super();
            this.name = name;
            this.content = content;
        }

        /**
         * @see org.apache.james.imap.message.response.imap4rev1.FetchResponse.BodyElement#getName()
         */
        public String getName() {
            return name;
        }
        
        /**
         * @see org.apache.james.imap.message.response.imap4rev1.FetchResponse.BodyElement#size()
         */
        public long size() {
            return content.size();
        }
        
        /**
         * @see org.apache.james.imap.message.response.imap4rev1.FetchResponse.BodyElement#writeTo(WritableByteChannel)
         */
        public void writeTo(WritableByteChannel channel) throws IOException {
            content.writeTo(channel);
        }
        
        
    }
}
