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
import org.apache.mail.servlet.*;
import org.apache.james.transport.*;
import org.apache.java.util.*;
import org.apache.james.transport.match.*;

/**
 * Receive  a Mail from JamesSpoolManager and takes care of delivery 
 * the message to local inboxs.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class Pipe extends GenericMailServlet {

    private Context context;
    private Configuration conf;
    private Match match;
    private String param;
    private Vector pipe;

    public void init() throws Exception {
        conf = getConfigurations();
        context = getContext();
        String matchName = conf.getAttribute("condition");
        param = (String) null;
        int hasParam = matchName.indexOf('=');
        if (hasParam != -1) {
            param = matchName.substring(hasParam + 1);
            matchName = matchName.substring(0, hasParam);
        }   
        pipe = new Vector();
        match = (Match) Class.forName("org.apache.james.transport.match." + matchName).newInstance();
        match.setContext(context);
        match.setComponentManager(getComponentManager());
        for (Enumeration e = conf.getConfigurations("node"); e.hasMoreElements(); ) {
            Configuration c = (Configuration) e.nextElement();
            String type = c.getAttribute("type");
            if (type.equals("match")) {
                GenericMailServlet subPipe = new Pipe();
                subPipe.setConfiguration(c);
                subPipe.setContext(context);
                subPipe.setComponentManager(getComponentManager());
                subPipe.init();
                pipe.addElement(subPipe);
            } else if (type.equals("mailet")) {
                String name = c.getAttribute("name");
                pipe.addElement(context.get(name));
            }
        }
    }
    
    public Mail service(Mail mail) {
        Vector matching = match.match(mail, param);
        if (matching == null || matching.isEmpty()) return mail;
        Vector notMatching = VectorUtils.subtract(mail.getRecipients(), matching);
        Mail toBeParsed;
        Mail response;
        if (notMatching.isEmpty()) {
            toBeParsed = mail;
            response = (Mail) null;
        } else {
            toBeParsed = mail.duplicate();
            response = mail;
            response.setRecipients(notMatching);
            toBeParsed.setRecipients(matching);
        }
        for (Enumeration e = pipe.elements(); e.hasMoreElements(); ) {
            GenericMailServlet ms = (GenericMailServlet) e.nextElement();
            if (toBeParsed != null) {
                toBeParsed = ms.service(toBeParsed);
            }
        }
        return response;
    }

    public String getServletInfo() {
        return "ProcessingPipe Mailet";
    }
}
    
