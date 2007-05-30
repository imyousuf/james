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

package org.apache.james.experimental.imapserver.processor.imap4rev1;

import java.util.Date;

import javax.mail.internet.MimeMessage;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.experimental.imapserver.AuthorizationException;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.message.request.ImapRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.AppendRequest;
import org.apache.james.experimental.imapserver.message.response.ImapResponseMessage;
import org.apache.james.experimental.imapserver.message.response.imap4rev1.CommandCompleteResponse;
import org.apache.james.experimental.imapserver.processor.ImapProcessor;
import org.apache.james.experimental.imapserver.processor.base.AbstractMailboxAwareProcessor;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;

public class AppendProcessor extends AbstractMailboxAwareProcessor {

    public AppendProcessor(final ImapProcessor next, 
            final MailboxManagerProvider mailboxManagerProvider) {
        super(next, mailboxManagerProvider);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof AppendRequest);
    }

    protected ImapResponseMessage doProcess(ImapRequest message,
            ImapSession session, String tag, ImapCommand command)
            throws MailboxException, AuthorizationException, ProtocolException {
        final AppendRequest request = (AppendRequest) message;
        final ImapResponseMessage result = doProcess(request, session, tag,
                command);
        return result;
    }

    private ImapResponseMessage doProcess(AppendRequest request,
            ImapSession session, String tag, ImapCommand command)
            throws MailboxException, AuthorizationException, ProtocolException {
        final String mailboxName = request.getMailboxName();
        final MimeMessage message = request.getMessage();
        final Date datetime = request.getDatetime();
        final ImapResponseMessage result = doProcess(mailboxName, message,
                datetime, session, tag, command);
        return result;
    }

    private ImapResponseMessage doProcess(String mailboxName,
            MimeMessage message, Date datetime, ImapSession session,
            String tag, ImapCommand command) throws MailboxException,
            AuthorizationException, ProtocolException {
        // TODO: Flags are ignore: check whether the specification says that
        // they should be processed
        ImapMailboxSession mailbox = null;
        try {
            mailboxName = buildFullName(session, mailboxName);
            final MailboxManager mailboxManager = getMailboxManager(session);
            mailbox = mailboxManager.getImapMailboxSession(mailboxName);
        } catch (MailboxManagerException mme) {
            MailboxException me = new MailboxException(mme);
            me.setResponseCode("TRYCREATE");
            throw me;
        }

        try {
            mailbox.appendMessage(message, datetime, 0);
        } catch (MailboxManagerException e) {
            // TODO why not TRYCREATE?
            throw new MailboxException(e);
        }
        return new CommandCompleteResponse(false, command, tag);
    }
}
