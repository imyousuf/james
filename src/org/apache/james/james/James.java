/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.james;

import org.apache.avalon.blocks.*;
import org.apache.avalon.*;
import org.apache.arch.*;
import org.apache.james.*;
import java.net.*;
import java.io.*;
import javax.mail.internet.*;
import javax.mail.Session;
import java.util.Date;
import java.util.*;
import org.apache.mail.MessageContainer;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class James implements MailServer, Block {

    private ComponentManager comp;
    private Configuration conf;
    private SimpleComponentManager smtpServerCM;
    private Logger logger;
    private ThreadManager threadManager;
    private Store store;
    private MessageContainerRepository spool;
    private Store.Repository localInbox;
    private Store.Repository mailUsers;
    
    private static long count;
    
    private String serverName;
    
    public James() {
    }

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = comp;
    }

	public void init() throws Exception {

        smtpServerCM = new SimpleComponentManager(comp);

        this.logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        logger.log("JAMES init...", "JAMES", logger.INFO);
        this.serverName = conf.getConfiguration("servername", "SERVERNAME-NOT-FOUND").getValue();
        this.threadManager = (ThreadManager) comp.getComponent(Interfaces.THREAD_MANAGER);
        this.store = (Store) comp.getComponent(Interfaces.STORE);

        try {
            this.localInbox = (Store.Repository) store.getPublicRepository("localInbox");
        } catch (RuntimeException e) {
            logger.log("Cannot open public Repository LocalInbox", "JAMES", logger.ERROR);
            throw e;
        }
        logger.log("Public Repository LocalInbox opened", "JAMES", logger.INFO);
        smtpServerCM.put("localInbox", localInbox);

        try {
            this.mailUsers = (Store.Repository) store.getPublicRepository("MailUsers");
        } catch (RuntimeException e) {
            logger.log("Cannot open public Repository MailUsers", "JAMES", logger.ERROR);
            throw e;
        }
        logger.log("Public Repository MailUsers opened", "JAMES", logger.INFO);
        smtpServerCM.put("mailUsers", mailUsers);

        String spoolRepository = conf.getConfiguration("spoolRepository", ".").getValue();
        try {
            this.spool = (MessageContainerRepository) store.getPrivateRepository(spoolRepository, MessageContainerRepository.MESSAGE_CONTAINER, Store.ASYNCHRONOUS);
        } catch (RuntimeException e) {
            logger.log("Cannot open private MessageContainerRepository", "JAMES", logger.ERROR);
            throw e;
        }
        logger.log("Private MessageContainerRepository Spool opened", "JAMES", logger.INFO);
        smtpServerCM.put("spool", spool);

        int threads = conf.getConfiguration("spoolmanagerthreads", "1").getValueAsInt();
        while (threads-- > 0) {
            try {
                JamesSpoolManager spoolMgr = new JamesSpoolManager();
                spoolMgr.setConfiguration(conf.getConfiguration("spoolmanager"));
                spoolMgr.setComponentManager(smtpServerCM);
                spoolMgr.init();
                threadManager.execute((Stoppable) spoolMgr);
            } catch (Exception e) {
                logger.log("Exception in SpoolManager thread-" + threads + " init: " + e.getMessage(), "JAMES", logger.ERROR);
                throw e;
            }
            logger.log("SpoolManager " + (threads + 1) + " started", "JAMES", logger.INFO);
        }
        logger.log("JAMES ...init end", "JAMES", logger.INFO);
    }

/*    public OutputStream sendMail(String sender, Vector recipients) {

        return spool.addMessage(getMessageId(), sender, recipients);
    }*/

    public void sendMail(String sender, Vector recipients, MimeMessage message) {
        try {
            String id = getMessageId();
            spool.store(id, sender, recipients, message);
            logger.log("Sending mail " + id, "JAMES", logger.INFO);
        } catch (Exception e) {
        }
    }

    public void sendMail(String sender, Vector recipients, String message) {
        try {
            MimeMessage msg = new MimeMessage(Session.getDefaultInstance(System.getProperties(), null));
            msg.setText(message);
            msg.saveChanges();
            sendMail(sender, recipients, msg);
        } catch (Exception e) {
        }
    }

    public void destroy()
    throws Exception {
    }

    private String getMessageId() {
        return new String(new Date().getTime() + "." + count++ + "@" + serverName);
    }

}
    
