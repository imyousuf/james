/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.pop3server;

import org.apache.arch.*;
import org.apache.avalon.blocks.*;
import org.apache.java.util.*;
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

    public POP3Server() {}

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

        this.logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        logger.log("POP3Server init...", "POP3Server", logger.INFO);
        this.threadManager = (ThreadManager) comp.getComponent(Interfaces.THREAD_MANAGER);
        SocketServer socketServer = (SocketServer) comp.getComponent(Interfaces.SOCKET_SERVER);
        socketServer.openListener("POP3Listener", SocketServer.DEFAULT, conf.getConfiguration("port", "110").getValueAsInt(), this);
        logger.log("POP3Server ...init end", "POP3Server", logger.INFO);
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
            logger.log("Cannot parse request on socket " + s + " : " + e.getMessage(), "POP3Server", logger.ERROR);
        }
    }

    public void destroy() {
    }
}
    
