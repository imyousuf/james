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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.mail.Flags;
import javax.mail.MessagingException;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.api.imap.process.SelectedImapMailbox;
import org.apache.james.imap.message.response.imap4rev1.ExistsResponse;
import org.apache.james.imap.message.response.imap4rev1.ExpungeResponse;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse;
import org.apache.james.imap.message.response.imap4rev1.RecentResponse;
import org.apache.james.imap.message.response.imap4rev1.status.UntaggedNoResponse;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.FetchGroupImpl;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.Mailbox;
import org.apache.james.mailboxmanager.tracking.UidToMsnConverter;
import org.apache.james.mailboxmanager.util.MailboxEventAnalyser;

//TODO: deal with deleted or renamed mailboxes
public class SelectedMailboxSessionImpl extends AbstractLogEnabled implements SelectedImapMailbox {

    private final Mailbox mailbox;
    
    private final MailboxEventAnalyser events;
    private final UidToMsnConverter converter;    
    private final MailboxSession mailboxSession;
    private final Set recentUids;
    private boolean recentUidRemoved;
    
    public SelectedMailboxSessionImpl(Mailbox mailbox, List uids, 
            MailboxSession mailboxSession) throws MailboxManagerException {
        this.mailbox = mailbox;
        this.mailboxSession = mailboxSession;
        recentUids = new TreeSet();
        recentUidRemoved = false;
        final long sessionId = mailboxSession.getSessionId();
        events = new MailboxEventAnalyser(sessionId);
        // Ignore events from our session
        events.setSilentFlagChanges(true);
        mailbox.addListener(events);
        converter = new UidToMsnConverter(uids);
        mailbox.addListener(converter);
    }

    /**
     * @see org.apache.james.api.imap.process.SelectedImapMailbox#deselect()
     */
    public void deselect() {
        mailbox.removeListener(events);
    }

    public boolean isSizeChanged() {
        return events.isSizeChanged();
    }
    
    public Mailbox getMailbox() {
        return mailbox;
    }

    /**
     * @see org.apache.james.api.imap.process.SelectedImapMailbox#unsolicitedResponses(boolean, boolean)
     */
    public List unsolicitedResponses(boolean omitExpunged, boolean useUid) {
        final List results = new ArrayList();
        final Mailbox mailbox = getMailbox();
        final boolean sizeChanged = isSizeChanged();
        // New message response
        if (sizeChanged) {
            addExistsResponses(results, mailbox);
        }
        // Expunged messages
        if (!omitExpunged) {
            addExpungedResponses(results, mailbox);
        }
        if(sizeChanged || (recentUidRemoved && !omitExpunged)) {
            addRecentResponses(results, mailbox);
            recentUidRemoved = false;
        }
        
        // Message updates
        addFlagsResponses(results, useUid, mailbox);
        
        events.reset();
        return results;
    }

    public int msn(long uid) {
        return converter.getMsn(uid);
    }
    
    private void addExpungedResponses(List responses, final Mailbox mailbox) {
        for  (Iterator it = events.expungedUids(); it.hasNext();) {
            final Long uid = (Long) it.next();
            final long uidValue = uid.longValue();
            final int msn = msn(uidValue);
            // TODO: use factory
            ExpungeResponse response = new ExpungeResponse(msn);
            responses.add(response);
        }
        
        for  (Iterator it = events.expungedUids(); it.hasNext();) {
            final Long uid = (Long) it.next();
            final long uidValue = uid.longValue();
            converter.expunge(uidValue);
        }
    }

    private void addFlagsResponses(final List responses, boolean useUid, final Mailbox mailbox) {
        try {
            for (final Iterator it = events.flagUpdateUids(); it.hasNext();) {
                Long uid = (Long) it.next();
                GeneralMessageSet messageSet = GeneralMessageSetImpl.oneUid(uid.longValue());
                addFlagsResponses(responses, useUid, mailbox, messageSet);
            }
        } catch (MessagingException e) {
            final String message = "Failed to retrieve flags data";
            handleResponseException(responses, e, message);
        }
    }

    private void addFlagsResponses(final List responses, boolean useUid, final Mailbox mailbox, GeneralMessageSet messageSet) throws MailboxManagerException {
        final Iterator it = mailbox.getMessages(messageSet, FetchGroupImpl.FLAGS, mailboxSession);
        while (it.hasNext()) {
            MessageResult mr = (MessageResult) it.next();
            final long uid = mr.getUid();
            int msn = msn(uid);
            final Flags flags = mr.getFlags();
            final Long uidOut;
            if (useUid) {
                uidOut = new Long(uid);
            } else {
                uidOut = null;
            }
            FetchResponse response = new FetchResponse(msn, flags, uidOut, null, null, null, null, null);
            responses.add(response);
        }
    }

    private void addRecentResponses(final List responses, final Mailbox mailbox) {
        final int recentCount = recentCount();
        // TODO: use factory
        RecentResponse response = new RecentResponse(recentCount);
        responses.add(response);
    }

    private void handleResponseException(final List responses, MessagingException e, final String message) {
        getLogger().info(message);
        getLogger().debug(message, e);
        // TODO: consider whether error message should be passed to the user
        UntaggedNoResponse response = new UntaggedNoResponse(message, null);
        responses.add(response);
    }

    private void addExistsResponses(final List responses, final Mailbox mailbox) {
            try {
                final int messageCount = mailbox.getMessageCount(mailboxSession);
                // TODO: use factory
                ExistsResponse response = new ExistsResponse(messageCount);
                responses.add(response);
            } catch (MailboxManagerException e) {
                final String message = "Failed to retrieve exists count data";
                handleResponseException(responses, e, message);
            }
    }

    public long uid(int msn) {
        return converter.getUid(msn);
    }

    public boolean removeRecent(long uid) {
        final boolean result = recentUids.remove(new Long(uid));
        if (result) {
            recentUidRemoved = true;
        }
        return result;
    }
    
    public boolean addRecent(long uid) {
        final boolean result = recentUids.add(new Long(uid));
        return result;
    }

    public long[] getRecent() {
        checkExpungedRecents();
        final long[] results = new long[recentUids.size()];
        int count = 0;
        for (Iterator it=recentUids.iterator();it.hasNext();) {
            Long uid = (Long) it.next();
            results[count++] = uid.longValue();
        }
        return results;
    }

    public int recentCount() {
        checkExpungedRecents();
        return recentUids.size();
    }

    public String getName() {
        return mailbox.getName();
    }
    
    private void checkExpungedRecents() {
        for(final Iterator it = events.expungedUids();it.hasNext();) {
            final Long uid = (Long) it.next();
            removeRecent(uid.longValue());
        }
    }

    public boolean isRecent(long uid) {
        boolean result = false;
        for (Iterator ir = recentUids.iterator(); ir.hasNext();) {
            Long recentUid = (Long) ir.next();
            if (recentUid.longValue() == uid) {
                result = true;
                break;
            }
        }
        return result;
    }
}
