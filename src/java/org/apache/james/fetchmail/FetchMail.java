/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.fetchmail;

import java.io.IOException;
import java.io.InputStream;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Vector;
import java.util.*;
import java.io.*;
import javax.mail.*;
import javax.mail.event.*;
import javax.mail.internet.*;
import javax.activation.*;

import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.framework.component.ComponentException;
import org.apache.avalon.framework.component.ComponentManager;
import org.apache.avalon.framework.component.Composable;
import org.apache.avalon.framework.component.DefaultComponentManager;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.commons.net.pop3.POP3Client;
import org.apache.commons.net.pop3.POP3MessageInfo;
import org.apache.james.services.MailServer;
import org.apache.mailet.*;
import org.apache.james.core.MailImpl;

/**
 *
 * A class which fetches mail from a single account and inserts it
 * into the incoming spool
 *
 * $Id: FetchMail.java,v 1.2 2003/02/06 06:25:58 noel Exp $
 *
 */
public class FetchMail extends AbstractLogEnabled implements Configurable, Target {
    /**
     * The MailServer service
     */
    private MailServer server;
    /**
     * The user to send the fetched mail to
     */
    private MailAddress recipient;

    /**
     * Don't parse header looking for recipient
     */
    private boolean ignoreOriginalRecipient;
    /**
     * The unique, identifying name for this task
     */
    private String fetchTaskName;
    /**
     * The POP3 server host name for this fetch task
     */
    private String popHost;
    /**
     * The POP3 user name for this fetch task
     */
    private String popUser;
    /**
     * The POP3 user password for this fetch task
     */
    private String popPass;

    /**
     * Flag to determine if you want to leave messages on server
     * If so unseen messages will be marked as seen
     */
    private boolean popLeaveOnServer = false;


    /**
     * The name of the javamail provider we want to user (pop3,imap,nntp,etc...)
     *
     */
    private String javaMailProviderName = "pop3";

    /**
     * The name of the folder to fetch from the javamail provider
     *
     */
    private String javaMailFolderName = "INBOX";


    /**
     * @see org.apache.avalon.cornerstone.services.scheduler.Target#targetTriggered(String)
     */
    private boolean fetching = false;


    public void targetTriggered(String arg0) {

        //
        // if we are already fetching then just return
        if (fetching) return;
        fetching = true;


        if (getLogger().isDebugEnabled()) {
            getLogger().debug(fetchTaskName + " fetcher starting fetch");
        }

        Store store = null;
        Session session = null;
        Folder folder = null;


        // Get a Properties object
        Properties props = System.getProperties();

        // Get a Session object
        session = Session.getDefaultInstance(props, null);
        //  session.setDebug(debug);

        // Get a Store object
        try {
            store = session.getStore(javaMailProviderName);

            // Connect
            if (popHost != null || popUser != null || popPass != null)
                store.connect(popHost, popUser, popPass);
            else
                store.connect();

            // Open the Folder
            folder = store.getFolder(javaMailFolderName);
            if (folder == null) {
                getLogger().debug(fetchTaskName + " No default folder");
            }


            //    // try to open read/write and if that fails try read-only
            try {
                folder.open(Folder.READ_WRITE);
            } catch (MessagingException ex) {
                try {
                    folder.open(Folder.READ_ONLY);
                } catch (MessagingException ex2) {
                    getLogger().debug(fetchTaskName + " Failed to open folder!");
                    store.close();
                }
            }

            int totalMessages = folder.getMessageCount();
            if (totalMessages == 0) {
                getLogger().debug(fetchTaskName + " Empty folder");
                folder.close(false);
                store.close();
                fetching = false;
                return;
            }

            Message[] msgs = folder.getMessages();
            Message[] received = new Message[folder.getUnreadMessageCount()];

            // Use a suitable FetchProfile
            FetchProfile fp = new FetchProfile();
            fp.add(FetchProfile.Item.ENVELOPE);
            fp.add(FetchProfile.Item.CONTENT_INFO);
            fp.add(FetchProfile.Item.FLAGS);
            fp.add("X-Mailer");

            folder.fetch(msgs, fp);

            int j = 0;
            for (int i = 0; i < msgs.length; i++) {
                Flags flags = msgs[i].getFlags();
                MimeMessage message = (MimeMessage) msgs[i];

                if (!msgs[i].isSet(Flags.Flag.SEEN)) {

                    //
                    // saved recieved messages for furthe processing...
                    received[j++] = msgs[i];
                    Collection recipients = new ArrayList(1);

                    try {
                        if (!ignoreOriginalRecipient) {
                            String er = getEnvelopeRecipient(message);
                            if (er != null) {
                                recipients.add(new MailAddress(er));
                                getLogger().info("Using original envelope recipient as new envelope recipient");
                            } else {
                                Address[] to = message.getAllRecipients();
                                if (to.length == 1) {
                                    recipients.add(new
                                            MailAddress((InternetAddress) to[0]));
                                    getLogger().info("Using To: header address as new envelope recipient");
                                } else {
                                    getLogger().info("Using configured recipient as new envelope recipient");
                                    recipients.add(recipient);
                                }
                            }
                        } else {
                            getLogger().info("Using configured recipient as new envelope recipient");
                            recipients.add(recipient);
                        }
                    } catch (ParseException pe) {
                        recipients.add(recipient);
                    }

                    MailImpl mail = new MailImpl(server.getId(), new
                            MailAddress((InternetAddress) message.getFrom()[0]), recipients, message);

                    // Lets see if this mail has been bouncing by counting
                    // the   X-fetched-from headers
                    // if it is then move it to the ERROR repository
                    Enumeration enum = message.getMatchingHeaderLines(new
                            String[]{"X-fetched-from"});
                    int count = 1;
                    while (enum.hasMoreElements()) {
                        Object o = enum.nextElement();
                        count++;
                    }
                    if (count > 3) {
                        mail.setState(mail.ERROR);
                        mail.setErrorMessage("This mail from FetchMail task " + fetchTaskName + " seems to be bounceing!");
                        getLogger().error("A message from FetchMail task " + fetchTaskName + " seems to be bounceing! Moved to Error repository");
                    }

                    // Send to spooler
                    try {
                        server.sendMail(mail);
                        getLogger().debug("Spooled message to " +
                                recipients.toString());

                        //
                        // logging if needed
                        getLogger().debug("Sent message " + msgs[i].toString());

                    } catch (MessagingException innerE) {
                        getLogger().error("can't insert message " + msgs[i].toString());
                    } /*catch (IOException ioE) {
                    getLogger().error("can't convert message to a mime message " + ioE.getMessage());
                    }*/
                }
            }
            if (popLeaveOnServer) {
                Flags f = new Flags();
                f.add(Flags.Flag.SEEN);
                folder.setFlags(received, f, true);
                folder.close(false);
            } else {
                Flags f = new Flags();
                f.add(Flags.Flag.DELETED);

                folder.setFlags(received, f, true);
                folder.close(true);
            }

            store.close();
        } catch (MessagingException ex) {
            getLogger().debug(fetchTaskName + ex.getMessage());
        }
        fetching = false;
    }

