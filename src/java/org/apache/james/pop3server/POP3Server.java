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
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class POP3Server
    extends AbstractService implements Component {

    protected ConnectionHandlerFactory createFactory()
    {
        return new DefaultHandlerFactory( POP3Handler.class );
    }

    public void configure( final Configuration configuration )
        throws ConfigurationException {

        m_port = configuration.getChild( "port" ).getValueAsInteger( 25 );

        try
        {
            final String bindAddress = configuration.getChild( "bind" ).getValue( null );
            if( null != bindAddress )
            {
                m_bindTo = InetAddress.getByName( bindAddress );
            }
        }
        catch( final UnknownHostException unhe )
        {
            throw new ConfigurationException( "Malformed bind parameter", unhe );
        }

        final boolean useTLS = configuration.getChild("useTLS").getValueAsBoolean( false );
        if( useTLS ) {
            m_serverSocketType = "ssl";
        }

        super.configure( configuration.getChild( "handler" ) );
    }

    public void initialize() throws Exception {
        getLogger().info( "POP3Server init..." );
        StringBuffer logBuffer =
            new StringBuffer(128)
                .append("POP3Listener using ")
                .append(m_serverSocketType)
                .append(" on port ")
                .append(m_port)
                .append(" at ")
                .append(m_bindTo);
        getLogger().info( logBuffer.toString() );
        super.initialize();
        getLogger().info( "POP3Server ...init end" );
        System.out.println("Started POP3 Server " + m_connectionName);
    }

    public void dispose()
    {
        getLogger().info( "POP3Server dispose..." );
        getLogger().info( "POP3Server dispose..." + m_connectionName);
        super.dispose();

        // This is needed to make sure sockets are promptly closed on Windows 2000
        System.gc();

        getLogger().info( "POP3Server ...dispose end" );
    }

}

