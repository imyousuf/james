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

import java.util.Iterator;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.api.imap.process.SelectedImapMailbox;
import org.apache.james.imap.message.request.imap4rev1.ExpungeRequest;
import org.apache.james.imapserver.processor.base.AbstractImapRequestProcessor;
import org.apache.james.imapserver.processor.base.ImapSessionUtils;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;

public class ExpungeProcessor extends AbstractImapRequestProcessor {

    public ExpungeProcessor(final ImapProcessor next,
            final MailboxManagerProvider mailboxManagerProvider, final StatusResponseFactory factory) {
        super(next, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof ExpungeRequest);
    }

    protected void doProcess(ImapRequest message,
            ImapSession session, String tag, ImapCommand command, Responder responder) {
        ImapMailbox mailbox = ImapSessionUtils.getMailbox(session);
        if (!mailbox.isWriteable()) {
            no(command, tag, responder, HumanReadableTextKey.MAILBOX_IS_READ_ONLY);
        } else {
            try {
                final Iterator it = mailbox.expunge(GeneralMessageSetImpl.all(),
                        FetchGroupImpl.MINIMAL, ImapSessionUtils.getMailboxSession(session));
                final SelectedImapMailbox mailboxSession = session.getSelected();
                if (mailboxSession != null) {
                    while(it.hasNext()) {
                        final MessageResult result = (MessageResult) it.next();
                        final long uid = result.getUid();
                        mailboxSession.removeRecent(uid);
                    }
                }
                unsolicitedResponses(session, responder, false);
                okComplete(command, tag, responder);
            } catch (MailboxManagerException e) {
                no(command, tag, responder, e);
            }
        }
    }
}
