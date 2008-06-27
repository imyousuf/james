/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport;

import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.core.MailetConfigImpl;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetContext;
import org.apache.mailet.MailetException;

import javax.mail.MessagingException;
import java.util.Vector;

/**
 * Loads Mailets for use inside James.
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 */
public class MailetLoader implements Component, Configurable {

    /**
     * The list of packages that may contain Mailets
     */
    private Vector mailetPackages;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
        mailetPackages = new Vector();
        mailetPackages.addElement("");
        final Configuration[] pkgConfs = conf.getChildren( "mailetpackage" );
        for ( int i = 0; i < pkgConfs.length; i++ )
        {
            Configuration c = pkgConfs[i];
            String packageName = c.getValue();
            if (!packageName.endsWith(".")) {
                packageName += ".";
            }
            mailetPackages.addElement(packageName);
        }
    }

    /**
     * Get a new Mailet with the specified name acting
     * in the specified context.
     *
     * @param matchName the name of the mailet to be loaded
     * @param context the MailetContext to be passed to the new
     *                mailet
     * @throws MessagingException if an error occurs
     */
    public Mailet getMailet(String mailetName, MailetContext context, Configuration configuration)
        throws MessagingException {
        try {
            for (int i = 0; i < mailetPackages.size(); i++) {
                String className = (String)mailetPackages.elementAt(i) + mailetName;
                ClassLoader theClassLoader = null;
                try {
                    MailetConfigImpl configImpl = new MailetConfigImpl();
                    configImpl.setMailetName(mailetName);
                    configImpl.setConfiguration(configuration);
                    configImpl.setMailetContext(context);

                    if (theClassLoader == null) {
                        theClassLoader = this.getClass().getClassLoader();
                    }

                    Mailet mailet = (Mailet) theClassLoader.loadClass(className).newInstance();
                    mailet.init(configImpl);
                    return mailet;
                } catch (ClassNotFoundException cnfe) {
                    //do this so we loop through all the packages
                }
            }
            StringBuffer exceptionBuffer =
                new StringBuffer(128)
                        .append("Requested mailet not found: ")
                        .append(mailetName)
                        .append(".  looked in ")
                        .append(mailetPackages.toString());
            throw new ClassNotFoundException(exceptionBuffer.toString());
        } catch (MessagingException me) {
            throw me;
        } catch (Exception e) {
            StringBuffer exceptionBuffer =
                new StringBuffer(128)
                        .append("Could not load mailet (")
                        .append(mailetName)
                        .append(")");
            throw new MailetException(exceptionBuffer.toString(), e);
        }
    }
}
