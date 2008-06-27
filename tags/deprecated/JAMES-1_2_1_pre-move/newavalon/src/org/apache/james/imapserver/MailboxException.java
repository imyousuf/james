/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imapserver;

/**
 * Thrown on an inappropriate attempt to reference a mailbox.
 * Includes attempting to create a mailbox that already exists and attempting
 * to open a mailbox that does not exist.
 * If status is ALREADY_EXISTS_REMOTELY or IF_CREATED_REMOTE then field
 * remoteServer should be set to the url of the remote server, formatted for
 * Mailbox Referral.
 *
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public class MailboxException extends Exception {

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

    /**
     * Construct a new <code>MailboxException</code> instance.
     *
     * @param message The detail message for this exception (mandatory).
     */
    public MailboxException(String message) {
        super(message);
    }

    /**
     * Construct a new <code>MailBoxException</code> instance.
     *
     * @param message The detail message for this exception (mandatory).
     * @param aStatus String constant indicating condition
     */
    public MailboxException(String message, String aStatus) {
	super(message);
	this.status = aStatus;
    }

    /**
     * Construct a new <code>MailBoxException</code> instance.
     *
     * @param message The detail message for this exception (mandatory).
     * @param aStatus String constant indicating condition
     * @param sServer String indicating another server where Mailbox should be.
     */
    public MailboxException(String message, String aStatus, String aServer) {
	super(message);
	this.status = aStatus;
	this.remoteServer = aServer;
    }

    public String getStatus() {
	return status;
    }

    public String getRemoteServer() {
	return remoteServer;
    }

    public boolean isRemote() {
	return ( status.equals(ALREADY_EXISTS_REMOTELY)
		 || status.equals(IF_CREATED_REMOTE) ) ;
    }
 
}
