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
import org.apache.james.core.MatcherConfigImpl;
import org.apache.mailet.MailetContext;
import org.apache.mailet.MailetException;
import org.apache.mailet.Matcher;

import javax.mail.MessagingException;
import java.util.Vector;

/**
 * Loads Matchers for use inside James.
 *
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 */
public class MatchLoader implements Component, Configurable {

    /**
     * The list of packages that may contain Mailets
     */
    private Vector matcherPackages;

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
        matcherPackages = new Vector();
        matcherPackages.addElement("");
        final Configuration[] pkgConfs = conf.getChildren( "matcherpackage" );
        for ( int i = 0; i < pkgConfs.length; i++ )
        {
            Configuration c = pkgConfs[i];
            String packageName = c.getValue();
            if (!packageName.endsWith(".")) {
                packageName += ".";
            }
            matcherPackages.addElement(packageName);
        }
    }

    /**
     * Get a new Matcher with the specified name acting
     * in the specified context.
     *
     * @param matchName the name of the matcher to be loaded
     * @param context the MailetContext to be passed to the new
     *                matcher
     * @throws MessagingException if an error occurs
     */
    public Matcher getMatcher(String matchName, MailetContext context)
        throws MessagingException {
        try {
            String condition = (String) null;
            int i = matchName.indexOf('=');
            if (i != -1) {
                condition = matchName.substring(i + 1);
                matchName = matchName.substring(0, i);
            }
            ClassLoader theClassLoader = null;
            for (i = 0; i < matcherPackages.size(); i++) {
                String className = (String)matcherPackages.elementAt(i) + matchName;
                try {
                    MatcherConfigImpl configImpl = new MatcherConfigImpl();
                    configImpl.setMatcherName(matchName);
                    configImpl.setCondition(condition);
                    configImpl.setMailetContext(context);

                    if (theClassLoader == null) {
                        theClassLoader = this.getClass().getClassLoader();
                    }

                    Matcher matcher = (Matcher) theClassLoader.loadClass(className).newInstance();
                    matcher.init(configImpl);
                    return matcher;
                } catch (ClassNotFoundException cnfe) {
                    //do this so we loop through all the packages
                }
            }
            StringBuffer exceptionBuffer =
                new StringBuffer(128)
                        .append("Requested matcher not found: ")
                        .append(matchName)
                        .append(".  looked in ")
                        .append(matcherPackages.toString());
            throw new ClassNotFoundException(exceptionBuffer.toString());
        } catch (MessagingException me) {
            throw me;
        } catch (Exception e) {
            StringBuffer exceptionBuffer =
                new StringBuffer(128)
                        .append("Could not load matcher (")
                        .append(matchName)
                        .append(")");
            throw new MailetException(exceptionBuffer.toString(), e);
        }
    }
}
