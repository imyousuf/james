/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.imapserver;

import org.apache.james.imapserver.store.ImapMailbox;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.imapserver.store.MessageFlags;
import org.apache.mailet.User;
import org.apache.mailet.UsersRepository;

import javax.mail.Flags;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision$
 */
public final class ImapSessionImpl implements ImapSession
{
    private ImapSessionState state = ImapSessionState.NON_AUTHENTICATED;
    private User user = null;
    private ImapSessionMailbox selectedMailbox = null;

    private String clientHostName;
    private String clientAddress;

    // TODO these shouldn't be in here - they can be provided directly to command components.
    private ImapHandler handler;
    private ImapHost imapHost;
    private UsersRepository users;

    public ImapSessionImpl( ImapHost imapHost,
                            UsersRepository users,
                            ImapHandler handler,
                            String clientHostName,
                            String clientAddress )
    {
        this.imapHost = imapHost;
        this.users = users;
        this.handler = handler;
        this.clientHostName = clientHostName;
        this.clientAddress = clientAddress;
    }

    public ImapHost getHost()
    {
        return imapHost;
    }

    public void unsolicitedResponses( ImapResponse request ) throws MailboxException {
        unsolicitedResponses(request, false);
    }

    public void unsolicitedResponses( ImapResponse response, boolean omitExpunged ) throws MailboxException {
        ImapSessionMailbox selected = getSelected();
        if (selected != null) {
            // New message response
            if (selected.isSizeChanged()) {
                response.existsResponse(selected.getMessageCount());
                response.recentResponse(selected.getRecentCount(true));
                selected.setSizeChanged(false);
            }

            // Message updates
            List flagUpdates = selected.getFlagUpdates();
            Iterator iter = flagUpdates.iterator();
            while (iter.hasNext()) {
                ImapSessionMailbox.FlagUpdate entry = 
                        (ImapSessionMailbox.FlagUpdate) iter.next();
                int msn = entry.getMsn();
                Flags updatedFlags = entry.getFlags();
                StringBuffer out = new StringBuffer( "FLAGS " );
                out.append( MessageFlags.format(updatedFlags) );
                if (entry.getUid() != null) {
                    out.append(" UID ");
                    out.append(entry.getUid());
                }
                response.fetchResponse(msn, out.toString());

            }

            // Expunged messages
            if (! omitExpunged) {
                int[] expunged = selected.getExpunged();
                for (int i = 0; i < expunged.length; i++) {
                    int msn = expunged[i];
                    response.expungeResponse(msn);
                }
            }
        }
    }
    
    public void closeConnection(String byeMessage) {
        handler.forceConnectionClose(byeMessage);
    }

    public void closeConnection()
    {
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
        if (selectedMailbox != null) {
            // TODO is there more to do here, to cleanup the mailbox.
            selectedMailbox.removeListener(selectedMailbox);
            this.selectedMailbox = null;
        }
    }

    public void setSelected( ImapMailbox mailbox, boolean readOnly )
    {
        ImapSessionMailbox sessionMailbox = new ImapSessionMailbox(mailbox, this, readOnly);
        this.state = ImapSessionState.SELECTED;
        this.selectedMailbox = sessionMailbox;
    }

    public ImapSessionMailbox getSelected()
    {
        return this.selectedMailbox;
    }

    public boolean selectedIsReadOnly()
    {
        return selectedMailbox.isReadonly();
    }

    public ImapSessionState getState()
    {
        return this.state;
    }

}
