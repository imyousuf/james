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

package org.apache.james.transport.mailets;

import org.apache.james.util.RFC2822Headers;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.Header;
import javax.mail.MessagingException;
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

        // Create a new InternetHeader collection
        InternetHeaders newHeaders = new InternetHeaders();
        // Set our Return-Path header
        newHeaders.setHeader(RFC2822Headers.RETURN_PATH, mail.getSender() == null ? "<>" : "<" + mail.getSender() + ">");
        // Remove all Return-Path headers from the message
        message.removeHeader(RFC2822Headers.RETURN_PATH);
        // Copy all remaining header lines from the message to our new header set
        Enumeration headers = message.getAllHeaderLines();
        while (headers.hasMoreElements()) {
            newHeaders.addHeaderLine((String) headers.nextElement());
        }
        // Remember all Header names
        headers = message.getAllHeaders();
        ArrayList names = new ArrayList();
        while (headers.hasMoreElements()) {
            names.add(((Header)headers.nextElement()).getName());
        }
        final String[] headerNames = (String[]) names.toArray(new String[0]);
        names = null;

        for (Iterator i = recipients.iterator(); i.hasNext(); ) {
            MailAddress recipient = (MailAddress) i.next();
            try {
                // Remove all headers
                for(int h = 0; h < headerNames.length; h++) {
                    message.removeHeader(headerNames[h]);
                }

                // Copy our new header set to the message
                headers = newHeaders.getAllHeaderLines();
                while (headers.hasMoreElements()) {
                    message.addHeaderLine((String) headers.nextElement());
                }

                // Add qmail's de facto standard Delivered-To header
                message.addHeaderLine("Delivered-To: " + recipient.toString());

                getMailetContext().storeMail(mail.getSender(), recipient, message);
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
