/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Vector;

/**
 * Receives a Mail from JamesSpoolManager and takes care of delivery
 * of the message to local inboxes.
 *
 * @author Federico Barbieri <scoobie@pop.systemy.it>
 * @author Serge Knystautas <sergek@lokitech.com>
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
        for (Iterator i = recipients.iterator(); i.hasNext(); ) {
            MailAddress recipient = (MailAddress) i.next();
            try {
                getMailetContext().storeMail(mail.getSender(), recipient, mail.getMessage());
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
                                        errors, mail.getMessage(), Mail.ERROR);
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

