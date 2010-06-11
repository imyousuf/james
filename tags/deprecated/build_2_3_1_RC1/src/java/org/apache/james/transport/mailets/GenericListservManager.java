/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/

package org.apache.james.transport.mailets;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;

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
                    getMailetContext().bounce(mail, "Unable to remove you from listserv for some reason.");
                }
            } else {
                getMailetContext().bounce(mail, "You are not subscribed to this listserv.");
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
