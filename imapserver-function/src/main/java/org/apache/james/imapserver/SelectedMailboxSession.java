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

package org.apache.james.imapserver;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.commons.collections.IteratorUtils;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.mailboxmanager.util.MailboxEventAnalyser;

//TODO: manage mailbox deletion
public class SelectedMailboxSession extends AbstractLogEnabled {
    
    private ImapMailboxSession mailbox;
    
    private final MailboxEventAnalyser events;

    public SelectedMailboxSession(ImapMailboxSession mailbox) throws MailboxManagerException {
        this.mailbox = mailbox;
        final long sessionId = mailbox.getSessionId();
        events = new MailboxEventAnalyser(sessionId);
        mailbox.addListener(events);
    }

    public void deselect() {
        mailbox.removeListener(events);
        mailbox = null;
    }

    public boolean isSizeChanged() {
        return events.isSizeChanged();
    }

    public void reset() {
        events.reset();
    }
    
    public Iterator getFlagUpdates() throws MailboxManagerException {
        List results = new ArrayList();
        for (final Iterator it = events.flagUpdateUids(); it.hasNext();) {
            Long uid = (Long) it.next();
            GeneralMessageSet messageSet = GeneralMessageSetImpl.oneUid(uid.longValue());
            final Iterator messages = mailbox.getMessages(messageSet, MessageResult.FLAGS | MessageResult.MSN);
            results.addAll(IteratorUtils.toList(messages));
        }
        return results.iterator();
    }
    
    public void close() throws MailboxManagerException  {
        mailbox.removeListener(events);
        mailbox.close();
        mailbox=null;
    }

    public ImapMailboxSession getMailbox() {
        return mailbox;
    }

    public void setSilent(boolean silent) {
        events.setSilentFlagChanges(silent);
    }
}
