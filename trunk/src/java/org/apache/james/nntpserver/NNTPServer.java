/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver;
import org.apache.avalon.cornerstone.services.connection.AbstractService;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory;
import org.apache.avalon.cornerstone.services.connection.DefaultHandlerFactory;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.component.Component;
import java.net.InetAddress;
import java.net.UnknownHostException;
/**
 * NNTP Server Protocol Handler
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 * @author  <a href="mailto:danny@apache.org">Danny Angus</a>
 */
public class NNTPServer extends AbstractService implements Component {
    private boolean enabled = true;
    protected ConnectionHandlerFactory createFactory() {
        return new DefaultHandlerFactory(NNTPHandler.class);
    }
    /**
     * Pass the <code>Configuration</code> to the instance.
     *
     * @param configuration the class configurations.
     * @throws ConfigurationException if an error occurs
     */
    public void configure(final Configuration configuration) throws ConfigurationException {
        if (configuration.getAttribute("enabled").equalsIgnoreCase("false")) {
            enabled = false;
        } else {
            m_port = configuration.getChild("port").getValueAsInteger(119);
            try {
                String bindAddress = configuration.getChild("bind").getValue(null);
                if (null != bindAddress)
                    m_bindTo = InetAddress.getByName(bindAddress);
            } catch (final UnknownHostException unhe) {
                throw new ConfigurationException("Malformed bind parameter", unhe);
            }
            final boolean useTLS = configuration.getChild("userTLS").getValueAsBoolean(false);
            if (useTLS) {
                m_serverSocketType = "ssl";
            }
            super.configure(configuration.getChild("handler"));
            if (getLogger().isInfoEnabled()) {
                getLogger().info("configured NNTPServer to run at : " + m_port);
            }
        }
    }
    /**
     * Initialize the component. Initialization includes
     * allocating any resources required throughout the
     * components lifecycle.
     *
     * @throws Exception if an error occurs
     */
    public void initialize() throws Exception {
        if (enabled) {
            super.initialize();
            System.out.println("NNTP Server Started " + m_connectionName);
        } else {
            getLogger().info("NNTP Server Disabled");
            System.out.println("NNTP Server Disabled");
        }
    }
    public void dispose() {
        if (enabled) {
            super.dispose();
        }
    }    
}
