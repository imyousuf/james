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
import org.apache.mail.Mail;
import java.net.*;
import java.io.*;
import javax.mail.internet.*;
import javax.mail.Session;
import javax.mail.MessagingException;
import java.util.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class James implements MailServer, Block {

    private SimpleComponentManager comp;
    private Configuration conf;
    private SimpleComponentManager spoolManagerCM;
    private SimpleContext context;
    private Logger logger;
    private ThreadManager threadManager;
    private Store store;
    private MailRepository spool;
    private MailRepository localInbox;
    private Store.Repository mailUsers;
    private String mailboxName;
    
    public James() {
    }

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }
    
    public void setComponentManager(ComponentManager comp) {
        this.comp = new SimpleComponentManager(comp);
    }

	public void init() throws Exception {

        spoolManagerCM = new SimpleComponentManager(comp);
        spoolManagerCM.put(Interfaces.MAIL_SERVER, this);

        this.logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        logger.log("JAMES init...", "JAMES", logger.INFO);
        context = new SimpleContext();
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
        this.threadManager = (ThreadManager) comp.getComponent(Interfaces.THREAD_MANAGER);
        this.store = (Store) comp.getComponent(Interfaces.STORE);

        try {
            this.mailboxName = conf.getConfiguration("mailboxName", "localInbox").getValue() + ".";
            this.localInbox = (MailRepository) store.getPublicRepository(mailboxName);
        } catch (RuntimeException e) {
            logger.log("Cannot open public Repository LocalInbox", "JAMES", logger.ERROR);
            throw e;
        }
        logger.log("Public Repository LocalInbox opened", "JAMES", logger.INFO);
        context.put(Constants.INBOX_ROOT, mailboxName);

        String postmaster = conf.getConfiguration("postmaster", "root@localhost").getValue();
        context.put(Constants.POSTMASTER, postmaster);
        
        try {
            this.mailUsers = (Store.Repository) store.getPublicRepository("MailUsers");
        } catch (RuntimeException e) {
            logger.log("Cannot open public Repository MailUsers", "JAMES", logger.ERROR);
            throw e;
        }
        logger.log("Public Repository MailUsers opened", "JAMES", logger.INFO);
        spoolManagerCM.put(Constants.USERS_REPOSITORY, mailUsers);

        String spoolRepository = conf.getConfiguration("spoolRepository", ".").getValue();
        try {
            this.spool = (MailRepository) store.getPrivateRepository(spoolRepository, MailRepository.MAIL, Store.ASYNCHRONOUS);
        } catch (RuntimeException e) {
            logger.log("Cannot open private MailRepository", "JAMES", logger.ERROR);
            throw e;
        }
        logger.log("Private MailRepository Spool opened", "JAMES", logger.INFO);
        spoolManagerCM.put(Constants.SPOOL_REPOSITORY, spool);

        int threads = conf.getConfiguration("spoolmanagerthreads", "1").getValueAsInt();
        while (threads-- > 0) {
            try {
                JamesSpoolManager spoolMgr = new JamesSpoolManager();
                spoolMgr.setConfiguration(conf.getConfiguration("spoolmanager"));
                spoolMgr.setContext(context);
                spoolMgr.setComponentManager(spoolManagerCM);
                spoolMgr.init();
                threadManager.execute(spoolMgr);
            } catch (Exception e) {
                logger.log("Exception in SpoolManager thread-" + threads + " init: " + e.getMessage(), "JAMES", logger.ERROR);
                throw e;
            }
            logger.log("SpoolManager " + (threads + 1) + " started", "JAMES", logger.INFO);
        }
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

    public MailRepository getInbox() {
        return localInbox;
    }

    public MailRepository getUserInbox(String userName) {

        MailRepository userInbox = (MailRepository) null;
        String repositoryName = mailboxName + userName;
        try {
            userInbox = (MailRepository) comp.getComponent(repositoryName);
        } catch (ComponentNotFoundException ex) {
            userInbox = (MailRepository) store.getPublicRepository(repositoryName);
            comp.put(repositoryName, userInbox);
        }
        return userInbox;
    }
    
    private String getId() {
        return "Mail" + System.currentTimeMillis();
    }

    public void destroy()
    throws Exception {
    }
}