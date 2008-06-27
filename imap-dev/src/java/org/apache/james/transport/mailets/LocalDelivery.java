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

import org.apache.mailet.RFC2822Headers;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.InternetHeaders;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Vector;

/**
 * Receives a Mail from JamesSpoolManager and takes care of delivery
 * of the message to local inboxes.
 *
 */
public class LocalDelivery extends GenericMailet {
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

                getMailetContext().storeMail(mail.getSender(), recipient, message);

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
}
