/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import javax.mail.internet.*;
import org.apache.avalon.framework.logger.AbstractLoggable;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.activity.Disposable;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.ContextException;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.cornerstone.services.scheduler.PeriodicTimeTrigger;
import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
import org.apache.james.AccessControlException;
import org.apache.james.AuthenticationException;
import org.apache.james.AuthorizationException;
import org.apache.james.Constants;
import org.apache.james.services.*;
import org.apache.james.util.InternetPrintWriter;
import org.apache.log.LogKit;
import org.apache.log.Logger;

/**
 * Different connection handlers extend this class
 * Common Connection Handler code could be factored into this class.
 * At present(April 28' 2001) there is not much in this class
 */
public class BaseConnectionHandler extends AbstractLoggable implements Configurable {
    protected int timeout;
    protected String helloName;

    public void configure( final Configuration configuration )
        throws ConfigurationException {

        timeout = configuration.getChild( "connectiontimeout" ).getValueAsInteger( 1800000 );
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch  (UnknownHostException ue) {
            hostName = "localhost";
        }
        
        Configuration helloConf = configuration.getChild("helloName");
        String autodetect = null;
        try {
            autodetect = helloConf.getAttribute("autodetect");
        } catch(ConfigurationException ex) {
            autodetect = "TRUE";
        }
        if ("TRUE".equals(autodetect))
            helloName = hostName;
        else
            helloName = helloConf.getValue("localhost");
        getLogger().info("Hello Name is: " + helloName);
    }
}
