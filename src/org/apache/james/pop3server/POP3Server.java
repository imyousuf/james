/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.pop3server;

import org.apache.avalon.blocks.*;
import org.apache.avalon.*;
import org.apache.java.util.*;
import org.apache.arch.*;
import org.apache.james.*;
import java.net.*;
import java.util.Date;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class POP3Server implements SocketHandler, Block {

    private ComponentManager comp;
    private Configuration conf;
    private Logger logger;
    private ThreadManager threadManager;
    private SimpleComponentManager pop3CM;

    public POP3Server() {}

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
    }

	public void init() throws Exception {

        this.logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        logger.log("POP3Server init...", "POP3Server", logger.INFO);
        this.threadManager = (ThreadManager) comp.getComponent(Interfaces.THREAD_MANAGER);
        Store store = (Store) comp.getComponent(Interfaces.STORE);
        Store.Repository mailUsers = (Store.Repository) store.getPublicRepository("MailUsers");
        logger.log("Public Repository MailUsers opened", "POP3Server", logger.INFO);
        pop3CM = new SimpleComponentManager(comp);
        pop3CM.put("mailUsers", mailUsers);
        logger.log("POP3Server ...init end", "POP3Server", logger.INFO);
    }

    public void parseRequest(Socket s) {

        try {
            POP3Handler handler = new POP3Handler();
            handler.setConfiguration(conf.getConfiguration("pop3handler"));
            handler.setComponentManager(pop3CM);
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
    
