/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.mailets;

import org.apache.mail.*;
import org.apache.james.transport.*;
import org.apache.java.lang.*;
import java.util.*;
import org.apache.james.*;
import org.apache.avalon.interfaces.*;

/**
 * Receive  a Mail from JamesSpoolManager and takes care of delivery
 * the message to local inboxs.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class LocalDelivery extends AbstractMailet {

    private MailServer james;

    public void init()
    throws Exception {
        james = (MailServer) getContext().getComponentManager().getComponent(Interfaces.MAIL_SERVER);
    }

    public void service(Mail mail) {
        Collection recipients = mail.getRecipients();
        Collection errors = new Vector();
        for (Iterator i = recipients.iterator(); i.hasNext(); ) {
            String recipient = (String) i.next();
            try {
                james.getUserInbox(Mail.getUser(recipient)).store(mail);
            } catch (Exception ex) {
                errors.add(recipient);
            }
        }
        if (errors.isEmpty()) {
            mail.setState(Mail.GHOST);
        } else {
            mail.setRecipients(errors);
            mail.setState(Mail.ERROR);
            mail.setErrorMessage("Unable to delivery locally message");
        }
    }

    public String getMailetInfo() {
        return "Local Delivery Mailet";
    }
}

