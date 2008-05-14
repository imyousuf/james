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
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.Mailbox;
import org.apache.james.mailboxmanager.util.MailboxEventAnalyser;
import org.apache.james.mailboxmanager.util.UidToMsnConverter;

//TODO: manage mailbox deletion
public class SelectedMailboxSession extends AbstractLogEnabled {
    
    private final MailboxEventAnalyser events;

    private final Mailbox mailbox;
    private final UidToMsnConverter converter;    
    private final MailboxSession mailboxSession;
    
    public SelectedMailboxSession(Mailbox mailbox, Collection uids, 
            MailboxSession mailboxSession) throws MailboxManagerException {
        this.mailbox = mailbox;
        this.mailboxSession = mailboxSession;
        final long sessionId = mailboxSession.getSessionId();
        converter = new UidToMsnConverter(uids);
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
            final Iterator messages = mailbox.getMessages(messageSet, FetchGroupImpl.FLAGS, mailboxSession);
            results.addAll(IteratorUtils.toList(messages));
        }
        return results.iterator();
    }
    
    public void close() throws MailboxManagerException  {
        mailbox.removeListener(events);
        mailbox.removeListener(converter);
    }

    public Mailbox getMailbox() {
        return mailbox;
    }

    public void setSilent(boolean silent) {
        events.setSilentFlagChanges(silent);
    }

    public Iterator expungedMsn() throws MailboxManagerException {
        final Collection results = new ArrayList();
        for  (Iterator it = events.expungedUids(); it.hasNext();) {
            final Long uid = (Long) it.next();
            final long uidValue = uid.longValue();
            final int msn = msn(uidValue);
            final Integer msnObject = new Integer(msn);
            results.add(msnObject);
        }
        
        for  (Iterator it = events.expungedUids(); it.hasNext();) {
            final Long uid = (Long) it.next();
            final long uidValue = uid.longValue();
            converter.expunge(uidValue);
        }
        return results.iterator();
    }
    
    public int msn(long uid) {
        return converter.getMsn(uid);
    }

    public long uid(int msn) {
        return converter.getUid(msn);
    }
}
