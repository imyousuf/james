/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.servlet;

import java.util.*;
import org.apache.arch.*;
import org.apache.james.*;
import org.apache.avalon.blocks.*;
import org.apache.mail.*;
import org.apache.james.transport.*;
import org.apache.java.util.*;
import org.apache.james.transport.match.*;

/**
 * Receive  a Mail from JamesSpoolManager and takes care of delivery 
 * the message to local inboxs.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class ProcessingPipe extends GenericMailServlet {

    private SimpleContext context;
    private GenericMailServlet rootPipe;

    public void init() throws Exception {
        context = new SimpleContext(getContext());
        for (Enumeration e = getConfigurations("mailets.mailet"); e.hasMoreElements(); ) {
            Configuration c = (Configuration) e.nextElement();
            String name = c.getAttribute("name");
            String className = c.getAttribute("class");
            try {
                GenericMailServlet ms = (GenericMailServlet) Class.forName(className).newInstance();
                ms.setConfiguration(c);
                ms.setContext(getContext());
                ms.setComponentManager(getComponentManager());
                ms.init();
                context.put(name, ms);
                log("Mailet " + className + " initializated");
            } catch (Exception ex) {
                log("Unable to init mailet " + className + ": " + ex);
                ex.printStackTrace();
                throw ex;
            }
        }
        
        rootPipe = new Pipe();
        rootPipe.setConfiguration(getConfiguration("node"));
        rootPipe.setContext(context);
        rootPipe.setComponentManager(getComponentManager());
        rootPipe.init();
    }
    
    public Mail service(Mail mail) {
        try {
            rootPipe.service(mail);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        return (Mail) null;
    }

    public String getServletInfo() {
        return "ProcessingPipe Mailet";
    }
}
    
