/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.bench;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.java.lang.*;
import org.apache.avalon.*;
import org.apache.avalon.interfaces.*;
import org.apache.mail.Mail;

import javax.mail.internet.*;
import javax.mail.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class Tester implements  Block {

    private ComponentManager comp;
    private Configuration conf;
    private Logger logger;
    private ThreadManager threadManager;
    
    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = new SimpleComponentManager(comp);
    }

	public void init() throws Exception {

        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        logger.log("TestJames init...", "Test", logger.INFO);
        threadManager = (ThreadManager) comp.getComponent(Interfaces.THREAD_MANAGER);
        int threads = conf.getConfiguration("children", "1").getValueAsInt();
        String target = conf.getConfiguration("target", "127.0.0.1").getValue();
        logger.log("Open connection to " + target, "Test", logger.INFO);
        while (threads-- > 0) {
            Caller caller = new Caller("caller" + threads, logger, target);
            threadManager.execute(caller);
        }
        logger.log("TestJames ...init end", "Test", logger.INFO);
    }
    
    public void destroy() {
    }
}