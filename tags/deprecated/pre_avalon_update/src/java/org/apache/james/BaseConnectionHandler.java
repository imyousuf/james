/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Different connection handlers extend this class
 * Common Connection Handler code could be factored into this class.
 * At present(April 28' 2001) there is not much in this class
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
public class BaseConnectionHandler extends AbstractLogEnabled implements Configurable {
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
        boolean autodetect = helloConf.getAttributeAsBoolean("autodetect", true);
        if (autodetect)
            helloName = hostName;
        else
            helloName = helloConf.getValue("localhost");
        getLogger().info("Hello Name is: " + helloName);
    }

    /**
     * Release a previously created ConnectionHandler e.g. for spooling.
     */
    public void releaseConnectionHandler(ConnectionHandler connectionHandler) {
    }
}
