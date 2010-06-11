/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james;

import java.io.*;
import java.net.*;
import java.util.*;

import org.apache.arch.*;
import org.apache.avalon.*;
import org.apache.avalon.blocks.*;
import org.apache.mail.Mail;

import org.apache.james.transport.*;
import org.apache.james.smtpserver.*;
import org.apache.james.pop3server.*;
import org.apache.james.remotemanager.*;
import org.apache.james.usermanager.*;

import javax.mail.internet.*;
import javax.mail.Session;
import javax.mail.MessagingException;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class James implements MailServer, Block {

    private SimpleComponentManager comp;
    private SimpleContext context;
    private Configuration conf;
    private Logger logger;
    private ThreadManager threadManager;
    private Store store;
    private MailRepository spool;
    private MailRepository localInbox;
    
    public James() {
    }

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = new SimpleComponentManager(comp);
    }

	public void init() throws Exception {

        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        logger.log("JAMES init...", "JAMES", logger.INFO);
        threadManager = (ThreadManager) comp.getComponent(Interfaces.THREAD_MANAGER);
        store = (Store) comp.getComponent(Interfaces.STORE);

        context = new SimpleContext();
            // Get this server names 
        Vector serverNames = new Vector();
        for (Enumeration e = conf.getConfigurations("servernames.servername"); e.hasMoreElements(); ) {
            serverNames.addElement(((Configuration) e.nextElement()).getValue());
        }
        if (serverNames.isEmpty()) {
            try {
                serverNames.addElement(InetAddress.getLocalHost().getHostName());
            } catch (UnknownHostException ue) {
            }
        }
        serverNames.addElement("localhost");
        for (Enumeration e = serverNames.elements(); e.hasMoreElements(); ) {
            logger.log("Local host is: " + e.nextElement(), "JAMES", logger.INFO);
        }
        context.put(Constants.SERVER_NAMES, serverNames);
        context.put(Constants.HELO_NAME, serverNames.elementAt(0));
            // Get postmaster
        String postmaster = conf.getConfiguration("postmaster", "root@localhost").getValue();
        context.put(Constants.POSTMASTER, postmaster);
            // Get the LocalInbox repository
        String inboxRepository = conf.getConfiguration("inboxRepository", "file://../mail/inbox/").getValue();
        try {
            this.localInbox = (MailRepository) store.getPrivateRepository(inboxRepository, MailRepository.MAIL, Store.ASYNCHRONOUS);
        } catch (Exception e) {
            logger.log("Cannot open private MailRepository", "JAMES", logger.ERROR);
            throw e;
        }
        logger.log("Private Repository LocalInbox opened", "JAMES", logger.INFO);
            // Add this to comp
        comp.put(Interfaces.MAIL_SERVER, this);
        
        String spoolRepository = conf.getConfiguration("spoolRepository", "file://../mail/spool/").getValue();
        try {
            this.spool = (MailRepository) store.getPrivateRepository(spoolRepository, MailRepository.MAIL, Store.ASYNCHRONOUS);
        } catch (Exception e) {
            logger.log("Cannot open private MailRepository", "JAMES", logger.ERROR);
            throw e;
        }
        logger.log("Private MailRepository Spool opened", "JAMES", logger.INFO);
        comp.put(Constants.SPOOL_REPOSITORY, spool);

        UserManager userManager = new UserManager();
        try {
            userManager.setConfiguration(conf.getConfiguration("usersManager"));
            userManager.setContext(context);
            userManager.setComponentManager(comp);
        } catch (Exception e) {
            logger.log("Exception in UserManager init: " + e.getMessage(), "JAMES", logger.ERROR);
            throw e;
        }
        comp.put(Constants.USERS_MANAGER, userManager);
        
        POP3Server pop3Server = new POP3Server();
        try {
            pop3Server.setConfiguration(conf.getConfiguration("pop3Server"));
            pop3Server.setContext(context);
            pop3Server.setComponentManager(comp);
        } catch (Exception e) {
            logger.log("Exception in POP3Server init: " + e.getMessage(), "JAMES", logger.ERROR);
            throw e;
        }
        
        SMTPServer smtpServer = new SMTPServer();
        try {
            smtpServer.setConfiguration(conf.getConfiguration("smtpServer"));
            smtpServer.setContext(context);
            smtpServer.setComponentManager(comp);
        } catch (Exception e) {
            logger.log("Exception in SMTPServer init: " + e.getMessage(), "JAMES", logger.ERROR);
            throw e;
        }
        
        RemoteManager remoteAdmin = new RemoteManager();
        try {
            remoteAdmin.setConfiguration(conf.getConfiguration("remoteManager"));
            remoteAdmin.setComponentManager(comp);
        } catch (Exception e) {
            logger.log("Exception in RemoteAdmin init: " + e.getMessage(), "JAMES", logger.ERROR);
            throw e;
        }
        
        int threads = conf.getConfiguration("spoolmanagerthreads", "1").getValueAsInt();
        while (threads-- > 0) {
            try {
                JamesSpoolManager spoolMgr = new JamesSpoolManager();
                spoolMgr.setConfiguration(conf.getConfiguration("spoolmanager"));
                spoolMgr.setContext(context);
                spoolMgr.setComponentManager(comp);
                spoolMgr.init();
                threadManager.execute(spoolMgr);
            } catch (Exception e) {
                logger.log("Exception in SpoolManager thread-" + threads + " init: " + e.getMessage(), "JAMES", logger.ERROR);
                throw e;
            }
            logger.log("SpoolManager " + (threads + 1) + " started", "JAMES", logger.INFO);
        }

        userManager.init();
        pop3Server.init();
        smtpServer.init();
        remoteAdmin.init();

        logger.log("JAMES ...init end", "JAMES", logger.INFO);
    }

    public void sendMail(String sender, Vector recipients, MimeMessage message)
    throws MessagingException {
//FIX ME!!! we should validate here MimeMessage.
        sendMail(new Mail(getId(), sender, recipients, message));
    }

    public void sendMail(String sender, Vector recipients, InputStream msg)
    throws MessagingException {

            // parse headers
        MailHeaders headers = new MailHeaders(msg);
            // if headers do not contains minimum REQUIRED headers fields throw Exception
        if (!headers.isValid()) {
            throw new MessagingException("Some REQURED header field is missing. Invalid Message");
        }
        ByteArrayInputStream headersIn = new ByteArrayInputStream(headers.toByteArray());
        sendMail(new Mail(getId(), sender, recipients, new SequenceInputStream(headersIn, msg)));
    }

    public void sendMail(Mail mail)
    throws MessagingException {
        try {
            spool.store(mail);
        } catch (Exception e) {
            try {
                spool.remove(mail);
            } catch (Exception ignored) {
            }
            throw new MessagingException("Exception spooling message: " + e.getMessage());
        }
        logger.log("Mail " + mail.getName() + " pushed in spool", "JAMES", logger.INFO);
    }

    public MailRepository getUserInbox(String userName) {

        MailRepository userInbox = (MailRepository) null;
        try {
            userInbox = (MailRepository) comp.getComponent(userName);
        } catch (ComponentNotFoundException ex) {
            String dest = localInbox.getChildDestination(userName);
            userInbox = (MailRepository) store.getPrivateRepository(dest, MailRepository.MAIL, Store.ASYNCHRONOUS);
            comp.put(userName, userInbox);
        }
        return userInbox;
    }
    
    private String getId() {
        return "Mail" + System.currentTimeMillis();
    }

	public static void main(String[] args) {
	    
	    System.out.println("ERROR!");
	    System.out.println("Cannot exceute James as a stand alone application.");
	    System.out.println("To run James you need to have the Avalon framework installed.");
	    System.out.println("Please refere to the Readme file to know how to run James.");
    }

    public void destroy()
    throws Exception {
    }
}