/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.servlet;

import java.util.*;
import org.apache.arch.Configuration;
import org.apache.mail.Mail;

/**
 * Replace incoming recipient with specified ones.
 * 
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class Forward extends GenericMailServlet {
    
    private Vector newRecipients;

    public void init () {
        newRecipients = new Vector();
        for (Enumeration e = getConfigurations("forwardto"); e.hasMoreElements(); ) {
            Configuration c = (Configuration) e.nextElement();
            newRecipients.addElement(c.getValue());
        }
    }
    
    public Mail service(Mail mail) {
        
        log("Forwarding mail " + mail.getName() + " to " + newRecipients);
        mail.setRecipients(newRecipients);
        return mail;
    }
    
    public String getServletInfo() {
        return "Forward Mail Servlet";
    }
}
    
