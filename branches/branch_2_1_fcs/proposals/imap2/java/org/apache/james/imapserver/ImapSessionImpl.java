/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;
import org.apache.james.imapserver.store.ImapMailbox;

/**
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.1 $
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
