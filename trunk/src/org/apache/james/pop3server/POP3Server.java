/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.pop3server;

import java.net.*;
import java.util.Date;
import org.apache.avalon.AbstractLoggable;
import org.apache.avalon.Contextualizable;
import org.apache.avalon.Context;
import org.apache.avalon.Composer;
import org.apache.avalon.ComponentManager;
import org.apache.avalon.configuration.Configurable;
import org.apache.avalon.configuration.Configuration;
import org.apache.avalon.configuration.ConfigurationException;
import org.apache.avalon.Component;
import org.apache.avalon.util.lang.ThreadManager;
import org.apache.avalon.util.thread.ThreadPool;
import org.apache.cornerstone.services.SocketServer;
import org.apache.james.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class POP3Server 
    extends AbstractLoggable
    implements SocketServer.SocketHandler, Component, Configurable, Composer, Contextualizable {

    private Context context;
    private Configuration conf;
    private ComponentManager compMgr;
    private ThreadPool threadPool;

    public void configure(Configuration conf) throws ConfigurationException {
        this.conf = conf;
    }
    
    public void compose(ComponentManager comp) {
        compMgr = comp;
    }
    
    public void contextualize(Context context) {
        this.context = context;
    }

    public void init() throws Exception {

        getLogger().info("POP3Server init...");
        
        threadPool = ThreadManager.getWorkerPool("whateverNameYouFancy");
        SocketServer socketServer = (SocketServer) compMgr.lookup("org.apache.cornerstone.services.SocketServer");
        int port = conf.getChild("port").getValueAsInt(110);
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
        getLogger().info("POP3Listener using " + type + " on port " + port);
        
        socketServer.openListener("POP3Listener", type, port, bind, this);
        getLogger().info("POP3Server ...init end");
    }

    public void parseRequest(Socket s) {

        try {
            POP3Handler handler = new POP3Handler();
            handler.configure(conf.getChild("pop3handler"));
            handler.contextualize(context);
            handler.compose(compMgr);
            handler.init();
            handler.parseRequest(s);
            threadPool.execute((Runnable) handler);
        } catch (Exception e) {
            getLogger().error("Cannot parse request on socket " + s + " : "
                              + e.getMessage());
        }
    }
}
    
