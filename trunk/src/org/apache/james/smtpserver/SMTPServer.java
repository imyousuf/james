/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.smtpserver;

import java.net.*;

import org.apache.avalon.*;
import org.apache.avalon.services.*;
import org.apache.avalon.util.lang.*;

import org.apache.james.*;
import org.apache.log.LogKit;
import org.apache.log.Logger;


/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class SMTPServer implements SocketServer.SocketHandler, Configurable, Composer, Service, Contextualizable {

    private Context context;
    private Configuration conf;
    private ComponentManager compMgr;
    private Logger logger =  LogKit.getLoggerFor("james.SMTPServer");
    private WorkerPool workerPool;
 
    
    public void configure(Configuration conf) throws ConfigurationException{
        this.conf = conf;
    }
    
    public void compose(ComponentManager comp) {
        compMgr = comp;
    }
    
    public void contextualize(Context context) {
        this.context = context;
    }

    public void init() throws Exception {

        logger.info("SMTPServer init...");
	//int threadPool = conf.getChild("ThreadPoolSize").getVaueAsInt();
	workerPool = ThreadManager.getWorkerPool("whateverNameYouFancy");
        SocketServer socketServer = (SocketServer) compMgr.lookup("org.apache.avalon.services.SocketServer");
        int port = conf.getChild("port").getValueAsInt(25);
        InetAddress bind = null;
        try {
            String bindTo = conf.getChild("bind").getValue();
            if (bindTo.length() > 0) {
                bind = InetAddress.getByName(bindTo);
            }
        } catch (ConfigurationException e) {
        }
        socketServer.openListener("SMTPListener", SocketServer.DEFAULT, port, bind, this);
        logger.info("SMTPServer ...init end");
    }

    public void parseRequest(Socket s) {

        try {
            SMTPHandler smtpHandler = new SMTPHandler();
            smtpHandler.configure(conf.getChild("smtphandler"));
            smtpHandler.contextualize(context);
            smtpHandler.compose(compMgr);
            smtpHandler.init();
            smtpHandler.parseRequest(s);
            workerPool.execute((Runnable)smtpHandler);
            logger.debug("Executing handler.");
        } catch (Exception e) {
            logger.error("Cannot parse request on socket " + s + " : "
			 + e.getMessage());
            e.printStackTrace();
        }
    }

    public void destroy() throws Exception {
    }
}
    
