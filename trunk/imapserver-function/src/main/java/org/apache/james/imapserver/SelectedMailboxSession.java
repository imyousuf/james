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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.commons.collections.IteratorUtils;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;
import org.apache.james.mailboxmanager.tracking.UidToMsnConverter;
import org.apache.james.mailboxmanager.util.MailboxEventAnalyser;

//TODO: manage mailbox deletion
public class SelectedMailboxSession extends AbstractLogEnabled {
    
    private final MailboxEventAnalyser events;

    private final ImapMailbox mailbox;
    private final UidToMsnConverter converter;    
    
    public SelectedMailboxSession(ImapMailbox mailbox, Collection uids) throws MailboxManagerException {
        this.mailbox = mailbox;
        converter = new UidToMsnConverter(mailbox.getSessionId(), uids);
        final long sessionId = mailbox.getSessionId();
        events = new MailboxEventAnalyser(sessionId);
        mailbox.addListener(events);
        mailbox.addListener(converter);
    }

    public void deselect() {
        mailbox.removeListener(events);
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
            final Iterator messages = mailbox.getMessages(messageSet, MessageResult.FLAGS);
            results.addAll(IteratorUtils.toList(messages));
        }
        return results.iterator();
    }
    
    public void close() throws MailboxManagerException  {
        mailbox.removeListener(events);
        mailbox.removeListener(converter);
    }

    public ImapMailbox getMailbox() {
        return mailbox;
    }

    public void setSilent(boolean silent) {
        events.setSilentFlagChanges(silent);
    }

    public Iterator getExpungedEvents(boolean reset) throws MailboxManagerException {
        return converter.getExpungedEvents(reset);
    }
    
    public int msn(long uid) {
        return converter.getMsn(uid);
    }

    public long uid(int msn) {
        return converter.getUid(msn);
    }
}
