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
import org.apache.mailet.*;
import org.apache.james.core.*;
import org.apache.avalon.*;

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
        for (Iterator it = conf.getChildren("matcherpackage"); it.hasNext(); ) {
            Configuration c = (Configuration) it.next();
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
