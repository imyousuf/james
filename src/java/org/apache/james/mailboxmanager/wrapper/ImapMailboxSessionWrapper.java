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

package org.apache.james.mailboxmanager.wrapper;

import javax.mail.search.SearchTerm;

import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.Quota;
import org.apache.james.mailboxmanager.acl.MailboxRights;
import org.apache.james.mailboxmanager.impl.MailboxEventDispatcher;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.mailboxmanager.mailbox.SearchableMailbox;

public class ImapMailboxSessionWrapper extends FlaggedSessionMailboxWrapper
        implements ImapMailboxSession {

    protected MailboxEventDispatcher eventDispatcher = new MailboxEventDispatcher();

    public ImapMailboxSessionWrapper(ImapMailbox imapMailbox) throws MailboxManagerException {
        super(imapMailbox);
    }

    public MailboxRights myRights() {
        // TODO Auto-generated method stub
        return null;
    }

    public Quota[] getQuota() {
        // TODO Auto-generated method stub
        return null;
    }



    public boolean isSelectable() {
        // TODO Auto-generated method stub
        return true;
    }

    public long getUidValidity() throws MailboxManagerException {
        return ((ImapMailbox) mailbox).getUidValidity();
    }

    public long getUidNext() throws MailboxManagerException {
        return ((ImapMailbox) mailbox).getUidNext();
    }

    public MessageResult[] search(GeneralMessageSet set, SearchTerm searchTerm, int result) throws MailboxManagerException {
        return addMsnToResults(((SearchableMailbox)mailbox).search(set, searchTerm, noMsnResult(result)),result);
    }





}
