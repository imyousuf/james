/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.smtpserver;
import org.apache.avalon.cornerstone.services.connection.AbstractService;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory;
import org.apache.avalon.cornerstone.services.connection.DefaultHandlerFactory;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.component.Component;
import org.apache.mailet.MailetContext;
import org.apache.james.Constants;
import java.net.InetAddress;
import java.net.UnknownHostException;
/**
 * <p>Accepts SMTP connections on a server socket and dispatches them to SMTPHandlers.</p>
 *
 * <p>Also responsible for loading and parsing SMTP specific configuration.</p>
 *
 * @version 1.1.0, 06/02/2001
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author  Matthew Pangaro <mattp@lokitech.com>
 * @author  <a href="mailto:donaldp@apache.org">Peter Donald</a>
 * @author  <a href="mailto:danny@apache.org">Danny Angus</a>
 */
/*
 * IMPORTANT: SMTPServer extends AbstractService.  If you implement ANY
 * lifecycle methods, you MUST call super.<method> as well.
 */
public class SMTPServer extends AbstractService implements Component {
    /**
     * The mailet context - we access it here to set the hello name for the Mailet API
     */
    MailetContext mailetcontext;

    /**
     * Whether this service is enabled
     */
    private volatile boolean enabled = true;

    protected ConnectionHandlerFactory createFactory() {
        return new DefaultHandlerFactory(SMTPHandler.class);
    }

    /**
     * @see org.apache.avalon.framework.component.Composable#compose(ComponentManager)
     */
    public void compose(final ComponentManager componentManager) throws ComponentException {
        super.compose(componentManager);
        mailetcontext = (MailetContext) componentManager.lookup("org.apache.mailet.MailetContext");
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(final Configuration configuration) throws ConfigurationException {
        enabled = configuration.getAttributeAsBoolean("enabled", true);
        if (enabled) {
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
            // make our "helloName" available through the MailetContext
            String helloName = SMTPHandler.configHelloName(configuration.getChild("handler"));
            mailetcontext.setAttribute(Constants.HELLO_NAME, helloName);
        }
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        if (enabled) {
            getLogger().info("SMTPServer init...");
            super.initialize();
            StringBuffer logBuffer =
                new StringBuffer(128)
                    .append("SMTPServer using ")
                    .append(m_serverSocketType)
                    .append(" on port ")
                    .append(m_port)
                    .append(" at ")
                    .append(m_bindTo);
            getLogger().info(logBuffer.toString());
            getLogger().info("SMTPServer ...init end");
            System.out.println("SMTP Server Started " + m_connectionName);
        } else {
            getLogger().info("SMTP Server Disabled");
            System.out.println("SMTP Server Disabled");
        }
    }

    /**
     * @see org.apache.avalon.framework.activity.Disposable#dispose()
     */
    public void dispose() {
        if (enabled) {
            getLogger().info("SMTPServer dispose...");
            getLogger().info("SMTPServer dispose..." + m_connectionName);
            super.dispose();
            // This is needed to make sure sockets are promptly closed on Windows 2000
            System.gc();
            getLogger().info("SMTPServer ...dispose end");
        }
    }
}
