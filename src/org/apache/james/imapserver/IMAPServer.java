/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imapserver;



import java.net.*;
import java.util.Date;
import org.apache.avalon.Loggable;
import org.apache.avalon.AbstractLoggable;
import org.apache.avalon.Contextualizable;
import org.apache.avalon.Context;
import org.apache.avalon.Composer;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.configuration.Configurable;
import org.apache.avalon.configuration.Configuration;
import org.apache.avalon.configuration.ConfigurationException;
import org.apache.avalon.Component;
import org.apache.avalon.util.lang.*;
import org.apache.avalon.util.thread.ThreadPool;
import org.apache.cornerstone.services.SocketServer;
import org.apache.james.*;
import org.apache.log.LogKit;
import org.apache.log.Logger;

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
    extends AbstractLoggable
    implements SocketServer.SocketHandler,  Component, Configurable, Composer, Contextualizable {

    private Context context;
    private Configuration conf;
    private ComponentManager compMgr;
    private ThreadPool threadPool;

    public void configure(Configuration conf) throws ConfigurationException {
        this.conf = conf;
    }
    
    public void contextualize(Context context) {
        this.context = context;
    }
    
    public void compose(ComponentManager comp) {
        compMgr = comp;
    }

    public void init() throws Exception {

        getLogger().info("IMAPServer init...");

        threadPool = ThreadManager.getWorkerPool("whateverNameYouFancy");
        SocketServer socketServer = (SocketServer) compMgr.lookup("org.apache.cornerstone.services.SocketServer");
        int port = conf.getChild("port").getValueAsInt(143);

        InetAddress bind = null;
        try {
            String bindTo = conf.getChild("bind").getValue();
            if (bindTo.length() > 0) {
                bind = InetAddress.getByName(bindTo);
            }
        } catch (ConfigurationException e) {
        }

        String type = SocketServer.DEFAULT;
        try {
            if (conf.getChild("useTLS").getValue().equals("TRUE")) type = SocketServer.TLS;
        } catch (ConfigurationException e) {
        }
        getLogger().info("IMAPListener using " + type + " on port " + port);

        socketServer.openListener("IMAPListener", type, port, bind, this);
        getLogger().info("IMAPServer ...init end");
    }

    public void parseRequest(Socket s) {

        try {
            ConnectionHandler handler = new SingleThreadedConnectionHandler();
            ((Loggable)handler).setLogger( getLogger() );
            handler.configure(conf.getChild("imaphandler"));
            handler.contextualize(context);
            handler.compose(compMgr);
            handler.init();
            handler.parseRequest(s);
            threadPool.execute((Runnable) handler);
        } catch (Exception e) {
            getLogger().error("IMAPServer: Cannot parse request on socket " + s + " : " + e.getMessage());
        }
    }
}
    
