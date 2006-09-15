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


import org.apache.james.services.MailRepository;
import org.apache.james.util.watchdog.Watchdog;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;

/**
 * All the handlers access this interface to communicate with
 * POP3Handler object
 */

public interface POP3Session {

    /**
     * Writes response string to the client
     *
     * @param respString String that needs to send to the client
     */
    void writeResponse(String respString);

    /**
     * Reads a line of characters off the command line.
     *
     * @return the trimmed input line
     * @throws IOException if an exception is generated reading in the input characters
     */
    String readCommandLine() throws IOException;


    /**
     * Returns ResponseBuffer, this optimizes the unecessary creation of resources
     * by each handler object
     *
     * @return responseBuffer
     */
    StringBuffer getResponseBuffer();

    /**
     * Clears the response buffer, returning the String of characters in the buffer.
     *
     * @return the data in the response buffer
     */
    String clearResponseBuffer();

    /**
     * Returns currently process command name
     *
     * @return current command name
     */
    String getCommandName();

    /**
     * Returns currently process command argument
     *
     * @return current command argument
     */
    String getCommandArgument();

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
     * this makes the session to close
     *
     */
    void endSession();

    /**
     * Returns the session status
     *
     * @return if the session is open or closed
     */
    boolean isSessionEnded();

    /**
     * Returns Map that consists of the state of the POP3Session
     *
     * @return map of the current POP3Session state
     */
    HashMap getState();

    /**
     * Resets message-specific, but not authenticated user, state.
     *
     */
    void resetState();

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
     * @param userID the user name
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
    List getUserMailbox();

    /**
     * Sets a new mailbox content
     * 
     * @param userMailbox mailbox
     */
    void setUserMailbox(List userMailbox);
    
    /**
     * Returns the backup mailbox
     * 
     * @return list backup
     */
    List getBackupUserMailbox();


    /**
     * Sets a new backup mailbox content
     * 
     * @param backupUserMailbox the mailbox backup
     */
    void setBackupUserMailbox(List backupUserMailbox);

    /**
     * Returns the raw output stream
     *
     * @return the raw outputstream
     */
    OutputStream getOutputStream();

}

