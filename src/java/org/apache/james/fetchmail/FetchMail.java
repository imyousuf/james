/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.fetchmail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Properties;

import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

import org.apache.avalon.cornerstone.services.scheduler.Target;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.core.MailImpl;
import org.apache.james.services.MailServer;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

/**
 *
 * A class which fetches mail from a single account and inserts it
 * into the incoming spool
 *
 * $Id: FetchMail.java,v 1.7 2003/04/28 12:03:26 danny Exp $
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
     * The server host name for this fetch task
     */
    private String sHost;
    /**
     * The user name for this fetch task
     */
    private String sUser;

    /**
     * The user password for this fetch task
     */
    private String sPass;

    /**
     * Keep retrieved messages on the remote mailserver.  Normally, messages
     * are deleted from the folder on the mailserver after they have been retrieved
     */
    private boolean bKeep = false;

    /**
     * Retrieve both old (seen) and new messages from the mailserver.  The default
     * is to fetch only messages the server has not marked as seen.
     */
    private boolean bAll = false;

    /**
     * Recurse folders if available?
     */
    private boolean bRecurse = false;

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


    public boolean processMessage(Session session, MimeMessage message, MimeMessage received) {

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




            //
            // set the X-fetched-from header
            received.addHeader("X-fetched-from", fetchTaskName);

            Mail mail = new MailImpl(server.getId(), new
                    MailAddress((InternetAddress) received.getFrom()[0]), recipients, received);


            // Lets see if this mail has been bouncing by counting
            // the   X-fetched-from headers
            // if it is then move it to the ERROR repository
            Enumeration enum = message.getMatchingHeaderLines(new String[]{"X-fetched-from"});
            int count = 1;
            while (enum.hasMoreElements()) {
                Object o = enum.nextElement();
                count++;
            }
            if (count > 3) {
                mail.setState(Mail.ERROR);
                mail.setErrorMessage("This mail from FetchMail task " + fetchTaskName + " seems to be bounceing!");
                getLogger().error("A message from FetchMail task " + fetchTaskName + " seems to be bounceing! Moved to Error repository");
                return false;
            }

            server.sendMail(mail);
            getLogger().debug("Spooled message to " +
                    recipients.toString());

            //
            // logging if needed
            getLogger().debug("Sent message " + message.toString());
            return true;
        } catch (ParseException pe) {
            recipients.add(recipient);
        } catch (MessagingException innerE) {
            getLogger().error("can't insert message " + message.toString());
        }
        return false;
    }

    public boolean processFolder(Session session, Folder folder) {

        boolean ret = false;

        try {

            //
            // try to open read/write and if that fails try read-only
            try {
                folder.open(Folder.READ_WRITE);
            } catch (MessagingException ex) {
                try {
                    folder.open(Folder.READ_ONLY);
                } catch (MessagingException ex2) {
                    getLogger().debug(fetchTaskName + " Failed to open folder!");
                }
            }

//            int totalMessages = folder.getMessageCount();
//            if (totalMessages == 0) {
//                getLogger().debug(fetchTaskName + " Empty folder");
//                folder.close(false);
//                fetching = false;
//                return false;
//            }

            Message[] msgs = folder.getMessages();
            MimeMessage[] received = new MimeMessage[folder.getMessageCount()];

            int j = 0;
            for (int i = 0; i < msgs.length; i++, j++) {
                Flags flags = msgs[i].getFlags();
                MimeMessage message = (MimeMessage) msgs[i];

                //
                // saved recieved messages for further processing...
                received[j] = new MimeMessage(/*session,*/ message);

                received[j].addHeader("X-fetched-folder", folder.getFullName());

                if (bAll) {
                    ret = processMessage(session, message, received[j]);
                } else if (message.isSet(Flags.Flag.SEEN)) {
                    ret = processMessage(session, message, received[j]);
                }


                if (ret) {
                //
                // need to get the flags again just in case processMessage
                // has changed the flags....
                Flags f = received[j].getFlags();

                if (!bKeep) {

                    message.setFlag(Flags.Flag.DELETED, true);
                } else {
                    f.add(Flags.Flag.SEEN);

                    received[j].setFlags(f, true);
                }
                }
            }
            folder.close(true);

            //
            // see if this folder contains subfolders and recurse is true
            if (bRecurse) {
                if ((folder.getType() & Folder.HOLDS_FOLDERS) != 0) {
                    //
                    // folder contains subfolders...
                    Folder folders[] = folder.list();

                    for (int k = 0; k < folders.length; k++) {
                        processFolder(session, folders[k]);
                    }

                }
            }
            return true;
        } catch (MessagingException mex) {
            getLogger().debug(mex.toString());

        } /*catch (IOException ioex) {
            getLogger().debug(ioex.toString());
        }   */
        fetching = false;
        return false;
    }


    public void targetTriggered(String arg0) {
        Store store = null;
        Session session = null;
        Folder folder = null;

        // Get a Properties object
        Properties props = System.getProperties();


        //
        // if we are already fetching then just return
        if (fetching) return;
        fetching = true;


        if (getLogger().isDebugEnabled()) {
            getLogger().debug(fetchTaskName + " fetcher starting fetch");
        }


        // Get a Session object
        session = Session.getDefaultInstance(props, null);
        //  session.setDebug(debug);

        // Get a Store object
        try {
            store = session.getStore(javaMailProviderName);

            // Connect
            if (sHost != null || sUser != null || sPass != null)
                store.connect(sHost, sUser, sPass);
            else
                store.connect();

            // Open the Folder
            folder = store.getFolder(javaMailFolderName);
            if (folder == null) {
                getLogger().debug(fetchTaskName + " No default folder");
            }


            processFolder(session, folder);

            store.close();
        } catch (MessagingException ex) {
            getLogger().debug(fetchTaskName + ex.getMessage());
        }
        fetching = false;
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(final ServiceManager manager ) throws ServiceException {
        try {
            server = (MailServer) manager.lookup(MailServer.ROLE);
        } catch (ClassCastException cce) {
            StringBuffer errorBuffer =
                    new StringBuffer(128).append("Component ").append(MailServer.ROLE).append(
                            "does not implement the required interface.");
            throw new ServiceException(errorBuffer.toString());
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
        this.sHost = conf.getChild("host").getValue();
        this.sUser = conf.getChild("user").getValue();
        this.sPass = conf.getChild("password").getValue();
        this.fetchTaskName = conf.getAttribute("name");
        this.javaMailProviderName = conf.getChild("javaMailProviderName").getValue();
        this.javaMailFolderName = conf.getChild("javaMailFolderName").getValue();
        try {
            this.recipient = new MailAddress(conf.getChild("recipient").getValue());
        } catch (ParseException pe) {
            throw new ConfigurationException("Invalid recipient address specified");
        }
        this.ignoreOriginalRecipient = conf.getChild("recipient").getAttributeAsBoolean("ignorercpt-header");
        this.bAll = conf.getChild("fetchall").getValueAsBoolean();
        this.bKeep = conf.getChild("leaveonserver").getValueAsBoolean();
        this.bRecurse = conf.getChild("recursesubfolders").getValueAsBoolean();
        if (getLogger().isDebugEnabled()) {
            getLogger().info("Configured FetchMail fetch task " + fetchTaskName);
        }
    }
}
