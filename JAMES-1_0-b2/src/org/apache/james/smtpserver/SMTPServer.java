/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.smtpserver;

import org.apache.avalon.blocks.*;
import org.apache.avalon.*;
import org.apache.arch.*;
import org.apache.james.*;
import java.net.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class SMTPServer implements SocketServer.SocketHandler, Configurable, Composer, Service, Contextualizable {

    private ComponentManager comp;
    private Configuration conf;
    private Logger logger;
    private ThreadManager threadManager;
    private Context context;
    
    public SMTPServer() {
    }

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
    }
    
    public void setContext(Context context) {
        this.context = context;
    }

	public void init() throws Exception {

        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        logger.log("SMTPServer init...", "SMTPServer", logger.INFO);
        threadManager = (ThreadManager) comp.getComponent(Interfaces.THREAD_MANAGER);
        SocketServer socketServer = (SocketServer) comp.getComponent(Interfaces.SOCKET_SERVER);
        socketServer.openListener("SMTPListener", SocketServer.DEFAULT, conf.getConfiguration("port", "25").getValueAsInt(), this);
        logger.log("SMTPServer ...init end", "SMTPServer", logger.INFO);
    }

    public void parseRequest(Socket s) {

        try {
            SMTPHandler smtpHandler = new SMTPHandler();
            smtpHandler.setConfiguration(conf.getConfiguration("smtphandler"));
            smtpHandler.setContext(context);
            smtpHandler.setComponentManager(comp);
            smtpHandler.init();
            smtpHandler.parseRequest(s);
            threadManager.execute(smtpHandler);
            logger.log("Executing handler.", "SMTPServer", logger.DEBUG);
        } catch (Exception e) {
            logger.log("Cannot parse request on socket " + s + " : " + e.getMessage(), "SMTPServer", logger.ERROR);
            e.printStackTrace();
        }
    }

    public void destroy()
    throws Exception {
    }
}
    