    /**
     * @see org.apache.avalon.framework.component.Composable#compose(ComponentManager)
     */
    public void compose(final ComponentManager componentManager) throws ComponentException {
        try {
            server = (MailServer) componentManager.lookup(MailServer.ROLE);
        } catch (ClassCastException cce) {
            StringBuffer errorBuffer =
                    new StringBuffer(128).append("Component ").append(MailServer.ROLE).append(
                            "does not implement the required interface.");
            throw new ComponentException(errorBuffer.toString());
        }
    }


    /**
     * Try and parse the "for" parameter from a Received header
     * Maybe not the most accurate parsing in the world but it should do
     * I opted not to use ORO (maybe I should have)
     */
    private String getEnvelopeRecipient(MimeMessage msg) {
        try {
            Enumeration enum = msg.getMatchingHeaderLines(new String[]{"Received"});
            while (enum.hasMoreElements()) {
                String received = (String) enum.nextElement();

                int nextSearchAt = 0;
                int i = 0;
                int start = 0;
                int end = 0;
                boolean hasBracket = false;
                boolean usableAddress = false;
                while (!usableAddress && (i != -1)) {
                    hasBracket = false;
                    i = received.indexOf("for ", nextSearchAt);
                    if (i > 0) {
                        start = i + 4;
                        end = 0;
                        nextSearchAt = start;
                        for (int c = start; c < received.length(); c++) {
                            char ch = received.charAt(c);
                            switch (ch) {
                                case '<':
                                    hasBracket = true;
                                    continue;
                                case '@':
                                    usableAddress = true;
                                    continue;
                                case ' ':
                                    end = c;
                                    break;
                                case ';':
                                    end = c;
                                    break;
                            }
                            if (end > 0)
                                break;
                        }
                    }
                }
                if (usableAddress) {
                    // lets try and grab the email address
                    String mailFor = received.substring(start, end);

                    // strip the <> around the address if there are any
                    if (mailFor.startsWith("<") && mailFor.endsWith(">"))
                        mailFor = mailFor.substring(1, (mailFor.length() - 1));

                    return mailFor;
                }
            }
        } catch (MessagingException me) {
            getLogger().info("No Recieved headers found");
        }
        return null;
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) throws ConfigurationException {
        this.popHost = conf.getChild("host").getValue();
        this.popUser = conf.getChild("user").getValue();
        this.popPass = conf.getChild("password").getValue();
        this.fetchTaskName = conf.getAttribute("name");
        this.javaMailProviderName = conf.getChild("javaMailProviderName").getValue();
        this.javaMailFolderName = conf.getChild("javaMailFolderName").getValue();
        try {
            this.recipient = new MailAddress(conf.getChild("recipient").getValue());
        } catch (ParseException pe) {
            throw new ConfigurationException("Invalid recipient address specified");
        }
        this.ignoreOriginalRecipient = conf.getChild("recipient").getAttributeAsBoolean("ignorercpt-header");
        this.popLeaveOnServer = conf.getChild("leaveonserver").getValueAsBoolean();
        if (getLogger().isDebugEnabled()) {
            getLogger().info("Configured FetchMail fetch task " + fetchTaskName);
        }
    }
}
