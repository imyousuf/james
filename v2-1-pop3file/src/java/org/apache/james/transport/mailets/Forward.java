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
import java.util.HashSet;
import java.util.StringTokenizer;

/**
 * Replaces incoming recipients with those specified.
 *
 * @author Federico Barbieri <scoobie@pop.systemy.it>
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public class Forward extends GenericMailet {

    private Collection newRecipients;

    /**
     * Initialize the mailet
     */
    public void init() throws MessagingException {
        newRecipients = new HashSet();
        StringTokenizer st = new StringTokenizer(getMailetConfig().getInitParameter("forwardto"), ",", false);
        while (st.hasMoreTokens()) {
            newRecipients.add(new MailAddress(st.nextToken()));
        }
    }

    /**
     * Forwards a mail to a particular recipient.
     *
     * @param mail the mail being processed
     *
     * @throws MessagingException if an error occurs while forwarding the mail
     */
    public void service(Mail mail) throws MessagingException {
       if (mail.getSender() == null || getMailetContext().getMailServers(mail.getSender().getHost()).size() != 0) {
           // If we do not do this check, and somone uses Forward in a
           // processor initiated by SenderInFakeDomain, then a fake
           // sender domain will cause an infinite loop (the forwarded
           // e-mail still appears to come from a fake domain).
           // Although this can be viewed as a configuration error, the
           // consequences of such a mis-configuration are severe enough
           // to warrant protecting against the infinite loop.
           getMailetContext().sendMail(mail.getSender(), newRecipients, mail.getMessage());
       }
       else {
           StringBuffer logBuffer = new StringBuffer(256)
                                   .append("Forward mailet cannot forward ")
                                   .append(mail)
                                   .append(". Invalid sender domain for ")
                                   .append(mail.getSender())
                                   .append(". Consider using the Redirect mailet.");
           log(logBuffer.toString());
       }
       if(! (new Boolean(getInitParameter("passThrough"))).booleanValue()) {
            mail.setState(Mail.GHOST);
       }
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Forward Mailet";
    }
}

