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

    /**
     * Whether this service is enabled
     */
    private volatile boolean enabled = true;

    protected ConnectionHandlerFactory createFactory() {
        return new DefaultHandlerFactory(NNTPHandler.class);
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(final Configuration configuration) throws ConfigurationException {
        enabled = configuration.getAttributeAsBoolean("enabled", true);
        if (enabled) {
            m_port = configuration.getChild("port").getValueAsInteger(119);
            try {
                String bindAddress = configuration.getChild("bind").getValue(null);
                if (null != bindAddress)
                    m_bindTo = InetAddress.getByName(bindAddress);
            } catch (final UnknownHostException unhe) {
                throw new ConfigurationException("Malformed bind parameter", unhe);
            }
            final boolean useTLS = configuration.getChild("useTLS").getValueAsBoolean(false);
            if (useTLS) {
                m_serverSocketType = "ssl";
            }
            super.configure(configuration.getChild("handler"));
            boolean authRequired =
                configuration.getChild("handler").getChild("authRequired").getValueAsBoolean(false);
            if (getLogger().isDebugEnabled()) {
                if (authRequired) {
                    getLogger().debug("NNTP Server requires authentication.");
                } else {
                    getLogger().debug("NNTP Server doesn't require authentication.");
                }
            }
            if (getLogger().isInfoEnabled()) {
                getLogger().info("Configured NNTPServer to run at : " + m_port);
            }
        }
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        if (enabled) {
            super.initialize();
            System.out.println("NNTP Server Started " + m_connectionName);
            getLogger().info("NNTP Server Started " + m_connectionName);
        } else {
            getLogger().info("NNTP Server Disabled");
            System.out.println("NNTP Server Disabled");
        }
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose() {
        if (enabled) {
            super.dispose();
        }
    }
}
