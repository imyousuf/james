/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

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
 * @version This is $Revision: 1.18 $
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
