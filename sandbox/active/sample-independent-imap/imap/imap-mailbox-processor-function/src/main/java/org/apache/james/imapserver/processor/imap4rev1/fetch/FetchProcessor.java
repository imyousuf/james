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

package org.apache.james.imapserver.processor.imap4rev1.fetch;

import java.util.Collection;
import java.util.Iterator;

import javax.mail.MessagingException;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
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
import org.apache.james.imapserver.processor.base.ImapSessionUtils;
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.MessageRange;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.UnsupportedCriteriaException;
import org.apache.james.mailboxmanager.MessageResult.FetchGroup;
import org.apache.james.mailboxmanager.MessageResult.MimePath;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.impl.MessageRangeImpl;
import org.apache.james.mailboxmanager.mailbox.Mailbox;
import org.apache.james.mime4j.field.address.parser.ParseException;

public class FetchProcessor extends AbstractImapRequestProcessor {

    public FetchProcessor(final ImapProcessor next,
            final StatusResponseFactory factory) {
        super(next, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof FetchRequest);
    }

    protected void doProcess(ImapRequest message, ImapSession session,
            String tag, ImapCommand command, Responder responder) {
        final FetchRequest request = (FetchRequest) message;
        final boolean useUids = request.isUseUids();
        final IdRange[] idSet = request.getIdSet();
        final FetchData fetch = request.getFetch();
        try
        {
            FetchGroup resultToFetch = getFetchGroup(fetch);
            Mailbox mailbox = ImapSessionUtils.getMailbox(session);
            for (int i = 0; i < idSet.length; i++) {
                final FetchResponseBuilder builder = new FetchResponseBuilder(getLogger(), new EnvelopeBuilder(getLogger()));
                final long highVal;
                final long lowVal;
                if (useUids) {
                    highVal = idSet[i].getHighVal();
                    lowVal = idSet[i].getLowVal();      
                } else {
                    highVal = session.getSelected().uid((int)idSet[i].getHighVal());
                    lowVal = session.getSelected().uid((int) idSet[i].getLowVal()); 
                }
                MessageRange messageSet = MessageRangeImpl.uidRange(lowVal, highVal);
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
        } catch (MessagingException e) {
            no(command, tag, responder, e);
        } catch (ParseException e) {
            no(command, tag, responder, HumanReadableTextKey.FAILURE_MAIL_PARSE);
        }
    }

    private FetchGroup getFetchGroup(FetchData fetch) {
        FetchGroupImpl result = new FetchGroupImpl();
        if (fetch.isFlags() || fetch.isSetSeen()) {
            result.or(FetchGroup.FLAGS);
        }
        if (fetch.isInternalDate()) {
            result.or(FetchGroup.INTERNAL_DATE);
        }
        if (fetch.isSize()) {
            result.or(FetchGroup.SIZE);
        }
        if (fetch.isEnvelope()) {
            result.or(FetchGroup.HEADERS);
        }
        if (fetch.isBody() || fetch.isBodyStructure()) {
            result.or(FetchGroup.MIME_DESCRIPTOR);
        }

        Collection bodyElements = fetch.getBodyElements();
        if (bodyElements != null) {
            for (final Iterator it = bodyElements.iterator(); it.hasNext();) {
                final BodyFetchElement element = (BodyFetchElement) it.next();
                final int sectionType = element.getSectionType();
                final int[] path = element.getPath();
                final boolean isBase = (path == null || path.length == 0);
                switch(sectionType) {
                    case BodyFetchElement.CONTENT:
                        if (isBase) {
                            addContent(result, path, isBase, MessageResult.FetchGroup.FULL_CONTENT);
                        } else {
                            addContent(result, path, isBase, MessageResult.FetchGroup.MIME_CONTENT);
                        }
                        break;
                    case BodyFetchElement.HEADER:
                    case BodyFetchElement.HEADER_NOT_FIELDS:
                    case BodyFetchElement.HEADER_FIELDS:
                        addContent(result, path, isBase, MessageResult.FetchGroup.HEADERS);
                        break;
                    case BodyFetchElement.MIME:
                        addContent(result, path, isBase, MessageResult.FetchGroup.MIME_HEADERS);
                        break;
                    case BodyFetchElement.TEXT:
                        addContent(result, path, isBase, MessageResult.FetchGroup.BODY_CONTENT);
                        break;
                }
                
            }
        }
        return result;
    }

    private void addContent(FetchGroupImpl result, final int[] path, final boolean isBase, final int content) {
        if (isBase) {                            
            result.or(content);
        } else {
            MimePath mimePath = new MimePathImpl(path);
            result.addPartContent(mimePath, content);
        }
    }
}
