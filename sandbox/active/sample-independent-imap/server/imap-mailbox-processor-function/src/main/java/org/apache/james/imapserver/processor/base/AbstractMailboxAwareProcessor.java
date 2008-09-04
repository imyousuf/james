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
package org.apache.james.imapserver.processor.base;

import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;

abstract public class AbstractMailboxAwareProcessor extends AbstractImapRequestProcessor {

    private final MailboxManagerProvider mailboxManagerProvider;
    
    public AbstractMailboxAwareProcessor(final ImapProcessor next, 
            final MailboxManagerProvider mailboxManagerProvider, final StatusResponseFactory factory) {
        super(next, factory);
        this.mailboxManagerProvider = mailboxManagerProvider;
    }
    
    public String buildFullName(final ImapSession session, String mailboxName) throws MailboxManagerException {
        final String user = ImapSessionUtils.getUserName(session);
        return buildFullName(mailboxName, user);
    }

    private String buildFullName(String mailboxName, String user) throws MailboxManagerException {
        if (!mailboxName.startsWith(NAMESPACE_PREFIX)) {
            mailboxName = mailboxManagerProvider.getMailboxManager().resolve(user,mailboxName);
        }
        return mailboxName;
    }

    public MailboxManager getMailboxManager( final ImapSession session ) throws MailboxManagerException {
        // TODO: removed badly implemented and ineffective check that mailbox user matches current user
        // TODO: add check into user login methods
        // TODO: shouldn't need to cache mailbox manager
        // TODO: consolidate API by deleting provider and supply manager direct
        MailboxManager result = (MailboxManager) session.getAttribute( ImapSessionUtils.MAILBOX_MANAGER_ATTRIBUTE_SESSION_KEY );
        if (result == null) {
            result = mailboxManagerProvider.getMailboxManager();
            //
            // Mailbox manager is the primary point of contact
            // But not need to create mailbox until user is logged in
            //
            final String user = ImapSessionUtils.getUserName(session);
            if (user != null)
            {
                result.getMailbox(buildFullName(MailboxManager.INBOX, user), true);
                // TODO: reconsider decision not to sunchronise
                // TODO: mailbox creation is ATM an expensive operation
                // TODO: so caching is required
                // TODO: caching in the session seems like the wrong design decision, though
                // TODO: the mailbox provider should perform any caching that is required
                session.setAttribute( ImapSessionUtils.MAILBOX_MANAGER_ATTRIBUTE_SESSION_KEY, result );
                if (ImapSessionUtils.getMailboxSession(session) == null) {
                    final MailboxSession mailboxSession = (MailboxSession) result.createSession(); 
                    ImapSessionUtils.setMailboxSession(session, mailboxSession);
                }
            }
        }
        return result;
    }
    

}
