/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.pop3server;

import org.apache.avalon.*;
import org.apache.avalon.blocks.*;
import org.apache.avalon.utils.*;
import org.apache.james.*;
import java.net.*;
import java.util.Date;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class POP3Server implements SocketServer.SocketHandler, Configurable, Composer, Service, Contextualizable {

    private Context context;
    private Configuration conf;
    private ComponentManager comp;
    private ThreadManager threadManager;
    private Logger logger;

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
    }

    public void init() throws Exception {

        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        logger.log("POP3Server init...", "POP3", logger.INFO);
        this.threadManager = (ThreadManager) comp.getComponent(Interfaces.THREAD_MANAGER);
        SocketServer socketServer = (SocketServer) comp.getComponent(Interfaces.SOCKET_SERVER);
        int port = conf.getConfiguration("port").getValueAsInt(110);
        InetAddress bind = null;
        try {
            String bindTo = conf.getConfiguration("bind").getValue();
            if (bindTo.length() > 0) {
                bind = InetAddress.getByName(bindTo);
            }
        } catch (ConfigurationException e) {
        }

        String type = SocketServer.DEFAULT;
        try {
            if (conf.getConfiguration("useTLS").getValue().equals("TRUE")) type = SocketServer.TLS;
        } catch (ConfigurationException e) {
        }
        String typeMsg = "POP3Listener using " + type + " on port " + port;
        logger.log(typeMsg, "POP3", logger.INFO);

        socketServer.openListener("POP3Listener", type, port, bind, this);

        logger.log("POP3Server ...init end", "POP3", logger.INFO);
    }

    public void parseRequest(Socket s) {

        try {
            POP3Handler handler = new POP3Handler();
            handler.setConfiguration(conf.getConfiguration("pop3handler"));
            handler.setContext(context);
            handler.setComponentManager(comp);
            handler.init();
            handler.parseRequest(s);
            threadManager.execute((Runnable) handler);
        } catch (Exception e) {
            logger.log("Cannot parse request on socket " + s + " : " + e.getMessage(), "POP3", logger.ERROR);
        }
    }

    public void destroy() {
    }
}

