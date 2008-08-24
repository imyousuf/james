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
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse.ResponseCode;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.imap.message.request.imap4rev1.CopyRequest;
import org.apache.james.imapserver.processor.base.AbstractMailboxAwareProcessor;
import org.apache.james.imapserver.processor.base.ImapSessionUtils;
import org.apache.james.mailboxmanager.MessageRange;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.impl.MessageRangeImpl;
import org.apache.james.mailboxmanager.mailbox.Mailbox;
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
            ImapSession session, String tag, ImapCommand command, Responder responder) {
        final CopyRequest request = (CopyRequest) message;
        final String mailboxName = request.getMailboxName();
        final IdRange[] idSet = request.getIdSet();
        final boolean useUids = request.isUseUids();
        Mailbox currentMailbox = ImapSessionUtils
                .getMailbox(session);
        try {
            String fullMailboxName = buildFullName(session, mailboxName);
            final MailboxManager mailboxManager = getMailboxManager(session);
            final boolean mailboxExists = mailboxManager.existsMailbox(fullMailboxName);
            if (!mailboxExists) {
                no(command, tag, responder, 
                        HumanReadableTextKey.FAILURE_NO_SUCH_MAILBOX, ResponseCode.tryCreate());
            } else {
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
                    MessageRange messageSet = MessageRangeImpl.uidRange(lowVal, highVal);
                    final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
                    mailboxManager.copyMessages(messageSet, currentMailbox.getName(), 
                            fullMailboxName, mailboxSession);
                }
                unsolicitedResponses(session, responder, useUids);
                okComplete(command, tag, responder);
            }
        } catch (MailboxManagerException e) {
            no(command, tag, responder, e);
        }
    }
}
