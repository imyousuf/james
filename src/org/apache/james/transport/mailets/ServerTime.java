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
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Returns the current time for the mail server.  Sample configuration:
 * <mailet match="RecipientIs=time@cadenza.lokitech.com" class="ServerTime">
 * </mailet>
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class ServerTime extends AbstractMailet {
    /**
     * Sends a message back to the sender indicating what time the server thinks it is.
     */
    private Mailet transport;
    private Logger logger;

    public void init() {
        MailetContext context = getContext();
        transport = (Mailet) context.get("transport");
        logger = (Logger) context.getComponentManager().getComponent(Interfaces.LOGGER);
    }

    public void service(Mail mail) {
        try {
            logger.log("Sending timestamp", "Mailets", Logger.INFO);
            MimeMessage response = new MimeMessage(Session.getDefaultInstance(new Properties(), null));
            response.setSubject("The time is now...");
            response.setText("This mail server thinks it's " + new java.util.Date() + ".");

            Collection recipients = new Vector();
            recipients.add(mail.getSender());
            InternetAddress addr[] = {new InternetAddress(mail.getSender())};
            response.setRecipients(Message.RecipientType.TO, addr);
            response.setFrom(new InternetAddress(mail.getRecipients().iterator().next().toString()));

            mail.setSender(mail.getRecipients().iterator().next().toString());
            mail.setMessage(response);
            mail.setRecipients(recipients);
            transport.service(mail);
        } catch (Exception e) {
            logger.log("Exception while retrieving message " + mail.getName () + ": " + e.getMessage(), "Mailets", Logger.ERROR);
        }
    }

    public String getMailetInfo() {
        return "ServerTime Mailet";
    }
}

