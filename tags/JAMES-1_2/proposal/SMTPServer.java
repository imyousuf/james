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
        logger.log("SMTPServer init...", "SMTP", logger.INFO);
        threadManager = (ThreadManager) comp.getComponent(Interfaces.THREAD_MANAGER);
        SocketServer socketServer = (SocketServer) comp.getComponent(Interfaces.SOCKET_SERVER);
        int port = conf.getConfiguration("port").getValueAsInt(25);
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
	String typeMsg = "SMTPListener using " + type + " on port " + port;
        logger.log(typeMsg, "SMTP", logger.INFO);

        socketServer.openListener("SMTPListener", type, port, bind, this);

        //socketServer.openListener("SMTPListener", SocketServer.DEFAULT, port, bind, this);
        logger.log("SMTPServer ...init end", "SMTP", logger.INFO);
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
            logger.log("Executing handler.", "SMTP", logger.DEBUG);
        } catch (Exception e) {
            logger.log("Cannot parse request on socket " + s + " : " + e.getMessage(), "SMTP", logger.ERROR);
            e.printStackTrace();
        }
    }

    public void destroy()
    throws Exception {
    }
}
    
