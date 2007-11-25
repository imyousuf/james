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
import org.apache.james.imapserver.processor.base.AbstractImapRequestProcessor;
import org.apache.james.imapserver.processor.base.AuthorizationException;
import org.apache.james.imapserver.processor.base.ImapSessionUtils;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.store.SimpleMessageAttributes;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.MessageResultUtils;
import org.apache.james.mailboxmanager.UnsupportedCriteriaException;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;

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
            int resultToFetch = getNeededMessageResult(fetch);
            ImapMailboxSession mailbox = ImapSessionUtils.getMailbox(session);
            for (int i = 0; i < idSet.length; i++) {
                final FetchResponseBuilder builder = new FetchResponseBuilder(getLogger());
                GeneralMessageSet messageSet = GeneralMessageSetImpl.range(idSet[i]
                        .getLowVal(), idSet[i].getHighVal(), useUids);
                final Iterator it = mailbox.getMessages(messageSet, resultToFetch);
                while (it.hasNext()) {
                    final MessageResult result = (MessageResult) it.next();
                    final FetchResponse response = builder.build(fetch, result, mailbox, useUids);
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

    private int getNeededMessageResult(FetchData fetch) {
        int result = MessageResult.MSN;
        if (fetch.isFlags() || fetch.isSetSeen()) {
            result |= MessageResult.FLAGS;
        }
        if (fetch.isInternalDate()) {
            result |= MessageResult.INTERNAL_DATE;
        }
        if (fetch.isSize()) {
            result |= MessageResult.SIZE;
        }
        if (fetch.isUid()) {
            result |= MessageResult.UID;
        }
        if (fetch.isEnvelope() || fetch.isBody() || fetch.isBodyStructure()) {
            // TODO: structure
            // result |= MessageResult.ENVELOPE;
            result |= MessageResult.MIME_MESSAGE;
        }

        result |= fetchForBodyElements(fetch.getBodyElements());

        return result;
    }

    private int fetchForBodyElements(final Collection bodyElements) {
        int result = 0;
        if (bodyElements != null) {
            for (final Iterator it = bodyElements.iterator(); it.hasNext();) {
                final BodyFetchElement element = (BodyFetchElement) it.next();
                final String section = element.getParameters();
                if ("HEADER".equalsIgnoreCase(section)) {
                    result |= MessageResult.HEADERS;
                } else if (section.startsWith("HEADER.FIELDS.NOT ")) {
                    result |= MessageResult.HEADERS;
                } else if (section.startsWith("HEADER.FIELDS ")) {
                    result |= MessageResult.HEADERS;
                } else if (section.equalsIgnoreCase("TEXT")) {
                    ;
                    result |= MessageResult.BODY_CONTENT;
                } else if (section.length() == 0) {
                    result |= MessageResult.FULL_CONTENT;
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
        private StringBuffer elements;
        
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
                    size, misc, elements);
            return result;
        }

        public FetchResponse build(FetchData fetch, MessageResult result,
                ImapMailboxSession mailbox, boolean useUids)
                throws MailboxException, ProtocolException {
            
            setMsn(result);
            
            // Check if this fetch will cause the "SEEN" flag to be set on this
            // message
            // If so, update the flags, and ensure that a flags response is included
            // in the response.
            try {
                boolean ensureFlagsResponse = false;
                if (fetch.isSetSeen()
                        && !result.getFlags().contains(Flags.Flag.SEEN)) {
                    mailbox.setFlags(new Flags(Flags.Flag.SEEN), true, false,
                            GeneralMessageSetImpl.oneUid(result.getUid()), MessageResult.NOTHING);
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

                // Only create when needed
                if (fetch.isEnvelope() || fetch.isBody() || fetch.isBodyStructure()) {
                    misc = new StringBuffer();
                    // TODO: replace SimpleMessageAttributes
                    final SimpleMessageAttributes attrs = new SimpleMessageAttributes(
                            result.getMimeMessage(), logger);

                    // ENVELOPE response
                    if (fetch.isEnvelope()) {
                        misc.append(" ENVELOPE ");
                        misc.append(attrs.getEnvelope());
                    }

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
                this.elements = new StringBuffer();
                for (Iterator iterator = elements.iterator(); iterator.hasNext();) {
                    BodyFetchElement fetchElement = (BodyFetchElement) iterator
                            .next();
                    this.elements.append(ImapConstants.SP);
                    this.elements.append(fetchElement.getResponseName());
                    this.elements.append(ImapConstants.SP);

                    // Various mechanisms for returning message body.
                    String sectionSpecifier = fetchElement.getParameters();

                    try {
                        handleBodyFetch(result, sectionSpecifier, this.elements);
                    } catch (MessagingException e) {
                        throw new MailboxException(e.getMessage(), e);
                    }
                }
                return build();
            } catch (MailboxManagerException mme) {
                throw new MailboxException(mme);
            } catch (MessagingException me) {
                throw new MailboxException(me);
            }
        }

        private void setSize(int size) {
            this.size = new Integer(size);
        }

        public void setInternalDate(Date internalDate) {
            this.internalDate = internalDate;
        }

        private void setMsn(MessageResult result) {
            final int msn = result.getMsn();
            reset(msn);
        }

        private void handleBodyFetch(final MessageResult result,
                String sectionSpecifier, StringBuffer response)
                throws ProtocolException, MessagingException {
            // TODO: section specifier should be fully parsed during parsing phase
            if (sectionSpecifier.length() == 0) {
                final MessageResult.Content fullMessage = result.getFullMessage();
                addLiteralContent(fullMessage, response);
            } else if (sectionSpecifier.equalsIgnoreCase("HEADER")) {
                final Iterator headers = result.iterateHeaders();
                List lines = MessageResultUtils.getAll(headers);
                addHeaders(lines, response);
            } else if (sectionSpecifier.startsWith("HEADER.FIELDS.NOT ")) {
                String[] excludeNames = extractHeaderList(sectionSpecifier,
                        "HEADER.FIELDS.NOT ".length());
                final Iterator headers = result.iterateHeaders();
                List lines = MessageResultUtils.getNotMatching(excludeNames, headers);
                addHeaders(lines, response);
            } else if (sectionSpecifier.startsWith("HEADER.FIELDS ")) {
                String[] includeNames = extractHeaderList(sectionSpecifier,
                        "HEADER.FIELDS ".length());
                final Iterator headers = result.iterateHeaders();
                List lines = MessageResultUtils.getMatching(includeNames, headers);
                addHeaders(lines, response);
            } else if (sectionSpecifier.equalsIgnoreCase("MIME")) {
                // TODO implement
                throw new ProtocolException("MIME not yet implemented.");

            } else if (sectionSpecifier.equalsIgnoreCase("TEXT")) {
                final MessageResult.Content messageBody = result.getMessageBody();
                addLiteralContent(messageBody, response);

            } else {
                // Should be a part specifier followed by a section specifier.
                // See if there's a leading part specifier.
                // If so, get the number, get the part, and call this recursively.
                int dotPos = sectionSpecifier.indexOf('.');
                if (dotPos == -1) {
                    throw new ProtocolException("Malformed fetch attribute: "
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
                throw new ProtocolException(
                        "Mime parts not yet implemented for fetch.");
            }

        }

        private void addLiteralContent(final MessageResult.Content content,
                final StringBuffer response) throws MessagingException {
            response.append('{');
            final long length = content.size();
            response.append(length); // TODO JD addLiteral: why was it
                                        // bytes.length +1 here?
            response.append('}');
            response.append("\r\n");
            content.writeTo(response);
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

        private void addHeaders(List headerLines, StringBuffer response)
                throws MessagingException {
            int count = 0;
            for (final Iterator it = headerLines.iterator(); it.hasNext();) {
                MessageResult.Header header = (MessageResult.Header) it.next();
                count += header.size() + 2;
            }
            response.append('{');
            response.append(count + 2);
            response.append('}');
            response.append("\r\n");

            for (final Iterator it = headerLines.iterator(); it.hasNext();) {
                MessageResult.Header header = (MessageResult.Header) it.next();
                header.writeTo(response);
                response.append("\r\n");
            }
            response.append("\r\n");
        }
        
    }
}
