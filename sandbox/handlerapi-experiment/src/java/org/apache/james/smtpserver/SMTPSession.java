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


import org.apache.mailet.Mail;

import java.util.Map;

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
    public final static String CURRENT_HELO_NAME = "CURRENT_HELO_NAME"; 
    public static final Object CURRENT_RECIPIENT = "CURRENT_RECIPIENT"; // Current recipient
    public final static String SESSION_STATE_MAP = "SESSION_STATE_MAP"; // the Session state 

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
     * Returns Map that consists of the state of the SMTPSession per mail
     *
     * @return map of the current SMTPSession state per mail
     */
    Map getState();

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
     * Returns whether Relaying is allowed or not
     *
     * @return the relaying status
     */
    boolean isRelayingAllowed();
    
    /**
     * Set if reallying is allowed
     * 
     * @param relayingAllowed
     */
    void setRelayingAllowed(boolean relayingAllowed);

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
     * @param user the user name
     */
    void setUser(String user);

    /**
     * Returns the SMTP session id
     *
     * @return SMTP session id
     */
    String getSessionID();
    
    /**
     * Returns the recipient count
     * 
     * @return recipient count
     */
    int getRcptCount();
    
    /**
     * Returns Map that consists of the state of the SMTPSession per connection
     *
     * @return map of the current SMTPSession state per connection
     */
    Map getConnectionState();

    /**
     * Put a new line handler in the chain
     * @param overrideCommandHandler
     */
    void pushLineHandler(LineHandler overrideCommandHandler);
    
    /**
     * Pop the last command handler 
     */
    void popLineHandler();

    /**
     * Write an SMTPResponse to the client
     */
    void writeSMTPResponse(SMTPResponse response);
}

