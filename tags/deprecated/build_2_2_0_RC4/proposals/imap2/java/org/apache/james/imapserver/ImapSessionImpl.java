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

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;
import org.apache.james.imapserver.store.ImapMailbox;

/**
 *
 *
 * @version $Revision: 1.1.2.3 $
 */
public final class ImapSessionImpl implements ImapSession
{
    private ImapSessionState state = ImapSessionState.NON_AUTHENTICATED;
    private User user = null;
    private ImapMailbox selectedMailbox = null;
    // TODO: Use a session-specific wrapper for user's view of mailbox
    // this wrapper would provide access control and track if the mailbox
    // is opened read-only.
    private boolean selectedIsReadOnly = false;

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

    public void unsolicitedResponses( ImapResponse request )
    {
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
    }

    public void setSelected( ImapMailbox mailbox, boolean readOnly )
    {
        this.state = ImapSessionState.SELECTED;
        this.selectedMailbox = mailbox;
        this.selectedIsReadOnly = readOnly;
    }

    public ImapMailbox getSelected()
    {
        return this.selectedMailbox;
    }

    public boolean selectedIsReadOnly()
    {
        return this.selectedIsReadOnly;
    }

    public ImapSessionState getState()
    {
        return this.state;
    }
}
