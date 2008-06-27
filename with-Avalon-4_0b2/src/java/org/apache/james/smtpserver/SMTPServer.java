/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.smtpserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.cornerstone.services.connection.AbstractService;
import org.apache.avalon.cornerstone.services.connection.ConnectionHandlerFactory;
import org.apache.avalon.cornerstone.services.connection.DefaultHandlerFactory;

/**
 *
 * @version 1.1.0, 06/02/2001
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author  Matthew Pangaro <mattp@lokitech.com>
 * @author  <a href="mailto:donaldp@apache.org">Peter Donald</a>
 */
public class SMTPServer 
    extends AbstractService {

    protected ConnectionHandlerFactory createFactory()
    {
        return new DefaultHandlerFactory( SMTPHandler.class );
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

        final String useTLS = configuration.getChild("useTLS").getValue( "" );
        if( useTLS.equals( "TRUE" ) ) m_serverSocketType = "ssl";
 
       super.configure( configuration.getChild( "handler" ) );
    }

    public void initialize() throws Exception {
        getLogger().info("SMTPServer init...");
        super.initialize();
        getLogger().info("SMTPServer ...init end");
        System.out.println("Started SMTP Server "+m_connectionName);
    }
}
    
