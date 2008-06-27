/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/


package org.apache.james.imapserver;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * THIS FILE RETRIEVED FROM THE MAIN TRUNK ATTIC, TO ENABLE IMAP PROPOSAL
 * TO COMPILE - EXTENDED BY o.a.james.imapserver.BaseCommand.
 * TODO: Use the AbstractJamesService for ImapServer, and get rid of this file.
 *
 * Different connection handlers extend this class
 * Common Connection Handler code could be factored into this class.
 * At present(April 28' 2001) there is not much in this class
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
public class BaseConnectionHandler extends AbstractLogEnabled implements Configurable {

    /**
     * The default timeout for the connection
     */
    private static int DEFAULT_TIMEOUT = 1800000;

    /**
     * The timeout for the connection
     */
    protected int timeout = DEFAULT_TIMEOUT;

    /**
     * The hello name for the connection
     */
    protected String helloName;

    /**
     * Get the hello name for this server
     *
     * @param configuration a configuration object containing server name configuration info
     * @return the hello name for this server
     */
    public static String configHelloName(final Configuration configuration)
        throws ConfigurationException {
        String hostName = null;

        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch  (UnknownHostException ue) {
            // Default to localhost if we can't get the local host name.
            hostName = "localhost";
        }

        Configuration helloConf = configuration.getChild("helloName");
        boolean autodetect = helloConf.getAttributeAsBoolean("autodetect", true);

        return autodetect ? hostName : helloConf.getValue("localhost");
    }


    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( final Configuration configuration )
        throws ConfigurationException {

        timeout = configuration.getChild( "connectiontimeout" ).getValueAsInteger( DEFAULT_TIMEOUT );
        helloName = configHelloName(configuration);
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Hello Name is: " + helloName);
        }
    }

    /**
     * Release a previously created ConnectionHandler e.g. for spooling.
     *
     * @param connectionHandler the ConnectionHandler to be released
     */
    public void releaseConnectionHandler(ConnectionHandler connectionHandler) {
    }
}
