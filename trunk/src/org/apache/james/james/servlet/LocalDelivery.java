/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.james.servlet;

import java.io.*;
import java.util.*;
import org.apache.arch.*;
import org.apache.james.*;
import org.apache.avalon.blocks.*;
import org.apache.mail.*;
import org.apache.james.james.*;
/**
 * Receive  a MessageContainer from JamesSpoolManager and takes care of delivery 
 * the message to local inboxs.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class LocalDelivery extends GenericMailServlet {

    private Store store;
    private String inboxRootName;

    public void init() 
    throws Exception {
        ComponentManager comp = getComponentManager();
        Context context = getContext();
        inboxRootName = (String) context.get(Constants.INBOX_ROOT);
        store = (Store) comp.getComponent(Interfaces.STORE);
    }
    
    public MessageContainer service(MessageContainer mc) {
        log("Locally delivering mail " + mc.getMessageId());
        Vector recipients = mc.getRecipients();
        Vector errors = new Vector();
        for (Enumeration e = recipients.elements(); e.hasMoreElements(); ) {
            String recipient = (String) e.nextElement();
            try {
                log("Local delivery to " + getUser(recipient) + " (" + recipient + ")");
                getUserInbox(getUser(recipient)).store(mc.getMessageId(), mc);
            } catch (Exception ex) {
                log("Exception while storing message to " + recipient + ": " + ex.getMessage());
                errors.addElement(recipient);
            }
        }
        if (errors.isEmpty()) {
            return (MessageContainer) null;
        } else {
            mc.setRecipients(errors);
            mc.setState(MessageContainer.ERROR);
            mc.setErrorMessage("Unable to delivery locally message");
            return mc;
        }
    }

    public void destroy() {
    }

    public String getServletInfo() {
        return "Local Delivery Mail Servlet";
    }
    
    private MessageContainerRepository getUserInbox(String userName) {
        return (MessageContainerRepository) store.getPublicRepository(inboxRootName + "." + userName);
    }

    private String getUser(String recipient) {
        return recipient.substring(0, recipient.indexOf("@"));
    }
}
    
