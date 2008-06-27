/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.mailets;

import org.apache.mailet.*;
import java.util.*;
import javax.mail.MessagingException;

/**
 * Receive  a Mail from JamesSpoolManager and takes care of delivery
 * the message to local inboxes.
 *
 * @author Federico Barbieri <scoobie@pop.systemy.it>
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public class LocalDelivery extends GenericMailet {

    public void service(Mail mail) throws MessagingException {
        Collection recipients = mail.getRecipients();
        Collection errors = new Vector();
        for (Iterator i = recipients.iterator(); i.hasNext(); ) {
            MailAddress recipient = (MailAddress) i.next();
            try {
                getMailetContext().storeMail(mail.getSender(), recipient, mail.getMessage());
            } catch (Exception ex) {
                ex.printStackTrace();
                errors.add(recipient);
            }
        }
        if (errors.isEmpty()) {
            mail.setState(Mail.GHOST);
        } else {
            getMailetContext().sendMail(mail.getSender(), errors, mail.getMessage(), Mail.ERROR);
            //mail.setRecipients(errors);
            //mail.setState(Mail.ERROR);
            //mail.setErrorMessage("Unable to delivery locally message");
        }
    }

    public String getMailetInfo() {
        return "Local Delivery Mailet";
    }
}

