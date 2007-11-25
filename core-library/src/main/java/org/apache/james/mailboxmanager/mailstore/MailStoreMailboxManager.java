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

package org.apache.james.mailboxmanager.mailstore;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.ListResult;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.GeneralMailbox;
import org.apache.james.mailboxmanager.mailbox.GeneralMailboxSession;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.mailboxmanager.mailbox.MailboxSession;
import org.apache.james.mailboxmanager.manager.MailboxExpression;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.repository.MailRepositoryMailboxSession;
import org.apache.james.services.User;

public class MailStoreMailboxManager extends AbstractLogEnabled implements
        MailboxManager {

    MailstoreMailboxCache mailstoreMailboxCache;
    private User user;
    
    public void close() {
    }

    public void copyMessages(GeneralMailbox from, GeneralMessageSet set,
            String to) throws MailboxManagerException {
        throw new RuntimeException("operation not supported");
    }

    public void createMailbox(String mailboxName)
            throws MailboxManagerException {
        throw new RuntimeException("operation not supported");
    }

    public void deleteMailbox(String mailboxName)
            throws MailboxManagerException {
        throw new RuntimeException("operation not supported");    }

    public boolean existsMailbox(String mailboxName)
            throws MailboxManagerException {
        return true;
    }

    public synchronized GeneralMailboxSession getGeneralMailboxSession(
            String mailboxName) throws MailboxManagerException {
        throw new RuntimeException("operation not supported");
    }

    public ImapMailboxSession getImapMailboxSession(String mailboxName)
            throws MailboxManagerException {
        throw new RuntimeException("operation not supported");
    }

    public MailboxSession getMailboxSession(String mailboxName,
            boolean autoCreate) throws MailboxManagerException {
        return mailstoreMailboxCache.getMailboxSession(mailboxName);
    }

    public ListResult[] list(MailboxExpression expression)
            throws MailboxManagerException {
        return new ListResult[0];
    }

    public void renameMailbox(String from, String to)
            throws MailboxManagerException {
        throw new RuntimeException("operation not supported");
    }

    public void setSubscription(String mailboxName, boolean value)
            throws MailboxManagerException {
        throw new RuntimeException("operation not supported");
    }

    public synchronized void releaseSession(MailRepositoryMailboxSession session)
            throws MailboxManagerException {
        mailstoreMailboxCache.releaseSession(session);
    }

    public void setMailstoreMailboxCache(MailstoreMailboxCache mailstoreMailboxCache) {
        this.mailstoreMailboxCache=mailstoreMailboxCache;
    }

    public void setUser(User user) {
        this.user=user;
    }

    public boolean createInbox(User user) throws MailboxManagerException {
        return false;
    }

}
