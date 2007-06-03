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

package org.apache.james.experimental.imapserver;


import java.util.Map;

import javax.mail.Flags;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.ImapSessionState;
import org.apache.james.api.imap.message.MessageFlags;
import org.apache.james.experimental.imapserver.encode.ImapResponseComposer;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.services.User;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

/**
 * @version $Revision: 109034 $
 */
public final class ImapSessionImpl extends AbstractLogEnabled implements ImapSession, ImapConstants
{
    private ImapSessionState state = ImapSessionState.NON_AUTHENTICATED;
    private User user = null;
    private SelectedMailboxSession selectedMailbox = null;

    private final String clientHostName;
    private final String clientAddress;

    private ImapHandlerInterface handler;
    
    private final Map attributesByKey;
    
    public ImapSessionImpl( ImapHandlerInterface handler,
                            String clientHostName,
                            String clientAddress )
    {
        this.handler = handler;
        this.clientHostName = clientHostName;
        this.clientAddress = clientAddress;
        this.attributesByKey = new ConcurrentHashMap();
    }

    public void unsolicitedResponses( ImapResponseComposer request, boolean useUid ) {
        unsolicitedResponses(request, false, useUid);
    }

    public void unsolicitedResponses(ImapResponseComposer response, boolean omitExpunged, boolean useUid) {
        SelectedMailboxSession selected = getSelected();
            if (selected != null) {
                final ImapMailboxSession mailbox = selected.getMailbox();
                // New message response
                if (selected.isSizeChanged()) {
                    selected.setSizeChanged(false);
                    existsResponse(response, mailbox);
                    recentResponse(response, mailbox);
                }

                // Message updates
                // TODO: slow to check flags every time
//              TODO: add conditional to selected mailbox
                flagsResponse(response, useUid, mailbox);

                // Expunged messages
                if (!omitExpunged) {
                    // TODO: slow to check flags every time
                    // TODO: add conditional to selected mailbox
                    expungedResponse(response, mailbox);
                }
            }
    }

    private void expungedResponse(ImapResponseComposer response, final ImapMailboxSession mailbox) {
        try {
            MessageResult[] expunged = mailbox.getExpungedEvents(true);
            for (int i = 0; i < expunged.length; i++) {
                MessageResult mr = expunged[i];
                response.expungeResponse(mr.getMsn());
            }
        } catch (MailboxManagerException e) {
            final String message = "Failed to retrieve expunged count data";
            handleResponseException(response, e, message);
        }
    }

    private void flagsResponse(ImapResponseComposer response, boolean useUid, final ImapMailboxSession mailbox) {
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
                response.fetchResponse(msn, out.toString());
            }
        } catch (MailboxManagerException e) {
            final String message = "Failed to retrieve flags data";
            handleResponseException(response, e, message);
        }
    }

    private void recentResponse(ImapResponseComposer response, final ImapMailboxSession mailbox) {
        try {
            final int recentCount = mailbox.getRecentCount(true);
            response.recentResponse(recentCount);
        } catch (MailboxManagerException e) {
            final String message = "Failed to retrieve recent count data";
            handleResponseException(response, e, message);
        }
    }

    private void handleResponseException(ImapResponseComposer response, MailboxManagerException e, final String message) {
        getLogger().info(message);
        getLogger().debug(message, e);
        // TODO: consider whether error message should be passed to the user
        response.untaggedNoResponse(message, null);
    }

    private void existsResponse(ImapResponseComposer response, final ImapMailboxSession mailbox) {
            try {
                final int messageCount = mailbox.getMessageCount();
                response.existsResponse(messageCount);
            } catch (MailboxManagerException e) {
                final String message = "Failed to retrieve exists count data";
                handleResponseException(response, e, message);
            }
    }
    
    public void closeConnection(String byeMessage) {
        closeMailbox();
        handler.forceConnectionClose(byeMessage);
    }

    public void closeConnection()
    {
        closeMailbox();
        handler.resetHandler();
    }

    public String getClientHostname()
    {
        return clientHostName;
    }

    public String getClientIP()
    {
        return clientAddress;
    }

    public void setAuthenticated( User user )
    {
        this.state = ImapSessionState.AUTHENTICATED;
        this.user = user;
    }

    public User getUser()
    {
        return this.user;
    }

    public void deselect()
    {
        this.state = ImapSessionState.AUTHENTICATED;
        closeMailbox();
    }

    public void setSelected( ImapMailboxSession mailbox, boolean readOnly ) throws MailboxManagerException
    {
        SelectedMailboxSession sessionMailbox = new SelectedMailboxSession(mailbox, this, readOnly);
        setupLogger(sessionMailbox);
        this.state = ImapSessionState.SELECTED;
        closeMailbox();
        this.selectedMailbox = sessionMailbox;
    }

    public SelectedMailboxSession getSelected()
    {
        return this.selectedMailbox;
    }

    public ImapSessionState getState()
    {
        return this.state;
    }

    public void closeMailbox() {
        if (selectedMailbox != null) {
            try {
                selectedMailbox.close();
            } catch (MailboxManagerException e) {
                getLogger().error("error closing Mailbox", e);
            }
            selectedMailbox=null;
        }
        
    }


    public Object getAttribute(String key) {
        final Object result = attributesByKey.get(key);
        return result;
    }

    public void setAttribute(String key, Object value) {
        if (value == null) {
            attributesByKey.remove(key);
        } else {
            attributesByKey.put(key, value);
        }
    }
}
