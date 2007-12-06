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


import java.util.Collection;
import java.util.Iterator;

import javax.mail.Flags;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.api.imap.message.MessageFlags;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;

/**
 * @version $Revision: 109034 $
 */
public final class ImapSessionImpl extends AbstractLogEnabled implements ImapSession, ImapConstants
{
    private ImapSessionState state = ImapSessionState.NON_AUTHENTICATED;
    private User user = null;
    private SelectedMailboxSession selectedMailbox = null;

    private String clientHostName;
    private String clientAddress;

    // TODO these shouldn't be in here - they can be provided directly to command components.
    private ImapHandlerInterface handler;
    private MailboxManagerProvider mailboxManagerProvider;
    private UsersRepository users;
    
    private MailboxManager mailboxManager = null;
    private User mailboxManagerUser = null;
    
    public ImapSessionImpl( MailboxManagerProvider mailboxManagerProvider,
                            UsersRepository users,
                            ImapHandlerInterface handler,
                            String clientHostName,
                            String clientAddress )
    {
        this.mailboxManagerProvider = mailboxManagerProvider;
        this.users = users;
        this.handler = handler;
        this.clientHostName = clientHostName;
        this.clientAddress = clientAddress;
    }
    
    public void unsolicitedResponses( ImapResponse request, boolean useUid ) throws MailboxException {
        unsolicitedResponses(request, false, useUid);
    }

    public void unsolicitedResponses(ImapResponse response, boolean omitExpunged, boolean useUid)
            throws MailboxException {
        SelectedMailboxSession selected = getSelected();
        try {
            if (selected != null) {
                // New message response
                if (selected.isSizeChanged()) {
                    response.existsResponse(selected.getMailbox()
                            .getMessageCount());
                    response.recentResponse(selected.getMailbox()
                            .getRecentCount(true));
                }

               // Message updates
               for (final Iterator it = selected.getFlagUpdates(); it.hasNext(); ) {
                    MessageResult mr = (MessageResult) it.next();
                    int msn = selected.msn(mr.getUid());
                    Flags updatedFlags = mr.getFlags();
                    StringBuffer out = new StringBuffer("FLAGS ");
                    out.append(MessageFlags.format(updatedFlags));
                    if (useUid) {
                        out.append(" UID ");
                        out.append(mr.getUid());
                    }
                    response.fetchResponse(msn, out.toString());
                }

                // Expunged messages
                if (!omitExpunged) {
                    final Iterator expunged = selected.getExpungedEvents(true);
                    while (expunged.hasNext()) {
                        final int msn = ((Integer) expunged.next()).intValue();
                        response.expungeResponse(msn);
                    }
                }
                selected.reset();
            }
        } catch (MailboxManagerException e) {
            throw new MailboxException(e);
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

    public UsersRepository getUsers()
    {
        return users;
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

    public void setSelected( ImapMailbox mailbox, boolean readOnly, Collection uids ) throws MailboxManagerException
    {
        SelectedMailboxSession sessionMailbox = new SelectedMailboxSession(mailbox, uids);
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



    public MailboxManager getMailboxManager() throws MailboxManagerException {
        final boolean usersEqual;
        if (mailboxManagerUser!=null) {
            usersEqual=mailboxManagerUser.equals(user);
        } else {
            usersEqual=(user==null);
        }
        if (mailboxManager==null || !usersEqual) {
            if (mailboxManager!=null) {
                mailboxManager.close();
            }
            mailboxManager=mailboxManagerProvider.getMailboxManager();
            mailboxManagerUser = user;
            mailboxManager.createInbox(user);
        }
        return mailboxManager;
    }



    public String buildFullName(String mailboxName) throws MailboxManagerException {
        if (!mailboxName.startsWith(NAMESPACE_PREFIX)) {
            mailboxName = mailboxManagerProvider.getPersonalDefaultNamespace(user).getName()+HIERARCHY_DELIMITER+mailboxName;
        }
        return mailboxName;
    }
}
