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

import javax.mail.Flags;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.response.ImapResponseMessage;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.api.imap.process.SelectedImapMailbox;
import org.apache.james.imap.message.response.imap4rev1.legacy.ExamineAndSelectResponse;
import org.apache.james.imapserver.processor.base.AbstractMailboxAwareProcessor;
import org.apache.james.imapserver.processor.base.AuthorizationException;
import org.apache.james.imapserver.processor.base.ImapSessionUtils;
import org.apache.james.imapserver.processor.base.SelectedMailboxSessionImpl;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MailboxNotFoundException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;

abstract public class AbstractMailboxSelectionProcessor extends
        AbstractMailboxAwareProcessor {

    final StatusResponseFactory statusResponseFactory;
    
    public AbstractMailboxSelectionProcessor(final ImapProcessor next,
            final MailboxManagerProvider mailboxManagerProvider, 
            final StatusResponseFactory statusResponseFactory) {
        super(next, mailboxManagerProvider);
        this.statusResponseFactory = statusResponseFactory;
    }

    protected final ImapResponseMessage doProcess(String mailboxName,
            boolean isExamine, ImapSession session, String tag,
            ImapCommand command) throws MailboxException,
            AuthorizationException, ProtocolException {
        ImapResponseMessage result;
        session.deselect();
        try {
            String fullMailboxName = buildFullName(session, mailboxName);
            selectMailbox(fullMailboxName, session, isExamine);
            ImapMailboxSession mailbox = ImapSessionUtils.getMailbox(session);
            result = process(isExamine, tag, command, mailbox);
        } catch (MailboxNotFoundException e) {
            result = statusResponseFactory.taggedNo(tag, command, 
                    HumanReadableTextKey.FAILURE_NO_SUCH_MAILBOX);
        } catch (MailboxManagerException e) {
            throw new MailboxException(e);
        }
        return result;
    }

    private ImapResponseMessage process(boolean isExamine, String tag, ImapCommand command, ImapMailboxSession mailbox) 
                throws MailboxException, MailboxManagerException {
        ImapResponseMessage result;
        // TODO: compact this into a single API call for meta-data about the repository
        final Flags permanentFlags = mailbox.getPermanentFlags();
        final boolean writeable = mailbox.isWriteable() && !isExamine;
        final boolean resetRecent = !isExamine;
        final int recentCount = mailbox.getRecentCount(resetRecent);
        final long uidValidity = mailbox.getUidValidity();
        final MessageResult firstUnseen = mailbox
        .getFirstUnseen(MessageResult.MSN);
        final int messageCount = mailbox.getMessageCount();
        final int msn;
        if (firstUnseen == null) {
            msn = -1;
        } else {
            msn = firstUnseen.getMsn();
        }
        result = new ExamineAndSelectResponse(command, permanentFlags,
                writeable, recentCount, uidValidity, msn, messageCount, tag);
        return result;
    }

    private boolean selectMailbox(String mailboxName, ImapSession session,
            boolean readOnly) throws MailboxException, MailboxManagerException {
        final MailboxManager mailboxManager = getMailboxManager(session);
        final ImapMailboxSession mailbox = mailboxManager
                .getImapMailboxSession(mailboxName);

        if (!mailbox.isSelectable()) {
            throw new MailboxException("Nonselectable mailbox.");
        }

        SelectedImapMailbox sessionMailbox = new SelectedMailboxSessionImpl(
                mailbox, session);
        session.selected(sessionMailbox);
        session.setAttribute(
                ImapSessionUtils.SELECTED_MAILBOX_ATTRIBUTE_SESSION_KEY,
                mailbox);
        return readOnly;
    }
}
