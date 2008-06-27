/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.pop3server;

import java.net.*;
import java.util.Date;

import org.apache.avalon.*;
import org.apache.avalon.services.*;
import org.apache.avalon.util.lang.*;

import org.apache.james.*;
import org.apache.james.util.InternetPrintWriter;
import org.apache.log.LogKit;
import org.apache.log.Logger;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class POP3Server implements SocketServer.SocketHandler, Configurable, Composer, Service, Contextualizable {

    private Context context;
    private Configuration conf;
    private ComponentManager compMgr;
    private WorkerPool workerPool;
    private Logger logger =  LogKit.getLoggerFor("james.POP3Server");

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

        logger.info("POP3Server init...");
	
	workerPool = ThreadManager.getWorkerPool("whateverNameYouFancy");
        SocketServer socketServer = (SocketServer) compMgr.lookup("org.apache.avalon.services.SocketServer");
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
	logger.info("POP3Listener using " + type + " on port " + port);

        socketServer.openListener("POP3Listener", type, port, bind, this);
        logger.info("POP3Server ...init end");
    }

    public void parseRequest(Socket s) {

        try {
            POP3Handler handler = new POP3Handler();
            handler.configure(conf.getChild("pop3handler"));
            handler.contextualize(context);
            handler.compose(compMgr);
            handler.init();
            handler.parseRequest(s);
            workerPool.execute((Runnable) handler);
        } catch (Exception e) {
            logger.error("Cannot parse request on socket " + s + " : "
			 + e.getMessage());
        }
    }

    public void destroy() throws Exception {
    }
}
    
