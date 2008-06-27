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

package org.apache.james.imapserver.store;

import javax.mail.MessagingException;

import org.apache.james.mailboxmanager.MailboxManagerException;

/**
 * Thrown on an inappropriate attempt to reference a mailbox.
 * Includes attempting to create a mailbox that already exists and attempting
 * to open a mailbox that does not exist.
 * If status is ALREADY_EXISTS_REMOTELY or IF_CREATED_REMOTE then field
 * remoteServer should be set to the url of the remote server, formatted for
 * Mailbox Referral.
 */
public class MailboxException extends Exception
{

    public final static String ALREADY_EXISTS_LOCALLY
            = "Already exists locally";
    public final static String ALREADY_EXISTS_REMOTELY
            = "Already exists remotely";
    public final static String IF_CREATED_LOCAL
            = "If created, mailbox would be local";
    public final static String IF_CREATED_REMOTE
            = "If created, mailbox would be remote";
    public final static String NOT_LOCAL
            = "Does not exist locally, no further information available";
    public final static String LOCAL_BUT_DELETED
            = "Was local but has been deleted.";

    protected String status = null;
    protected String remoteServer = null;

    private String responseCode = null;

    /**
     * Construct a new <code>MailboxException</code> instance.
     *
     * @param message The detail message for this exception (mandatory).
     */
    public MailboxException( String message )
    {
        super( message );
    }

    /**
     * Construct a new <code>MailBoxException</code> instance.
     *
     * @param message The detail message for this exception (mandatory).
     * @param aStatus String constant indicating condition
     */
    public MailboxException( String message, String aStatus )
    {
        super( message );
        this.status = aStatus;
    }

    /**
     * Construct a new <code>MailBoxException</code> instance.
     *
     * @param message The detail message for this exception (mandatory).
     * @param aStatus String constant indicating condition
     * @param aServer String indicating another server where Mailbox should be.
     */
    public MailboxException( String message, String aStatus, String aServer )
    {
        super( message );
        this.status = aStatus;
        this.remoteServer = aServer;
    }

    public MailboxException(MailboxManagerException e) {
        super(e);
    }

    public MailboxException(MessagingException e) {
        super(e);
    }

    public String getStatus()
    {
        return status;
    }

    public String getRemoteServer()
    {
        return remoteServer;
    }

    public boolean isRemote()
    {
        return ( status.equals( ALREADY_EXISTS_REMOTELY )
                || status.equals( IF_CREATED_REMOTE ) );
    }

    public String getResponseCode()
    {
        return responseCode;
    }

    public void setResponseCode( String responseCode )
    {
        this.responseCode = responseCode;
    }
}
