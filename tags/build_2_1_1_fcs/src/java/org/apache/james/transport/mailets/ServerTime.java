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

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.HashSet;
import java.util.Set;

/**
 * Returns the current time for the mail server.  Sample configuration:
 * <mailet match="RecipientIs=time@cadenza.lokitech.com" class="ServerTime">
 * </mailet>
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class ServerTime extends GenericMailet {
    /**
     * Sends a message back to the sender indicating what time the server thinks it is.
     *
     * @param mail the mail being processed
     *
     * @throws MessagingException if an error is encountered while formulating the reply message
     */
    public void service(Mail mail) throws javax.mail.MessagingException {
        MimeMessage response = (MimeMessage)mail.getMessage().reply(false);
        response.setSubject("The time is now...");
        StringBuffer textBuffer =
            new StringBuffer(128)
                    .append("This mail server thinks it's ")
                    .append((new java.util.Date()).toString())
                    .append(".");
        response.setText(textBuffer.toString());

        // Someone manually checking the server time by hand may send
        // an formatted message, lacking From and To headers.  If the
        // response fields are null, try setting them from the SMTP
        // MAIL FROM/RCPT TO commands used to send the inquiry.

        if (response.getFrom() == null) {
            response.setFrom(((MailAddress)mail.getRecipients().iterator().next()).toInternetAddress());
        }

        if (response.getAllRecipients() == null) {
            response.setRecipients(MimeMessage.RecipientType.TO, mail.getSender().toString());
        }

        response.saveChanges();
        getMailetContext().sendMail(response);
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "ServerTime Mailet";
    }
}

