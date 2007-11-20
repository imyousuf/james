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

import java.util.Date;
import java.util.Iterator;

import javax.mail.internet.MimeMessage;

import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.mailbox.GeneralMailbox;
import org.apache.james.mailboxmanager.mailbox.MailboxSession;

public class SessionMailboxWrapper extends NumberStableSessionWrapper implements MailboxSession {

    public SessionMailboxWrapper() {
    }
    
    public SessionMailboxWrapper(GeneralMailbox generalMailbox) throws MailboxManagerException {
        super(generalMailbox);
    }

    public MessageResult appendMessage(MimeMessage message, Date internalDate,
            int result) throws MailboxManagerException {
        return addMsnResult(mailbox.appendMessage(message, internalDate, noMsnResult(result)),result);
    }

    public int getMessageCount() throws MailboxManagerException {
        return mailbox.getMessageCount();
    }

    public int getMessageResultTypes() {
        return mailbox.getMessageResultTypes() | MessageResult.MSN;
    }

    public Iterator getMessages(GeneralMessageSet set, int result)
            throws MailboxManagerException {
        return addMsn(mailbox.getMessages(toUidSet(set),
                noMsnResult(result)));
    }

    public int getMessageSetTypes() {
        return mailbox.getMessageSetTypes() | GeneralMessageSet.TYPE_MSN;
    }

    public String getName() throws MailboxManagerException {
        return mailbox.getName();
    }

    public MessageResult updateMessage(GeneralMessageSet messageSet, MimeMessage message, int result) throws MailboxManagerException {
        return addMsnResult(mailbox.updateMessage(toUidSet(messageSet), message, noMsnResult(result)),result);
    }

    public boolean isWriteable() {
        return true;
    }

    public void remove(GeneralMessageSet set) throws MailboxManagerException {
        mailbox.remove(toUidSet(set));
    }

    public long getSessionId() {
        return mailbox.getSessionId();
    }

}
