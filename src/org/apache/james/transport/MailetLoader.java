/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.james.transport;

import java.util.*;
import javax.mail.*;
import org.apache.avalon.*;
import org.apache.mailet.*;
import org.apache.james.core.*;

/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 */
public class MailetLoader implements Component, Configurable {

    private Configuration conf;
    private Vector mailetPackages;

    public void setConfiguration(Configuration conf) {
        mailetPackages = new Vector();
        mailetPackages.addElement("");
        for (Enumeration e = conf.getConfigurations("mailetpackage"); e.hasMoreElements(); ) {
            Configuration c = (Configuration) e.nextElement();
            String packageName = c.getValue();
            if (!packageName.endsWith(".")) {
                packageName += ".";
            }
            mailetPackages.addElement(packageName);
        }
    }

    public Mailet getMailet(String mailetName, MailetContext context, Configuration configuration)
    throws MessagingException {
        try {
            for (int i = 0; i < mailetPackages.size(); i++) {
                String className = (String)mailetPackages.elementAt(i) + mailetName;
                try {
                    MailetConfigImpl configImpl = new MailetConfigImpl();
                    configImpl.setMailetName(mailetName);
                    configImpl.setConfiguration(configuration);
                    configImpl.setMailetContext(context);

                    Mailet mailet = (Mailet) Class.forName(className).newInstance();
                    mailet.init(configImpl);
                    return mailet;
                } catch (ClassNotFoundException cnfe) {
                    //do this so we loop through all the packages
                }
            }
            throw new ClassNotFoundException("Requested mailet not found: " + mailetName + ".  looked in " + mailetPackages.toString());
        } catch (MessagingException me) {
            throw me;
        } catch (Exception e) {
            throw new MailetException("Could not load mailet (" + mailetName + ")", e);
        }
    }
}
