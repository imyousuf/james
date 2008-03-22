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

package org.apache.james.mailboxmanager.mailbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.mail.internet.MimeMessage;

import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.util.AbstractLogFactoryAware;

/**
 * TODO: This class is only used for mailbox respository integation; consider removal
 */
public abstract class AbstractImapMailbox extends AbstractLogFactoryAware implements ImapMailbox {
    
    public Collection list(MailboxSession mailboxSession) throws MailboxManagerException {
        final Iterator it = getMessages(GeneralMessageSetImpl.all(), FetchGroupImpl.KEY, mailboxSession);
        final Collection result = new ArrayList(100);
        while (it.hasNext()) {
            final MessageResult next = (MessageResult) it.next();
            final String key = (next).getKey();
            result.add(key);
        }
        return result;
    }

    public void remove(String key, MailboxSession mailboxSession) throws MailboxManagerException {
        remove(GeneralMessageSetImpl.oneKey(key), mailboxSession);
    }

    public MimeMessage retrieve(final String key, MailboxSession mailboxSession) throws MailboxManagerException {
        final Iterator it = getMessages(GeneralMessageSetImpl.oneKey(key),
                FetchGroupImpl.MIME_MESSAGE, mailboxSession);
        final MimeMessage result;
        if (it.hasNext()) {
            final MessageResult message = (MessageResult) it.next();
            result = message.getMimeMessage();
        } else {
            result = null;
        }
        return result;
    }

    public String store(MimeMessage message, MailboxSession mailboxSession) throws MailboxManagerException {
        MessageResult result=appendMessage(message, new Date(), FetchGroupImpl.KEY, mailboxSession);
        return result.getKey();
    }
}
