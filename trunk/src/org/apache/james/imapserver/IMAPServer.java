/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.avalon.configuration.Configurable;
import org.apache.avalon.configuration.Configuration;
import org.apache.avalon.configuration.ConfigurationException;
import org.apache.cornerstone.services.connection.AbstractService;
import org.apache.cornerstone.services.connection.ConnectionHandlerFactory;
import org.apache.cornerstone.services.connection.DefaultHandlerFactory;

/**
 * The Server listens on a specified port and passes connections to a
 * ConnectionHandler. In this implementation, each ConnectionHandler runs in
 * its own thread.
 *
 * @version o.1 on 14 Dec 2000
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author  <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 */
public class IMAPServer 
    extends AbstractService {

    protected ConnectionHandlerFactory createFactory()
    {
        return new DefaultHandlerFactory( SingleThreadedConnectionHandler.class );
    }

    public void configure( final Configuration configuration )
        throws ConfigurationException {

        m_port = configuration.getChild( "port" ).getValueAsInt( 143 );

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

        final String useTLS = configuration.getChild("useTLS").getValue( "" );
        if( useTLS.equals( "TRUE" ) ) m_serverSocketType = "ssl";

        super.configure( configuration.getChild( "imaphandler" ) );
    }

    public void init() throws Exception {

        getLogger().info( "IMAPServer init..." );
        getLogger().info( "IMAPListener using " + m_serverSocketType + " on port " + m_port );
        super.init();
        getLogger().info("IMAPServer ...init end");
        System.out.println("Started IMAP Server "+m_connectionName);
    }
}
    
