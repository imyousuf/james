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

/**
 * Replace incoming recipient with specified ones.
 * 
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class Forward extends AbstractMailet {
    
    private Vector newRecipients;
    private Mailet transport;

    public void init () {
        newRecipients = new Vector();
        MailetContext context = getContext();
        Configuration conf = context.getConfiguration();
        transport = (Mailet) context.get("transport");
        for (Enumeration e = conf.getConfigurations("forwardto"); e.hasMoreElements(); ) {
            Configuration c = (Configuration) e.nextElement();
            newRecipients.addElement(c.getValue());
        }
    }
    
    public void service(Mail mail) throws Exception {
        
        mail.setRecipients(newRecipients);
        MailetContext context = getContext();
        transport = (Mailet) context.get("transport");
        try {
            transport.service(mail);
        } catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
        mail.setState(Mail.GHOST);
    }
    
    public String getServletInfo() {
        return "Forward Mail Servlet";
    }
}
    
