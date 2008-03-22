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

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.message.StatusDataItems;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.imap.message.request.imap4rev1.StatusRequest;
import org.apache.james.imap.message.response.imap4rev1.server.STATUSResponse;
import org.apache.james.imapserver.processor.base.AbstractMailboxAwareProcessor;
import org.apache.james.imapserver.processor.base.AuthorizationException;
import org.apache.james.imapserver.processor.base.ImapSessionUtils;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;

public class StatusProcessor extends AbstractMailboxAwareProcessor {

    public StatusProcessor(final ImapProcessor next,
            final MailboxManagerProvider mailboxManagerProvider, final StatusResponseFactory factory) {
        super(next, mailboxManagerProvider, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof StatusRequest);
    }

    protected void doProcess(ImapRequest message,
            ImapSession session, String tag, ImapCommand command, Responder responder)
            throws MailboxException, AuthorizationException, ProtocolException {
        final StatusRequest request = (StatusRequest) message;
        final String mailboxName = request.getMailboxName();
        final StatusDataItems statusDataItems = request.getStatusDataItems();
        final Logger logger = getLogger();
        final MailboxSession mailboxSession = ImapSessionUtils.getMailboxSession(session);
        
        try {
            String fullMailboxName = buildFullName(session, mailboxName);

            if (logger != null && logger.isDebugEnabled()) {
                logger.debug("Status called on mailbox named " + mailboxName
                        + " (" + fullMailboxName + ")");
            }

            final MailboxManager mailboxManager = getMailboxManager(session);
            final ImapMailbox mailbox = mailboxManager
                    .getImapMailbox(fullMailboxName, false);
            
            final Long messages = messages(statusDataItems, mailboxSession, mailbox);
            final Long recent = recent(statusDataItems, mailboxSession, mailbox);
            final Long uidNext = uidNext(statusDataItems, mailboxSession, mailbox);
            final Long uidValidity = uidValidity(statusDataItems, mailboxSession, mailbox);
            final Long unseen = unseen(statusDataItems, mailboxSession, mailbox);
            
            final STATUSResponse response = new STATUSResponse(messages, recent, uidNext, uidValidity, unseen, mailboxName);
            responder.respond(response);
            
        } catch (MailboxManagerException e) {
            if (logger != null && logger.isDebugEnabled()) {
                logger.debug("STATUS command failed: ", e);
            }
            throw new MailboxException(e);
        }
        
        unsolicitedResponses(session, responder, false);
        okComplete(command, tag, responder);
    }

    private Long unseen(final StatusDataItems statusDataItems, final MailboxSession mailboxSession, final ImapMailbox mailbox) throws MailboxManagerException {
        final Long unseen;
        if (statusDataItems.isUnseen()) {
            final int unseenCountValue = mailbox.getUnseenCount(mailboxSession);
            unseen = new Long(unseenCountValue);
        } else {
            unseen = null;
        }
        return unseen;
    }

    private Long uidValidity(final StatusDataItems statusDataItems, final MailboxSession mailboxSession, final ImapMailbox mailbox) throws MailboxManagerException {
        final Long uidValidity;
        if (statusDataItems.isUidValidity()) {
            final long uidValidityValue = mailbox.getUidValidity(mailboxSession);
            uidValidity = new Long(uidValidityValue);
        } else {
            uidValidity = null;
        }
        return uidValidity;
    }

    private Long uidNext(final StatusDataItems statusDataItems, final MailboxSession mailboxSession, final ImapMailbox mailbox) throws MailboxManagerException {
        final Long uidNext;
        if (statusDataItems.isUidNext()) {
            final long uidNextValue = mailbox.getUidNext(mailboxSession);
            uidNext = new Long(uidNextValue);
        } else {
            uidNext = null;
        }
        return uidNext;
    }

    private Long recent(final StatusDataItems statusDataItems, final MailboxSession mailboxSession, final ImapMailbox mailbox) throws MailboxManagerException {
        final Long recent;
        if (statusDataItems.isRecent()) {
            final int recentCount = mailbox.recent(false, mailboxSession).length;
            recent = new Long(recentCount);
        } else {
            recent = null;
        }
        return recent;
    }

    private Long messages(final StatusDataItems statusDataItems, final MailboxSession mailboxSession, final ImapMailbox mailbox) throws MailboxManagerException {
        final Long messages;
        if (statusDataItems.isMessages()) {
            final int messageCount = mailbox.getMessageCount(mailboxSession);
            messages = new Long(messageCount);
        } else {
            messages = null;
        }
        return messages;
    }
}
