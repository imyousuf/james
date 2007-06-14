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
import java.util.List;

import javax.mail.Flags;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.message.MessageFlags;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.api.imap.process.SelectedImapMailbox;
import org.apache.james.imap.message.response.imap4rev1.ExistsResponse;
import org.apache.james.imap.message.response.imap4rev1.ExpungeResponse;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse;
import org.apache.james.imap.message.response.imap4rev1.RecentResponse;
import org.apache.james.imap.message.response.imap4rev1.status.UntaggedNoResponse;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;

public class SelectedMailboxSessionImpl extends AbstractLogEnabled implements MailboxListener, SelectedImapMailbox {

    private ImapSession _session;
    private boolean _sizeChanged;
    private List expungedMsn = new ArrayList();
    private ImapMailboxSession mailbox;

    public SelectedMailboxSessionImpl(ImapMailboxSession mailbox, ImapSession session) throws MailboxManagerException {
        this.mailbox = mailbox;
        _session = session;
        // TODO make this a weak reference (or make sure deselect() is *always* called).
        mailbox.addListener(this,MessageResult.MSN | MessageResult.UID);
    }

    /**
     * @see org.apache.james.api.imap.process.SelectedImapMailbox#deselect()
     */
    public void deselect() {
        mailbox.removeListener(this);
        try {
            mailbox.close();
        } catch (MailboxManagerException e) {
            final Logger logger = getLogger();
            logger.warn("Cannot close selected mailbox" + e.getMessage());
            logger.debug("Failed to close selected mailbox. Deselection will continue.", e);
        }
        mailbox = null;
    }


    public void mailboxDeleted() {
        try {
            _session.closeConnection("Mailbox " + mailbox.getName() + " has been deleted");
        } catch (MailboxManagerException e) {
            getLogger().error("error closing connection", e);
        }
    }


    public boolean isSizeChanged() {
        return _sizeChanged;
    }

    public void setSizeChanged(boolean sizeChanged) {
        _sizeChanged = sizeChanged;
    }

    public void create() {
        throw new RuntimeException("should not create a selected mailbox");
        
    }

    public void expunged(MessageResult mr) {
        expungedMsn.add(new Integer(mr.getSize()));
    }

    public void added(MessageResult mr) {
       _sizeChanged = true;
    }

    public void flagsUpdated(MessageResult mr,MailboxListener silentListener) {
    }

    public ImapMailboxSession getMailbox() {
        return mailbox;
    }

    public void mailboxRenamed(String origName, String newName) {
        // TODO Auto-generated method stub
        
    }

    public void mailboxRenamed(String newName) {
        // TODO Auto-generated method stub
        
    }
    

    /**
     * @see org.apache.james.api.imap.process.SelectedImapMailbox#unsolicitedResponses(boolean, boolean)
     */
    public List unsolicitedResponses(boolean omitExpunged, boolean useUid) {
        final List results = new ArrayList();
        final ImapMailboxSession mailbox = getMailbox();
        // New message response
        if (isSizeChanged()) {
            setSizeChanged(false);
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
            MessageResult[] flagUpdates = mailbox.getFlagEvents(true);
                for (int i = 0; i < flagUpdates.length; i++) {
                MessageResult mr = flagUpdates[i];
                int msn = mr.getMsn();
                Flags updatedFlags = mr.getFlags();
                StringBuffer out = new StringBuffer("FLAGS ");
                out.append(MessageFlags.format(updatedFlags));
                if (useUid) {
                    out.append(" UID ");
                    out.append(mr.getUid());
                }
                // TODO: use CharSequence instead (avoid unnecessary string creation)
                FetchResponse response = new FetchResponse(msn, out.toString());
                responses.add(response);
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
