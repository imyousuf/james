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
import java.util.Iterator;
import java.util.List;

import javax.mail.search.SearchTerm;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.request.SearchKey;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.imap.message.request.imap4rev1.SearchRequest;
import org.apache.james.imap.message.response.imap4rev1.server.SearchResponse;
import org.apache.james.imapserver.processor.base.AbstractImapRequestProcessor;
import org.apache.james.imapserver.processor.base.AuthorizationException;
import org.apache.james.imapserver.processor.base.ImapSessionUtils;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.SearchQuery;
import org.apache.james.mailboxmanager.MessageResult.FetchGroup;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;

public class SearchProcessor extends AbstractImapRequestProcessor {

    public SearchProcessor(final ImapProcessor next, final StatusResponseFactory factory) {
        super(next, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof SearchRequest);
    }

    protected void doProcess(ImapRequest message,
            ImapSession session, String tag, ImapCommand command, Responder responder)
            throws MailboxException, AuthorizationException, ProtocolException {
        final SearchRequest request = (SearchRequest) message;
        final SearchKey searchKey = request.getSearchKey();
        final boolean useUids = request.isUseUids();
        doProcess(searchKey, useUids, session, tag, command, responder);
    }

    private void doProcess(final SearchKey searchKey,
            final boolean useUids, final ImapSession session, final String tag,
            final ImapCommand command, final Responder responder) throws MailboxException,
            AuthorizationException, ProtocolException {
        ImapMailbox mailbox = ImapSessionUtils.getMailbox(session);
        final FetchGroup resultCode = FetchGroupImpl.MINIMAL;

        final Iterator it;
        try {
            // TODO: implementation
            it = mailbox.search(new SearchQuery(),
                    resultCode, ImapSessionUtils.getMailboxSession(session));
        } catch (MailboxManagerException e) {
            throw new MailboxException(e);
        }

        final List results = new ArrayList();
        while (it.hasNext()) {
            final MessageResult result = (MessageResult) it.next();
            final Long number;
            if (useUids) {
                number = new Long(result.getUid());
            } else {
                final int msn = session.getSelected().msn(result.getUid());
                number = new Long(msn);
            }
            results.add(number);
        }
        
        final int length = results.size();
        long[] ids = new long[length];
        for (int i = 0; i < length; i++) {
            ids[i] = ((Long) results.get(i)).longValue();
        }
        
        final SearchResponse response = new SearchResponse(ids);
        responder.respond(response);
        boolean omitExpunged = (!useUids);
        unsolicitedResponses(session, responder, omitExpunged, useUids);
        okComplete(command, tag, responder);
    }
}
