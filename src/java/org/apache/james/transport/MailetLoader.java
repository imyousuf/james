/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport;
import javax.mail.MessagingException;

import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.core.MailetConfigImpl;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetContext;
import org.apache.mailet.MailetException;
/**
 * Loads Mailets for use inside James.
 *
 */
public class MailetLoader extends Loader implements Configurable {
           /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
           getPackages(conf,MAILET_PACKAGE);
           configureMailetClassLoader();
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
            for (int i = 0; i < packages.size(); i++) {
                String className = (String) packages.elementAt(i) + mailetName;
                try {
                    MailetConfigImpl configImpl = new MailetConfigImpl();
                    configImpl.setMailetName(mailetName);
                    configImpl.setConfiguration(configuration);
                    configImpl.setMailetContext(context);
                    Mailet mailet = (Mailet) mailetClassLoader.loadClass(className).newInstance();
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
                    .append(packages.toString());
            throw new ClassNotFoundException(exceptionBuffer.toString());
        } catch (MessagingException me) {
            throw me;
        } catch (Exception e) {
            StringBuffer exceptionBuffer =
                new StringBuffer(128).append("Could not load mailet (").append(mailetName).append(
                    ")");
            throw new MailetException(exceptionBuffer.toString(), e);
        }
    }
}
