/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.james.transport;

import java.util.*;
import org.apache.arch.*;
import org.apache.mail.servlet.*;
import org.apache.james.transport.servlet.*;

/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 */
public class MailetLoader implements Component, Configurable {

    private Configuration conf;
    private Vector servletPackages;

    public void setConfiguration(Configuration conf) {
        servletPackages = new Vector();
        servletPackages.addElement("");
        for (Enumeration e = conf.getConfigurations("servletpackage"); e.hasMoreElements(); ) {
            Configuration c = (Configuration) e.nextElement();
            servletPackages.addElement(c.getValue());
        }
    }

    public MailServlet getMailet(String servletName, Configuration conf, Context context, ComponentManager comp)
    throws Exception {
        for (int i = 0; i < servletPackages.size(); i++) {
            String className = (String)servletPackages.elementAt(i) + servletName;
            try {
                MailServlet mailet = (MailServlet) Class.forName(className).newInstance();
                mailet.setConfiguration(conf);
                if (mailet instanceof GenericMailServlet) {
                    ((GenericMailServlet) mailet).setContext(context);
                    ((GenericMailServlet) mailet).setComponentManager(comp);
                }
                mailet.init();
                return mailet;
            } catch (ClassNotFoundException cnfe) {
                //do this so we loop through all the packages
            }
        }
        throw new ClassNotFoundException("Requested mailet not found: " + servletName + ".  looked in " + servletPackages.toString());
    }
}
