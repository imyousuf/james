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

package org.apache.james.api.imap.process;

import java.util.List;

import org.apache.james.api.imap.ImapSessionState;
import org.apache.james.api.imap.message.response.ImapResponseMessage;
import org.apache.james.services.User;


/**
 * Encapsulates all state held for an ongoing Imap session,
 * which commences when a client first establishes a connection to the Imap
 * server, and continues until that connection is closed.
 *
 * TODO: {@link #logout()}, {@link #closeConnection(String)}, 
 * {@link #closeMailbox()} and {@link #deselect()} are too closely related
 * in function to justify separate API methods
 * @version $Revision: 109034 $
 */
public interface ImapSession
{
    /**
     * Sends any unsolicited responses to the client, such as EXISTS and FLAGS
     * responses when the selected mailbox is modified by another user.
     * @return <code>List</code> of {@link ImapResponseMessage}'s
     */
    List unsolicitedResponses( boolean useUid );

    List unsolicitedResponses( boolean omitExpunged, boolean useUid);
    
    /**
     * Logs out the session.
     * Marks the connection for closure;
     */
    void logout();

    /**
     * TODO: this method is not clearly 
     * @param byeMessage
     */
    void closeConnection(String byeMessage);

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
    void authenticated( User user );

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
    void selected( SelectedImapMailbox mailbox );

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
    SelectedImapMailbox getSelected();

    /**
     * Closes the Mailbox
     */
    void closeMailbox();

    /**
     * Gets an attribute of this session by name.
     * Implementations should ensure that access
     * is thread safe. 
     * @param key name of the key, not null
     * @return <code>Object</code> value
     * or null if this attribute has unvalued
     */
    public Object getAttribute(String key);
    
    /**
     * Sets an attribute of this session by name.
     * Implementations should ensure that access
     * is thread safe. 
     * @param key name of the key, not null
     * @param value <code>Object</code> value 
     * or null to set this attribute as unvalued
     */
    public void setAttribute(String key, Object value);
}
