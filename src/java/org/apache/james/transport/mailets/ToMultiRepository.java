/***********************************************************************
 * Copyright (c) 2000-2005 The Apache Software Foundation.             *
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

package org.apache.james.transport.mailets;

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.Constants;
import org.apache.james.James;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailServer;
import org.apache.mailet.RFC2822Headers;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetHeaders;

import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

/**
 * Receives a Mail from JamesSpoolManager and takes care of delivery
 * of the message to local inboxes.
 * 
 * Differently from LocalDelivery this does not lookup the UserRepository
 * This simply store the message in a repository named like the local part
 * of the recipient address.
 */
public class ToMultiRepository extends GenericMailet {
    /**
     * The number of mails generated.  Access needs to be synchronized for
     * thread safety and to ensure that all threads see the latest value.
     */
    private static long count;
  
    /**
     * The mailserver reference
     */
    private MailServer mailServer;

    /**
     * Delivers a mail to a local mailbox.
     *
     * @param mail the mail being processed
     *
     * @throws MessagingException if an error occurs while storing the mail
     */
    public void service(Mail mail) throws MessagingException {
        Collection recipients = mail.getRecipients();
        Collection errors = new Vector();

        MimeMessage message = mail.getMessage();

        // Set Return-Path and remove all other Return-Path headers from the message
        // This only works because there is a placeholder inserted by MimeMessageWrapper
        message.setHeader(RFC2822Headers.RETURN_PATH, (mail.getSender() == null ? "<>" : "<" + mail.getSender() + ">"));

        // Copy any Delivered-To headers from the message
        InternetHeaders deliveredTo = new InternetHeaders();
        Enumeration headers = message.getMatchingHeaders(new String[] {"Delivered-To"});
        while (headers.hasMoreElements()) {
            Header header = (Header) headers.nextElement();
            deliveredTo.addHeader(header.getName(), header.getValue());
        }

        for (Iterator i = recipients.iterator(); i.hasNext(); ) {
            MailAddress recipient = (MailAddress) i.next();
            try {
                // Add qmail's de facto standard Delivered-To header
                message.addHeader("Delivered-To", recipient.toString());

                storeMail(mail.getSender(), recipient, message);

                if (i.hasNext()) {
                    // Remove headers but leave all placeholders
                    message.removeHeader("Delivered-To");
                    headers = deliveredTo.getAllHeaders();
                    // And restore any original Delivered-To headers
                    while (headers.hasMoreElements()) {
                        Header header = (Header) headers.nextElement();
                        message.addHeader(header.getName(), header.getValue());
                    }
                }
            } catch (Exception ex) {
                getMailetContext().log("Error while storing mail.", ex);
                errors.add(recipient);
            }
        }

        if (!errors.isEmpty()) {
            // If there were errors, we redirect the email to the ERROR processor.
            // In order for this server to meet the requirements of the SMTP specification,
            // mails on the ERROR processor must be returned to the sender.  Note that this
            // email doesn't include any details regarding the details of the failure(s).
            // In the future we may wish to address this.
            getMailetContext().sendMail(mail.getSender(),
                                        errors, message, Mail.ERROR);
        }
        //We always consume this message
        mail.setState(Mail.GHOST);
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Local Delivery Mailet";
    }


    /**
     * 
     * @param sender
     * @param recipient
     * @param message
     * @throws MessagingException
     */
    public void storeMail(MailAddress sender, MailAddress recipient, MimeMessage message)
        throws MessagingException {
        String username;
        if (recipient == null) {
            throw new IllegalArgumentException("Recipient for mail to be spooled cannot be null.");
        }
        if (message == null) {
            throw new IllegalArgumentException("Mail message to be spooled cannot be null.");
        }
        username = recipient.getUser();

        Collection recipients = new HashSet();
        recipients.add(recipient);
        MailImpl mailImpl = new MailImpl(getId(), sender, recipients, message);
        MailRepository userInbox = mailServer.getUserInbox(username);
        if (userInbox == null) {
            StringBuffer errorBuffer =
                new StringBuffer(128)
                    .append("The inbox for user ")
                    .append(username)
                    .append(" was not found on this server.");
            throw new MessagingException(errorBuffer.toString());
        }
        userInbox.store(mailImpl);
    }
    

    /**
     * Return a new mail id.
     *
     * @return a new mail id
     */
    public String getId() {
        long localCount = -1;
        synchronized (James.class) {
            localCount = count++;
        }
        StringBuffer idBuffer =
            new StringBuffer(64)
                    .append("Mail")
                    .append(System.currentTimeMillis())
                    .append("-")
                    .append(localCount);
        return idBuffer.toString();
    }

    /**
     * @see org.apache.mailet.GenericMailet#init()
     */
    public void init() throws MessagingException {
            super.init();
        ServiceManager compMgr = (ServiceManager)getMailetContext().getAttribute(Constants.AVALON_COMPONENT_MANAGER);

        try {
            // Instantiate the a MailRepository for outgoing mails
            mailServer = (MailServer) compMgr.lookup(MailServer.ROLE);
        } catch (ServiceException cnfe) {
            log("Failed to retrieve Store component:" + cnfe.getMessage());
        } catch (Exception e) {
            log("Failed to retrieve Store component:" + e.getMessage());
        }
            
    }

}
