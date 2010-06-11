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

import org.apache.james.experimental.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;


/**
 * Encapsulates all state held for an ongoing Imap session,
 * which commences when a client first establishes a connection to the Imap
 * server, and continues until that connection is closed.
 *
 * @version $Revision: 109034 $
 */
public interface ImapSession
{
    /**
     * Sends any unsolicited responses to the client, such as EXISTS and FLAGS
     * responses when the selected mailbox is modified by another user.
     * TODO: this probably should not throw a MailboxException
     * @param response The response to write to
     */
    void unsolicitedResponses( ImapResponse response, boolean useUid ) throws MailboxException;

    /**
     * Closes the connection for this session.
     */
    void closeConnection();

    void closeConnection(String byeMessage);


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
     * @throws MailboxManagerException 
     */
    void setSelected( ImapMailboxSession mailbox, boolean readOnly ) throws MailboxManagerException;

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
    SelectedMailboxSession getSelected();

    void unsolicitedResponses( ImapResponse request, boolean omitExpunged, boolean useUid) throws MailboxException;

    /**
     * closes the Mailbox if needed
     * @throws MailboxManagerException 
     */
    void closeMailbox() throws MailboxManagerException;

    MailboxManager getMailboxManager() throws MailboxManagerException;

    String buildFullName(String mailboxName) throws MailboxManagerException;

}
