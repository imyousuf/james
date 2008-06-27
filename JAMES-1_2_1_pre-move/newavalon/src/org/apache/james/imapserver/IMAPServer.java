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

import org.apache.avalon.*;
import org.apache.avalon.services.*;
import org.apache.avalon.util.lang.*;
//import org.apache.avalon.utils.*;
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
public class IMAPServer implements SocketServer.SocketHandler, Configurable, Composer, Service, Contextualizable {

    private Context context;
    private Configuration conf;
    private ComponentManager compMgr;
    private WorkerPool workerPool;
    private Logger logger =  LogKit.getLoggerFor("james.IMAPServer");

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

        logger.info("IMAPServer init...");

	workerPool = ThreadManager.getWorkerPool("whateverNameYouFancy");
        SocketServer socketServer = (SocketServer) compMgr.lookup("org.apache.avalon.services.SocketServer");
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
	logger.info("IMAPListener using " + type + " on port " + port);

        socketServer.openListener("IMAPListener", type, port, bind, this);
        logger.info("IMAPServer ...init end");
    }

    public void parseRequest(Socket s) {

        try {
            ConnectionHandler handler = new SingleThreadedConnectionHandler();
            handler.configure(conf.getChild("imaphandler"));
            handler.contextualize(context);
            handler.compose(compMgr);
            handler.init();
            handler.parseRequest(s);
            workerPool.execute((Runnable) handler);
        } catch (Exception e) {
            logger.error("IMAPServer: Cannot parse request on socket " + s + " : " + e.getMessage());
        }
    }

    public void destroy() throws Exception {
    }
}
    
