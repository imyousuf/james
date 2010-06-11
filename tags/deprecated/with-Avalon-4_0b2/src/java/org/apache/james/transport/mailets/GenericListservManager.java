/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.mailet.*;

/**
 * An abstract implementation of a listserv manager.  This mailet reads the
 * address to find the command.
 */
public abstract class GenericListservManager extends GenericMailet {

    /**
     * Adds an address to the listserv.  Returns whether command was
     * successful.
     */
    public abstract boolean addAddress(MailAddress address);

    /**
     * Removes an address from the listserv.  Returns whether command
     * was successful.
     */
    public abstract boolean removeAddress(MailAddress address);


    /**
     * Indicates whether an address already exists on the listserv. Returns
     * whether the address exists.
     */
    public abstract boolean existsAddress(MailAddress address);

    /**
     * Processes the message.  Checks which command was sent based on the
     * recipient address, and does the appropriate action.
     */
    public final void service(Mail mail) throws MessagingException {
        if (mail.getRecipients().size() != 1) {
            getMailetContext().bounce(mail, "You can only send one command at a time to this listserv manager.");
            return;
        }
        MailAddress address = (MailAddress)mail.getRecipients().iterator().next();
        if (address.getUser().endsWith("-off")) {
            if (existsAddress(mail.getSender())) {
                if (removeAddress(mail.getSender())) {
                    getMailetContext().bounce(mail, "Successfully removed from listserv.");
                } else {
                    getMailetContext().bounce(mail, "You are not subscribed to this listserv.");
                }
            } else {
                getMailetContext().bounce(mail, "Unable to remove you from listserv for some reason");
            }
        } else if (address.getUser().endsWith("-on")) {
            if (existsAddress(mail.getSender())) {
                getMailetContext().bounce(mail, "You are already subscribed to this listserv.");
            } else {
                if (addAddress(mail.getSender())) {
                    getMailetContext().bounce(mail, "Successfully added to listserv.");
                } else {
                    getMailetContext().bounce(mail, "Unable to add you to the listserv for some reason");
                }
            }
        } else {
            getMailetContext().bounce(mail, "Could not understand the command you sent to this listserv manager.\r\n"
                                      + "Valid commands are <listserv>-on@domain.com and <listserv>-off@domain.com");
        }
        //Kill the command message
        mail.setState(Mail.GHOST);
    }
}
