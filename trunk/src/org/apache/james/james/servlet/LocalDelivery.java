/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.james.servlet;

import java.util.*;
import org.apache.arch.*;
import org.apache.james.*;
import org.apache.avalon.blocks.*;
import org.apache.mail.*;
import org.apache.james.james.*;
/**
 * Receive  a Mail from JamesSpoolManager and takes care of delivery 
 * the message to local inboxs.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class LocalDelivery extends GenericMailServlet {

    private Store store;
    private String inboxRootName;

    public void init() 
    throws Exception {
        store = (Store) getComponentManager().getComponent(Interfaces.STORE);
        inboxRootName = (String) getContext().get(Constants.INBOX_ROOT);
    }
    
    public Mail service(Mail mail) {
        log("Locally delivering mail " + mail.getName());
        Vector recipients = mail.getRecipients();
        Vector errors = new Vector();
        for (Enumeration e = recipients.elements(); e.hasMoreElements(); ) {
            String recipient = (String) e.nextElement();
            try {
                log("Local delivery to " + recipient);
                getUserInbox(getUser(recipient)).store(mail);
            } catch (Exception ex) {
                log("Exception while storing message to " + recipient + ": " + ex.getMessage());
                errors.addElement(recipient);
            }
        }
        if (errors.isEmpty()) {
            return (Mail) null;
        } else {
            mail.setRecipients(errors);
            mail.setState(Mail.ERROR);
            mail.setErrorMessage("Unable to delivery locally message");
            return mail;
        }
    }

    public String getServletInfo() {
        return "Local Delivery Mail Servlet";
    }
    
    private MailRepository getUserInbox(String userName) {
        return (MailRepository) store.getPublicRepository(inboxRootName + "." + userName);
    }

    private String getUser(String recipient) {
        return recipient.substring(0, recipient.indexOf("@"));
    }
}
    
