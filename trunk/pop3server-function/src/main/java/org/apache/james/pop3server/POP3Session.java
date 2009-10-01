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



package org.apache.james.pop3server;


import java.io.OutputStream;
import java.util.List;

import org.apache.james.services.MailRepository;
import org.apache.james.socket.TLSSupportedSession;
import org.apache.james.socket.Watchdog;
import org.apache.mailet.Mail;

/**
 * All the handlers access this interface to communicate with
 * POP3Handler object
 */

public interface POP3Session extends TLSSupportedSession{

    /**
     * Clears the response buffer, returning the String of characters in the buffer.
     *
     * @return the data in the response buffer
     */
    String clearResponseBuffer();


    /**
     * Returns host name of the client
     *
     * @return hostname of the client
     */
    String getRemoteHost();

    /**
     * Returns host ip address of the client
     *
     * @return host ip address of the client
     */
    String getRemoteIPAddress();

    /**
     * Returns the session status
     *
     * @return if the session is open or closed
     */
    boolean isSessionEnded();

    /**
     * Returns POP3Handler service wide configuration
     *
     * @return POP3HandlerConfigurationData
     */
    POP3HandlerConfigurationData getConfigurationData();

    /**
     * Returns the user name associated with this POP3 interaction.
     *
     * @return the user name
     */
    String getUser();

    /**
     * Sets the user name associated with this POP3 interaction.
     *
     * @param user the user name
     */
    void setUser(String user);

    /**
     * Returns Watchdog object used for handling timeout
     *
     * @return Watchdog object
     */
    Watchdog getWatchdog();
    
    /**
     * Returns the current handler state
     *
     * @return handler state
     */
    int getHandlerState();

    /**
     * Sets the new handler state
     * 
     * @param handlerState state
     */
    void setHandlerState(int handlerState);

    /**
     * Returns the current user inbox
     *
     * @return MailRepository
     */
    MailRepository getUserInbox();

    /**
     * Sets the user's mail repository
     * 
     * @param userInbox userInbox
     */
    void setUserInbox(MailRepository userInbox);

    /**
     * Returns the mail list contained in the mailbox
     * 
     * @return mailbox content
     */
    List<Mail> getUserMailbox();

    /**
     * Sets a new mailbox content
     * 
     * @param userMailbox mailbox
     */
    void setUserMailbox(List<Mail> userMailbox);
    
    /**
     * Returns the backup mailbox
     * 
     * @return list backup
     */
    List<Mail> getBackupUserMailbox();


    /**
     * Sets a new backup mailbox content
     * 
     * @param backupUserMailbox the mailbox backup
     */
    void setBackupUserMailbox(List<Mail> backupUserMailbox);

    /**
     * Returns the raw output stream
     *
     * @return the raw outputstream
     */
    OutputStream getOutputStream();

    /**
     * Write the response to the client
     * 
     * @param response
     */
    void writePOP3Response(POP3Response response);


    /**
     * Write the response to the client
     * 
     * @param string
     */
    void writeResponse(String string);

}

