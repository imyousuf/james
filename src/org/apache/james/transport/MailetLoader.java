/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.james.transport;

import java.util.*;
import org.apache.java.lang.*;
import org.apache.mail.*;

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

    public Mailet getMailet(String servletName, MailetContext context)
    throws Exception {
        for (int i = 0; i < servletPackages.size(); i++) {
            String className = (String)servletPackages.elementAt(i) + servletName;
            try {
                AbstractMailet mailet = (AbstractMailet) Class.forName(className).newInstance();
                mailet.setMailetContext(context);
                mailet.init();
                return mailet;
            } catch (ClassNotFoundException cnfe) {
                //do this so we loop through all the packages
            }
        }
        throw new ClassNotFoundException("Requested mailet not found: " + servletName + ".  looked in " + servletPackages.toString());
    }
}
