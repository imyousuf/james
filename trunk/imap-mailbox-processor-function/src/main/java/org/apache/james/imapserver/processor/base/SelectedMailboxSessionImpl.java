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

import javax.mail.Flags;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.process.SelectedImapMailbox;
import org.apache.james.imap.message.response.imap4rev1.ExistsResponse;
import org.apache.james.imap.message.response.imap4rev1.ExpungeResponse;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse;
import org.apache.james.imap.message.response.imap4rev1.RecentResponse;
import org.apache.james.imap.message.response.imap4rev1.status.UntaggedNoResponse;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.mailboxmanager.util.MailboxEventAnalyser;

//TODO: deal with deleted or renamed mailboxes
public class SelectedMailboxSessionImpl extends AbstractLogEnabled implements SelectedImapMailbox {

    private ImapMailboxSession mailbox;
    
    private final MailboxEventAnalyser events;
    
    public SelectedMailboxSessionImpl(ImapMailboxSession mailbox) throws MailboxManagerException {
        this.mailbox = mailbox;
        final long sessionId = mailbox.getSessionId();
        events = new MailboxEventAnalyser(sessionId);
        // Ignore events from our session
        events.setSilentFlagChanges(true);
        mailbox.addListener(events);
    }

    /**
     * @see org.apache.james.api.imap.process.SelectedImapMailbox#deselect()
     */
    public void deselect() {
        mailbox.removeListener(events);
        try {
            mailbox.close();
        } catch (MailboxManagerException e) {
            final Logger logger = getLogger();
            logger.warn("Cannot close selected mailbox" + e.getMessage());
            logger.debug("Failed to close selected mailbox. Deselection will continue.", e);
        }
        mailbox = null;
    }

    public boolean isSizeChanged() {
        return events.isSizeChanged();
    }
    
    public ImapMailboxSession getMailbox() {
        return mailbox;
    }

    /**
     * @see org.apache.james.api.imap.process.SelectedImapMailbox#unsolicitedResponses(boolean, boolean)
     */
    public List unsolicitedResponses(boolean omitExpunged, boolean useUid) {
        final List results = new ArrayList();
        final ImapMailboxSession mailbox = getMailbox();
        // New message response
        if (isSizeChanged()) {
            addExistsResponses(results, mailbox);
            addRecentResponses(results, mailbox);
        }

        // Message updates
        // TODO: slow to check flags every time
        // TODO: add conditional to selected mailbox
        addFlagsResponses(results, useUid, mailbox);

        // Expunged messages
        if (!omitExpunged) {
            // TODO: slow to check flags every time
            // TODO: add conditional to selected mailbox
            addExpungedResponses(results, mailbox);
        }
        
        events.reset();
        return results;
    }

    private void addExpungedResponses(List responses, final ImapMailboxSession mailbox) {
        try {
            MessageResult[] expunged = mailbox.getExpungedEvents(true);
            for (int i = 0; i < expunged.length; i++) {
                MessageResult mr = expunged[i];
                final int msn = mr.getMsn();
                // TODO: use factory
                ExpungeResponse response = new ExpungeResponse(msn);
                responses.add(response);
            }
        } catch (MailboxManagerException e) {
            final String message = "Failed to retrieve expunged count data";
            handleResponseException(responses, e, message);
        }
    }

    private void addFlagsResponses(final List responses, boolean useUid, final ImapMailboxSession mailbox) {
        try {
            
            for (final Iterator it = events.flagUpdateUids(); it.hasNext();) {
                Long uid = (Long) it.next();
                GeneralMessageSet messageSet = GeneralMessageSetImpl.oneUid(uid.longValue());
                final MessageResult[] messages = mailbox.getMessages(messageSet, MessageResult.FLAGS | MessageResult.MSN);
                for (int i = 0; i < messages.length; i++) {
                    MessageResult mr = messages[i];
                    int msn = mr.getMsn();
                    final Flags flags = mr.getFlags();
                    final Long uidOut;
                    if (useUid) {
                        uidOut = new Long(mr.getUid());
                    } else {
                        uidOut = null;
                    }
                    FetchResponse response = new FetchResponse(msn, flags, uidOut);
                    responses.add(response);
                }
            }
        } catch (MailboxManagerException e) {
            final String message = "Failed to retrieve flags data";
            handleResponseException(responses, e, message);
        }
    }

    private void addRecentResponses(final List responses, final ImapMailboxSession mailbox) {
        try {
            final int recentCount = mailbox.getRecentCount(true);
            // TODO: use factory
            RecentResponse response = new RecentResponse(recentCount);
            responses.add(response);
        } catch (MailboxManagerException e) {
            final String message = "Failed to retrieve recent count data";
            handleResponseException(responses, e, message);
        }
    }

    private void handleResponseException(final List responses, MailboxManagerException e, final String message) {
        getLogger().info(message);
        getLogger().debug(message, e);
        // TODO: consider whether error message should be passed to the user
        UntaggedNoResponse response = new UntaggedNoResponse(message, null);
        responses.add(response);
    }

    private void addExistsResponses(final List responses, final ImapMailboxSession mailbox) {
            try {
                final int messageCount = mailbox.getMessageCount();
                // TODO: use factory
                ExistsResponse response = new ExistsResponse(messageCount);
                responses.add(response);
            } catch (MailboxManagerException e) {
                final String message = "Failed to retrieve exists count data";
                handleResponseException(responses, e, message);
            }
    }
}
