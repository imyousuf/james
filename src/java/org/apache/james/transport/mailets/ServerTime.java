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
     */
    public void service(Mail mail) throws javax.mail.MessagingException {
        log("Sending timestamp");
        MimeMessage response = (MimeMessage)mail.getMessage().reply(false);
        response.setSubject("The time is now...");
        response.setText("This mail server thinks it's " + new java.util.Date() + ".");

        Set recipients = new HashSet();
        Address addresses[] = response.getAllRecipients();
        for (int i = 0; i < addresses.length; i++) {
            recipients.add(new MailAddress((InternetAddress)addresses[0]));
        }

        MailAddress sender = new MailAddress((InternetAddress)response.getFrom()[0]);
        getMailetContext().sendMail(sender, recipients, response);
    }

    public String getMailetInfo() {
        return "ServerTime Mailet";
    }
}

