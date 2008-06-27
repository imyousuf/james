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

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * NNTP Server Protocol Handler
 *
 * @author Harmeet Bedi <harmeet@kodemuse.com>
 */
public class NNTPServer extends AbstractService {

    protected ConnectionHandlerFactory createFactory() {
        return new DefaultHandlerFactory( NNTPHandler.class );
    }

    public void configure( final Configuration configuration )
        throws ConfigurationException
    {
        //System.out.println(getClass().getName()+": configure");
        m_port = configuration.getChild( "port" ).getValueAsInteger( 119 );

        try {
            String bindAddress = configuration.getChild( "bind" ).getValue( null );
            if( null != bindAddress )
                m_bindTo = InetAddress.getByName( bindAddress );
        } catch( final UnknownHostException unhe ) {
            throw new ConfigurationException( "Malformed bind parameter", unhe );
        }

        final boolean useTLS = configuration.getChild("userTLS").getValueAsBoolean( false );
        if ( useTLS )
        {
            m_serverSocketType = "ssl";
        }

        super.configure( configuration.getChild( "handler" ) );
        getLogger().info("configured NNTPServer to run at : "+m_port);
    }

    public void initialize() throws Exception {
        //System.out.println(getClass().getName()+": init");
        super.initialize();
        System.out.println("Started NNTP Server "+m_connectionName);
    }
}
