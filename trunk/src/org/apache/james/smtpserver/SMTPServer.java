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
import org.apache.avalon.util.lang.ThreadManager;
import org.apache.avalon.util.thread.ThreadPool;
import org.apache.cornerstone.services.SocketServer;

import org.apache.james.*;
import org.apache.log.LogKit;
import org.apache.log.Logger;


/**
 * @version 1.1.0, 06/02/2001
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author  Matthew Pangaro <mattp@lokitech.com>
 */
public class SMTPServer implements SocketServer.SocketHandler, Configurable, Composer, Contextualizable {

    private Context context;
    private Configuration conf;
    private ComponentManager compMgr;
    private Logger logger =  LogKit.getLoggerFor("james.SMTPServer");
    private ThreadPool threadPool;
    private String handlerStartMesg = "Executing handler";
    
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
	threadPool = ThreadManager.getWorkerPool("whateverNameYouFancy");
        SocketServer socketServer = (SocketServer) compMgr.lookup("org.apache.cornerstone.services.SocketServer");
        int port = conf.getChild("port").getValueAsInt(25);
        InetAddress bind = null;
        try {
            String bindTo = conf.getChild("bind").getValue();
            if (bindTo.length() > 0) {
                bind = InetAddress.getByName(bindTo);
            }
	    //get the configured max message size so we can log it
	    long limit =
		conf.getChild("smtphandler").getChild("maxmessagesize").getValueAsLong(0);
	    if (limit > 0) {
		handlerStartMesg += " with message size limit : " + limit 
		    + " KBytes";
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
            threadPool.execute((Runnable)smtpHandler);
            logger.debug(handlerStartMesg);
        } catch (Exception e) {
            logger.error("Cannot parse request on socket " + s + " : "
			 + e.getMessage());
            e.printStackTrace();
        }
    }

    public void destroy() throws Exception {
    }
}
    
