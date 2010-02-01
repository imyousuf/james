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


import java.util.List;

import org.apache.james.protocols.api.TLSSupportedSession;
import org.apache.james.services.MailRepository;
import org.apache.mailet.Mail;

/**
 * All the handlers access this interface to communicate with
 * POP3Handler object
 */

public interface POP3Session extends TLSSupportedSession {

    /**
     * A placeholder for emails deleted during the course of the POP3 transaction.  
     * This Mail instance is used to enable fast checks as to whether an email has been
     * deleted from the inbox.
     */
    public final static String DELETED ="DELETED_MAIL";
   
    // Authentication states for the POP3 interaction
    /** Waiting for user id */
    public final static int AUTHENTICATION_READY = 0;
    /** User id provided, waiting for password */
    public final static int AUTHENTICATION_USERSET = 1;  
    /**
     * A valid user id/password combination has been provided.
     * In this state the client can access the mailbox
     * of the specified user.
     */
    public final static int TRANSACTION = 2;              

    /**
     * Returns POP3Handler service wide configuration
     *
     * @return POP3HandlerConfigurationData
     */
    POP3HandlerConfigurationData getConfigurationData();
    
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
}

