/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.imapserver.store;

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
