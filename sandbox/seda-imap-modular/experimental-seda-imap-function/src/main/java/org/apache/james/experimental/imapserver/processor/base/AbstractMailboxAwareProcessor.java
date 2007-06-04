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
package org.apache.james.experimental.imapserver.processor.base;

import java.util.List;

import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.processor.ImapProcessor;
import org.apache.james.imap.message.response.base.AbstractImapResponse;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.services.User;

abstract public class AbstractMailboxAwareProcessor extends AbstractUnsolicitedResponsesAwareProcessor {

    // TODO: move into ImapConstants
    public static final String MAILBOX_ATTRIBUTE_SESSION_KEY 
        = "org.apache.james.api.imap.MAILBOX_ATTRIBUTE_SESSION_KEY";
    
    private final MailboxManagerProvider mailboxManagerProvider;
    
    public AbstractMailboxAwareProcessor(final ImapProcessor next, 
            final MailboxManagerProvider mailboxManagerProvider) {
        super(next);
        this.mailboxManagerProvider = mailboxManagerProvider;
    }
    
    public String buildFullName(final ImapSession session, String mailboxName) throws MailboxManagerException {
        User user = session.getUser();
        if (!mailboxName.startsWith(NAMESPACE_PREFIX)) {
            mailboxName = mailboxManagerProvider.getPersonalDefaultNamespace(user).getName()+HIERARCHY_DELIMITER+mailboxName;
        }
        return mailboxName;
    }

    public MailboxManager getMailboxManager( final ImapSession session ) throws MailboxManagerException {
        // TODO: removed badly implemented and ineffective check that mailbox user matches current user
        // TODO: add check into user login methods
        MailboxManager result = (MailboxManager) session.getAttribute( MAILBOX_ATTRIBUTE_SESSION_KEY );
        if (result == null) {
            // TODO: handle null user
            final User user = session.getUser();
            result = mailboxManagerProvider.getMailboxManagerInstance(user);
            result.createInbox(user);
            // TODO: reconsider decision not to sunchronise
            // TODO: mailbox creation is ATM an expensive operation
            // TODO: so caching is required
            // TODO: caching in the session seems like the wrong design decision, though
            // TODO: the mailbox provider should perform any caching that is required
            session.setAttribute( MAILBOX_ATTRIBUTE_SESSION_KEY, result );
        }
        return result;
    }
    
}
