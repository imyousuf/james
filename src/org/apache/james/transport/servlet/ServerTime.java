/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.servlet;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.java.lang.*;
import org.apache.james.*;
import org.apache.avalon.interfaces.*;
import org.apache.mail.*;
import org.apache.james.transport.*;

/**
 * Returns the current time for the mail server.  Sample configuration:
 * <servlet match="RecipientIs=time@cadenza.lokitech.com" class="ServerTime">
 * </servlet>
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class ServerTime extends GenericMailServlet {
    /**
     * Sends a message back to the sender indicating what time the server thinks it is.
     */
    public Mail service(Mail mail) {
        try {
            MimeMessage response = new MimeMessage(Session.getDefaultInstance(new Properties(), null));
            response.setSubject("The time is now...");
            response.setText("This mail server thinks it's " + new java.util.Date() + ".");

            Vector recipients = new Vector();
            recipients.addElement(mail.getSender());
            InternetAddress addr[] = {new InternetAddress(mail.getSender())};
            response.setRecipients(Message.RecipientType.TO, addr);
            response.setFrom(new InternetAddress(mail.getRecipients().elementAt(0).toString()));

            mail.setSender(mail.getRecipients().elementAt(0).toString());
            mail.setMessage(response);
            mail.setRecipients(recipients);
            return mail;
        } catch (Exception e) {
            log("Exception while retrieving message " + mail.getName ()+ ": " + e.getMessage() + ".  Killing message.");
        }
        return null;
    }

    public String getServletInfo() {
        return "ServerTime";
    }
}

