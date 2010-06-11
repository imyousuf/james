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

package org.apache.james.smtpserver;


import org.apache.james.util.watchdog.Watchdog;
import org.apache.mailet.Mail;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

/**
 * All the handlers access this interface to communicate with
 * SMTPHandler object
 */

public interface SMTPSession {

    // Keys used to store/lookup data in the internal state hash map
    public final static String MESG_FAILED = "MESG_FAILED";   // Message failed flag
    public final static String SENDER = "SENDER_ADDRESS";     // Sender's email address
    public final static String RCPT_LIST = "RCPT_LIST";   // The message recipients
    public final static String CURRENT_HELO_MODE = "CURRENT_HELO_MODE"; // HELO or EHLO

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
     * Returns Inputstream for handling messages and commands
     *
     * @return InputStream object
     */
    InputStream getInputStream();

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
     * Returns Mail object for message handlers to process
     *
     * @return Mail object
     */
    Mail getMail();

    /**
     * Sets the MailImpl object for further processing
     *
     * @param mail MailImpl object
     */
    void setMail(Mail mail);

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
     * this makes the message to be dropped inprotocol
     *
     */
    void abortMessage();

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
     * Returns Map that consists of the state of the SMTPSession
     *
     * @return map of the current SMTPSession state
     */
    HashMap getState();

    /**
     * Resets message-specific, but not authenticated user, state.
     *
     */
    void resetState();

    /**
     * Returns SMTPHandler service wide configuration
     *
     * @return SMTPHandlerConfigurationData
     */
    SMTPHandlerConfigurationData getConfigurationData();

    /**
     * Sets the blocklisted value
     *
     * @param blocklisted
     */
    void setBlockListed(boolean blocklisted);

    /**
     * Returns the blocklisted status
     *
     * @return blocklisted
     */
    boolean isBlockListed();

    /**
     * Returns whether Relaying is allowed or not
     *
     * @return the relaying status
     */
    boolean isRelayingAllowed();

    /**
     * Returns whether Authentication is required or not
     *
     * @return authentication required or not
     */
    boolean isAuthRequired();
    
    /**
     * Returns whether remote server needs to send HELO/EHLO
     *
     * @return HELO/EHLO required or not
     */
    boolean useHeloEhloEnforcement();

    /**
     * Returns the user name associated with this SMTP interaction.
     *
     * @return the user name
     */
    String getUser();

    /**
     * Sets the user name associated with this SMTP interaction.
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
     * Returns the SMTP session id
     *
     * @return SMTP session id
     */
    String getSessionID();

}

