/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.nntpserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.avalon.Initializable;
import org.apache.avalon.component.Component;
import org.apache.avalon.component.ComponentException;
import org.apache.avalon.component.ComponentManager;
import org.apache.avalon.component.DefaultComponentManager;
import org.apache.avalon.configuration.Configuration;
import org.apache.avalon.configuration.ConfigurationException;
import org.apache.cornerstone.services.connection.AbstractService;
import org.apache.cornerstone.services.connection.ConnectionHandlerFactory;
import org.apache.cornerstone.services.connection.DefaultHandlerFactory;

/**
 * @author Harmeet <hbedi@apache.org>
 */
public class NNTPServer extends AbstractService {

    protected ConnectionHandlerFactory createFactory() {
        return new DefaultHandlerFactory( NNTPHandler.class );
    }

    public void configure( final Configuration configuration )
        throws ConfigurationException
    {
        //System.out.println(getClass().getName()+": configure");
        m_port = configuration.getChild( "port" ).getValueAsInt( 119 );

        try {
            String bindAddress = configuration.getChild( "bind" ).getValue( null );
            if( null != bindAddress )
                m_bindTo = InetAddress.getByName( bindAddress );
        } catch( final UnknownHostException unhe ) {
            throw new ConfigurationException( "Malformed bind parameter", unhe );
        }

        final String useTLS = configuration.getChild("useTLS").getValue( "" );
        if( useTLS.equals( "TRUE" ) )
            m_serverSocketType = "ssl";

        super.configure( configuration.getChild( "nntphandler" ) );
        getLogger().info("configured NNTPServer to run at : "+m_port);
    }

    public void init() throws Exception {
        //System.out.println(getClass().getName()+": init");
        super.init();
        System.out.println("Started NNTP Server");
    }
}
