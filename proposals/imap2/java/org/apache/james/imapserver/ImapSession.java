/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;
import org.apache.james.imapserver.store.ImapMailbox;

/**
 * Encapsulates all state held for an ongoing Imap session,
 * which commences when a client first establishes a connection to the Imap
 * server, and continues until that connection is closed.
 *
 * @author  Darrell DeBoer <darrell@apache.org>
 *
 * @version $Revision: 1.2 $
 */
public interface ImapSession
{
    /**
     * Sends any unsolicited responses to the client, such as EXISTS and FLAGS
     * responses when the selected mailbox is modified by another user.
     * @param response The response to write to
     */
    public void unsolicitedResponses( ImapResponse response );

    /**
     * Closes the connection for this session.
     */
    public void closeConnection();

    /**
     * Provides the Imap host for this server, which is used for all access to mail
     * storage and subscriptions.
     * @return The ImapHost for this server.
     */
    ImapHost getHost();

    /**
     * Provides the UsersRepository for this session, to allow session
     * to validate logins.
     *
     * @return The UsersRepository for this session.
     */
    UsersRepository getUsers();

    /**
     * @return The hostname of the connected client.
     */
    String getClientHostname();

    /**
     * @return The IP address of the connected client.
     */
    String getClientIP();

    /**
     * @return Returns the current state of this session.
     */
    ImapSessionState getState();

    /**
     * Moves the session into {@link ImapSessionState#AUTHENTICATED} state with
     * the supplied user.
     * @param user The user who is authenticated for this session.
     */
    void setAuthenticated( User user );

    /**
     * Provides the authenticated user for this session, or <code>null</code> if this
     * session is not in {@link ImapSessionState#AUTHENTICATED} or
     * {@link ImapSessionState#SELECTED} state.
     * @return The user authenticated for this session
     */
    User getUser();

    /**
     * Moves this session into {@link ImapSessionState#SELECTED} state and sets the
     * supplied mailbox to be the currently selected mailbox.
     * @param mailbox The selected mailbox.
     * @param readOnly If <code>true</code>, the selection is set to be read only.
     */
    void setSelected( ImapMailbox mailbox, boolean readOnly );

    /**
     * Moves the session out of {@link ImapSessionState#SELECTED} state and back into
     * {@link ImapSessionState#AUTHENTICATED} state. The selected mailbox is cleared.
     */
    void deselect();

    /**
     * Provides the selected mailbox for this session, or <code>null</code> if this
     * session is not in {@link ImapSessionState#SELECTED} state.
     * @return the currently selected mailbox.
     */
    ImapMailbox getSelected();

    /**
     * TODO? return a read-only wrapper for read-only selections, and put the
     * isReadOnly() on the mailbox itself?
     * @return if the currently selected mailbox is open read only.
     */
    boolean selectedIsReadOnly();

}
