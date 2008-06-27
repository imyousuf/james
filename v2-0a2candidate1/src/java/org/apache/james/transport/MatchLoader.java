/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport;

import java.util.*;
import javax.mail.*;
import org.apache.avalon.framework.component.Component;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.james.core.*;
import org.apache.mailet.*;

/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 */
public class MatchLoader implements Component, Configurable {

    private Configuration conf;
    private Vector matcherPackages;

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

    public Matcher getMatcher(String matchName, MailetContext context)
        throws MessagingException {
        try {
            String condition = (String) null;
            int i = matchName.indexOf('=');
            if (i != -1) {
                condition = matchName.substring(i + 1);
                matchName = matchName.substring(0, i);
            }
            for (i = 0; i < matcherPackages.size(); i++) {
                String className = (String)matcherPackages.elementAt(i) + matchName;
                try {
                    MatcherConfigImpl configImpl = new MatcherConfigImpl();
                    configImpl.setCondition(condition);
                    configImpl.setMailetContext(context);

                    Matcher matcher = (Matcher) Class.forName(className).newInstance();
                    matcher.init(configImpl);
                    return matcher;
                } catch (ClassNotFoundException cnfe) {
                    //do this so we loop through all the packages
                }
            }
            throw new ClassNotFoundException("Requested matcher not found: " + matchName + ".  looked in " + matcherPackages.toString());
        } catch (MessagingException me) {
            throw me;
        } catch (Exception e) {
            throw new MailetException("Could not load matcher (" + matchName + ")", e);
        }
    }
}
