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

package org.apache.james.services;

import java.io.InputStream;
import java.util.Collection;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailRepository;

/**
 * The interface for Phoenix blocks to the James MailServer
 *
 *
 * @version This is $Revision: 1.17 $
 */
public interface MailServer
{
    /**
     * The component role used by components implementing this service
     */
    String ROLE = "org.apache.james.services.MailServer";

    /**
     * Reserved user name for the mail delivery agent for multi-user mailboxes
     */
    String MDA = "JamesMDA";

    /**
     * Reserved user name meaning all users for multi-user mailboxes
     */
    String ALL = "AllMailUsers";

    /**
     * Pass a MimeMessage to this MailServer for processing
     *
     * @param sender - the sender of the message
     * @param recipients - a Collection of String objects of recipients
     * @param msg - the MimeMessage of the headers and body content of
     * the outgoing message
     * @throws MessagingException - if the message fails to parse
     */
    void sendMail(MailAddress sender, Collection recipients, MimeMessage msg)
        throws MessagingException;

    /**
     * Pass a MimeMessage to this MailServer for processing
     *
     * @param sender - the sender of the message
     * @param recipients - a Collection of String objects of recipients
     * @param msg - an InputStream containing the headers and body content of
     * the outgoing message
     * @throws MessagingException - if the message fails to parse
     */
    void sendMail(MailAddress sender, Collection recipients, InputStream msg)
        throws MessagingException;

    /**
     *  Pass a Mail to this MailServer for processing
     * @param mail the Mail to be processed
     * @throws MessagingException
     */
    void sendMail(Mail mail)
        throws MessagingException;

    /**
     * Pass a MimeMessage to this MailServer for processing
     * @param message the message
     * @throws MessagingException
     */
    void sendMail(MimeMessage message)
        throws MessagingException;

    /**
     * Retrieve the primary mailbox for userName. For POP3 style stores this
     * is their (sole) mailbox.
     *
     * @param sender - the name of the user
     * @return a reference to an initialised mailbox
     */
    MailRepository getUserInbox(String userName);

    /**
     * Generate a new identifier/name for a mail being processed by this server.
     *
     * @return the new identifier
     */
    String getId();

    /**
     * Adds a new user to the mail system with userName. For POP3 style stores
     * this may only involve adding the user to the UsersStore.
     *
     * @param sender - the name of the user
     * @return a reference to an initialised mailbox
     */
    boolean addUser(String userName, String password);

    /**
     * Checks if a server is serviced by mail context
     *
     * @param serverName - name of server.
     * @return true if server is local, i.e. serviced by this mail context
     */
    boolean isLocalServer(String serverName);
}
