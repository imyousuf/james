/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.pop3server;
import org.apache.avalon.cornerstone.services.connection.AbstractService;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory;
import org.apache.avalon.cornerstone.services.connection.DefaultHandlerFactory;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.component.Component;
import java.net.InetAddress;
import java.net.UnknownHostException;
/**
 * <p>Accepts POP3 connections on a server socket and dispatches them to POP3Handlers.</p>
 *
 * <p>Also responsible for loading and parsing POP3 specific configuration.</p>
 *
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author  <a href="mailto:danny@apache.org">Danny Angus</a>
 */
public class POP3Server extends AbstractService implements Component {
    private boolean enabled = true;
    /**
     * Creates a subclass specific handler factory for use by the superclass.
     *
     * @return a ConnectionHandlerFactory that produces POP3Handlers
     */
    protected ConnectionHandlerFactory createFactory() {
        return new DefaultHandlerFactory(POP3Handler.class);
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
            m_port = configuration.getChild("port").getValueAsInteger(25);
            try {
                final String bindAddress = configuration.getChild("bind").getValue(null);
                if (null != bindAddress) {
                    m_bindTo = InetAddress.getByName(bindAddress);
                }
            } catch (final UnknownHostException unhe) {
                throw new ConfigurationException("Malformed bind parameter", unhe);
            }
            final boolean useTLS = configuration.getChild("useTLS").getValueAsBoolean(false);
            if (useTLS) {
                m_serverSocketType = "ssl";
            }
            super.configure(configuration.getChild("handler"));
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
            getLogger().info("POP3Server init...");
            StringBuffer logBuffer =
                new StringBuffer(128)
                    .append("POP3Listener using ")
                    .append(m_serverSocketType)
                    .append(" on port ")
                    .append(m_port)
                    .append(" at ")
                    .append(m_bindTo);
            getLogger().info(logBuffer.toString());
            super.initialize();
            getLogger().info("POP3Server ...init end");
            System.out.println("POP3 Server Started " + m_connectionName);
        } else {
            getLogger().info("POP3Server Disabled");
            System.out.println("POP3 Server Disabled");
        }
    }
    /**
     * The dispose operation is called at the end of a components lifecycle.
     * Instances of this class use this method to release and destroy any
     * resources that they own.
     *
     * @throws Exception if an error is encountered during shutdown
     */
    public void dispose() {
        if (enabled) {
            getLogger().info("POP3Server dispose...");
            getLogger().info("POP3Server dispose..." + m_connectionName);
            super.dispose();
            // This is needed to make sure sockets are promptly closed on Windows 2000
            System.gc();
            getLogger().info("POP3Server ...dispose end");
        }
    }
}
