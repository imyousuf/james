/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/
package org.apache.james.smtpserver;

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.arch.*;
import org.apache.avalon.blocks.*;
import org.apache.java.util.*;
import org.apache.james.*;

/**
 * @author Serge Knystautas <sergek@lokitech.com>
 * @author Federico Barbieri <scoobie@systemy.it>
 * @version 0.9
 */ 
public class JamesSpoolManager implements Component, Composer, Configurable, Stoppable, Service {

    private ComponentManager comp;
    private Configuration conf;
    private MessageSpool spool;
    private Logger logger;
    private LocalAgent localAgent;
    private RemoteAgent remoteAgent;
    private Vector unknownRecipients;

    /**
     * SpoolManager constructor comment.
     */
    public JamesSpoolManager() {
    }

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
    }

	public void init() throws Exception {

        this.logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        logger.log("JamesSpoolManager init...", "SMTPServer", logger.INFO);
        this.spool = (MessageSpool) comp.getComponent("spool");
        this.unknownRecipients = new Vector();
        try {
            this.localAgent = new LocalAgent();
            this.localAgent.setConfiguration(conf.getConfiguration("localAgent"));
            this.localAgent.setComponentManager(comp);
        } catch (Exception e) {
            logger.log("JamesSpoolManager: Exception in localAgent: " + e.getMessage(), "SMTPServer", logger.ERROR);
        }
        
        try {
            this.remoteAgent = new RemoteAgent();
            this.remoteAgent.setConfiguration(conf.getConfiguration("remoteAgent"));
            this.remoteAgent.setComponentManager(comp);
        } catch (Exception e) {
            logger.log("JamesSpoolManager: Exception in remoteAgent: " + e.getMessage(), "SMTPServer", logger.ERROR);
        }
    }


    /**
     * This routinely checks the message spool for messages, and processes them as necessary
     */
    public void run() {

        logger.log("run JamesSpoolManager", "SMTPServer", logger.INFO);

        String key;
        while(true) {

            try {
                logger.log("running JamesSpoolManager", "SMTPServer", logger.INFO);

                key = spool.accept();
                MessageContainer mc = spool.retrieve(key);
                DeliveryState state = mc.getState();
                Vector recipients = state.getRecipients();
                while (!recipients.isEmpty()) {
                    for (Enumeration e = recipients.elements(); e.hasMoreElements(); ) {
                        String recipient = (String) e.nextElement();
                        if(!localAgent.isLocal(recipient) && !remoteAgent.isRemote(recipient)) {
                            unknownRecipients.addElement(recipient);
                            recipients.removeElementAt(recipients.indexOf(recipient));
                        }
                    }
                    localAgent.delivery(mc);
                    remoteAgent.delivery(mc);
                }
                // if (!unknownRecipients.isEmpty()) store it to some errorRepository and log error.
                mc.getBodyInputStream().close();
                spool.remove(key);
            } catch (Exception e) {
                logger.log("exception in run JamesSpoolManager " + e.getMessage(), "SMTPServer", logger.ERROR);
                e.printStackTrace();
            }
        }
    }
    
    private boolean isValid(String recipient) {
        return true;
    }
    
    public void stop() {
    }
    
    public void destroy()
    throws Exception {
    }
}