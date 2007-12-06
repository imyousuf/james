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

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.ImapResponseMessage;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.api.imap.process.ImapProcessor.Responder;
import org.apache.james.imap.message.request.imap4rev1.CopyRequest;
import org.apache.james.imap.message.response.imap4rev1.legacy.CommandCompleteResponse;
import org.apache.james.imapserver.processor.base.AbstractMailboxAwareProcessor;
import org.apache.james.imapserver.processor.base.AuthorizationException;
import org.apache.james.imapserver.processor.base.ImapSessionUtils;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;

public class CopyProcessor extends AbstractMailboxAwareProcessor {

    public CopyProcessor(final ImapProcessor next,
            final MailboxManagerProvider mailboxManagerProvider, final StatusResponseFactory factory) {
        super(next, mailboxManagerProvider, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof CopyRequest);
    }

    protected void doProcess(ImapRequest message,
            ImapSession session, String tag, ImapCommand command, Responder responder)
            throws MailboxException, AuthorizationException, ProtocolException {
        final CopyRequest request = (CopyRequest) message;
        final ImapResponseMessage result = doProcess(request, session, tag,
                command);
        responder.respond(result);
    }

    private ImapResponseMessage doProcess(CopyRequest request,
            ImapSession session, String tag, ImapCommand command)
            throws MailboxException, AuthorizationException, ProtocolException {
        final String mailboxName = request.getMailboxName();
        final IdRange[] idSet = request.getIdSet();
        final boolean useUids = request.isUseUids();
        final ImapResponseMessage result = doProcess(mailboxName, idSet,
                useUids, session, tag, command);
        return result;
    }

    private ImapResponseMessage doProcess(String mailboxName, IdRange[] idSet,
            boolean useUids, ImapSession session, String tag,
            ImapCommand command) throws MailboxException,
            AuthorizationException, ProtocolException {
        ImapMailbox currentMailbox = ImapSessionUtils
                .getMailbox(session);
        try {
            String fullMailboxName = buildFullName(session, mailboxName);
            final MailboxManager mailboxManager = getMailboxManager(session);
            if (!mailboxManager.existsMailbox(fullMailboxName)) {
                MailboxException e = new MailboxException(
                        "Mailbox does not exists");
                e.setResponseCode("TRYCREATE");
                throw e;
            }
            for (int i = 0; i < idSet.length; i++) {
                final long highVal;
                final long lowVal;
                if (useUids) {
                    highVal = idSet[i].getHighVal();
                    lowVal = idSet[i].getLowVal();
                } else {
                    highVal = session.getSelected().uid((int)idSet[i].getHighVal());
                    lowVal = session.getSelected().uid((int)idSet[i].getLowVal());
                }
                GeneralMessageSet messageSet = GeneralMessageSetImpl.uidRange(lowVal, highVal);
                mailboxManager.copyMessages(currentMailbox, messageSet,
                        fullMailboxName);
            }
        } catch (MailboxManagerException e) {
            throw new MailboxException(e);
        }
        final CommandCompleteResponse result = new CommandCompleteResponse(
                command, tag);
        ImapSessionUtils.addUnsolicitedResponses(result, session, useUids);
        return result;
    }
}
