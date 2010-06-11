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

import org.apache.avalon.*;
import org.apache.avalon.blocks.*;
import org.apache.mailet.*;

import org.apache.james.core.*;
import org.apache.james.transport.*;
import org.apache.james.smtpserver.*;
import org.apache.james.dnsserver.*;
import org.apache.james.pop3server.*;
import org.apache.james.remotemanager.*;
import org.apache.james.userrepository.*;
import org.apache.james.mailrepository.*;

import javax.mail.internet.*;
import javax.mail.Address;
import javax.mail.Session;
import javax.mail.MessagingException;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class James implements MailServer, Block, MailetContext {

    private SimpleComponentManager comp;
    private SimpleContext context;
    private Configuration conf;
    private Logger logger;
    private ThreadManager threadManager;
    private Store store;
    private SpoolRepository spool;
    private MailRepository localInbox;
    private UsersRepository users;
    private Collection serverNames;
    private static long count;
    private String helloName;
    private String hostName;

    private Hashtable attributes = new Hashtable();

    public void setConfiguration(Configuration conf) {
        this.conf = conf;
    }

    public void setComponentManager(ComponentManager comp) {
        this.comp = new SimpleComponentManager(comp);
    }

    public void init() throws Exception {

        logger = (Logger) comp.getComponent(Interfaces.LOGGER);
        logger.log("JAMES init...", "JamesSystem", logger.INFO);
        threadManager = (ThreadManager) comp.getComponent(Interfaces.THREAD_MANAGER);
        store = (Store) comp.getComponent(Interfaces.STORE);

        context = new SimpleContext();

    try {
                hostName = InetAddress.getLocalHost().getHostName();
    } catch  (UnknownHostException ue) {
        hostName = "localhost";
    }
    logger.log("Local host is: " + hostName, "JamesSystem", logger.INFO);


    helloName = null;
    Configuration helloConf = conf.getConfiguration("helloName");
    if (helloConf.getAttribute("autodetect").equals("TRUE")) {
        helloName = hostName;
    } else {
        helloName = helloConf.getValue();
        if (helloName == null || helloName.trim().equals("") )
        helloName = "localhost";
    }
    logger.log("Hello Name is: " + helloName, "JamesSystem", logger.INFO);
        context.put(Constants.HELO_NAME, helloName);

    // Get this domains and hosts served by this instance
        serverNames = new Vector();
    Configuration serverConf = conf.getConfiguration("servernames");
    if (serverConf.getAttribute("autodetect").equals("TRUE") && (!hostName.equals("localhost"))) {
        serverNames.add(hostName);
    }
        for (Enumeration e = conf.getConfigurations("servernames.servername"); e.hasMoreElements(); ) {
            serverNames.add(((Configuration) e.nextElement()).getValue());
        }
        if (serverNames.isEmpty()) {
        throw new ConfigurationException ("Fatal configuration error: no servernames specified!", conf);
        }

        for (Iterator i = serverNames.iterator(); i.hasNext(); ) {
            logger.log("Handling mail for: " + i.next(), "JamesSystem", logger.INFO);
        }
        context.put(Constants.SERVER_NAMES, serverNames);


    // Get postmaster
        String postmaster = conf.getConfiguration("postmaster").getValue("root@localhost");
        context.put(Constants.POSTMASTER, new MailAddress(postmaster));

    // Get the LocalInbox repository
        String inboxRepository = conf.getConfiguration("inboxRepository").getValue("file://../mail/inbox/");
        try {
            this.localInbox = (MailRepository) store.getPrivateRepository(inboxRepository, MailRepository.MAIL, Store.ASYNCHRONOUS);
        } catch (Exception e) {
            logger.log("Cannot open private MailRepository", "JamesSystem", logger.ERROR);
            throw e;
        }
        logger.log("Private Repository LocalInbox opened", "JamesSystem", logger.INFO);
            // Add this to comp
        comp.put(Interfaces.MAIL_SERVER, this);

        String spoolRepository = conf.getConfiguration("spoolRepository").getValue("file://../mail/spool/");
        try {
            this.spool = (SpoolRepository) store.getPrivateRepository(spoolRepository, SpoolRepository.SPOOL, Store.ASYNCHRONOUS);
        } catch (Exception e) {
            logger.log("Cannot open private SpoolRepository", "JamesSystem", logger.ERROR);
            throw e;
        }
        logger.log("Private SpoolRepository Spool opened", "JamesSystem", logger.INFO);
        comp.put(Constants.SPOOL_REPOSITORY, spool);

        /*
        UserManager userManager = new UserManager();
        try {
            userManager.setConfiguration(conf.getConfiguration("usersManager"));
            userManager.setContext(context);
            userManager.setComponentManager(comp);
            userManager.init();
        } catch (Exception e) {
            logger.log("Exception in UserManager init: " + e.getMessage(), "JamesSystem", logger.ERROR);
            throw e;
        }
        comp.put(Constants.USERS_MANAGER, userManager);
        */
        String usersRepository = conf.getConfiguration("userRepository").getValue("file://../var/users/");
        if(usersRepository.startsWith("ldap")) {
            try {
            UsersLDAPRepository rootRepository = new UsersLDAPRepository();
            rootRepository.setConfiguration(conf.getConfiguration("usersLDAP"));
            rootRepository.setContext(context);
            rootRepository.setComponentManager(comp);
            rootRepository.setServerRoot();
            rootRepository.init();
            //get or create LocalUsers directory entry
            String usersName
                =  rootRepository.getChildDestination("LocalUsers");
            UsersLDAPRepository usersRep = new UsersLDAPRepository();
            usersRep.setConfiguration(conf.getConfiguration("usersLDAP"));
            usersRep.setContext(context);
            usersRep.setComponentManager(comp);
            usersRep.setBase(usersName);
            usersRep.init();
            rootRepository.dispose();
            this.users = (UsersRepository) usersRep;
            } catch (Exception e) {
                logger.log("Exception in UsersLDAPRepository init: " + e.getMessage(), "JamesSystem", logger.ERROR);
                throw e;
            }

        } else {
            try {
                this.users = (UsersRepository) store.getPrivateRepository(usersRepository, UsersRepository.USER, Store.ASYNCHRONOUS);
            } catch (Exception e) {
                logger.log("Cannot open private UserRepository", "JamesSystem", logger.ERROR);
                throw e;
            }
        }
        comp.put(Constants.LOCAL_USERS, users);


        logger.log("Users Manager Opened", "JamesSystem", logger.INFO);
        //users = (UsersRepository) userManager.getUserRepository("LocalUsers");

        POP3Server pop3Server = new POP3Server();
        try {
            pop3Server.setConfiguration(conf.getConfiguration("pop3Server"));
            pop3Server.setContext(context);
            pop3Server.setComponentManager(comp);
        } catch (Exception e) {
            logger.log("Exception in POP3Server init: " + e.getMessage(), "JamesSystem", logger.ERROR);
            throw e;
        }

        SMTPServer smtpServer = new SMTPServer();
        try {
            smtpServer.setConfiguration(conf.getConfiguration("smtpServer"));
            smtpServer.setContext(context);
            smtpServer.setComponentManager(comp);
        } catch (Exception e) {
            logger.log("Exception in SMTPServer init: " + e.getMessage(), "JamesSystem", logger.ERROR);
            throw e;
        }

        DNSServer dnsServer = new DNSServer();
        try {
            dnsServer.setConfiguration(conf.getConfiguration("dnsServer"));
            //dnsServer.setContext(context);
            dnsServer.setComponentManager(comp);
        } catch (Exception e) {
            logger.log("Exception in DNSServer init: " + e.getMessage(), "JamesSystem", logger.ERROR);
            throw e;
        }
        comp.put("DNS_SERVER", dnsServer);

        RemoteManager remoteAdmin = new RemoteManager();
        try {
            remoteAdmin.setConfiguration(conf.getConfiguration("remoteManager"));
            remoteAdmin.setComponentManager(comp);
        } catch (Exception e) {
            logger.log("Exception in RemoteAdmin init: " + e.getMessage(), "JamesSystem", logger.ERROR);
            throw e;
        }

        // For AVALON aware mailets and matchers, we put the Component object as
        // an attribute
        attributes.put(Constants.AVALON_COMPONENT_MANAGER, comp);

        int threads = conf.getConfiguration("spoolmanagerthreads").getValueAsInt(1);
        while (threads-- > 0) {
            try {
                JamesSpoolManager spoolMgr = new JamesSpoolManager();
                spoolMgr.setConfiguration(conf.getConfiguration("spoolmanager"));
                spoolMgr.setContext(context);
                spoolMgr.setComponentManager(comp);
                spoolMgr.init();
                threadManager.execute(spoolMgr);
            } catch (Exception e) {
                logger.log("Exception in SpoolManager thread-" + threads + " init: " + e.getMessage(), "JamesSystem", logger.ERROR);
                throw e;
            }
            logger.log("SpoolManager " + (threads + 1) + " started", "JamesSystem", logger.INFO);
        }

        pop3Server.init();
        smtpServer.init();
        dnsServer.init();
        remoteAdmin.init();

        logger.log("JAMES ...init end", "JamesSystem", logger.INFO);
    }

    public void sendMail(MimeMessage message) throws MessagingException {
        MailAddress sender = new MailAddress((InternetAddress)message.getFrom()[0]);
        Collection recipients = new HashSet();
        Address addresses[] = message.getAllRecipients();
        for (int i = 0; i < addresses.length; i++) {
            recipients.add(new MailAddress((InternetAddress)addresses[i]));
        }
        sendMail(sender, recipients, message);
    }

    public void sendMail(MailAddress sender, Collection recipients, MimeMessage message)
    throws MessagingException {
//FIX ME!!! we should validate here MimeMessage.  - why? (SK)
        sendMail(sender, recipients, message, Mail.DEFAULT);
    }

    public void sendMail(MailAddress sender, Collection recipients, MimeMessage message, String state)
    throws MessagingException {
//FIX ME!!! we should validate here MimeMessage.
        MailImpl mail = new MailImpl(getId(), sender, recipients, message);
        mail.setState(state);
        sendMail(mail);
    }

    public synchronized void sendMail(MailAddress sender, Collection recipients, InputStream msg)
    throws MessagingException {

            // parse headers
        MailHeaders headers = new MailHeaders(msg);
            // if headers do not contains minimum REQUIRED headers fields throw Exception
        if (!headers.isValid()) {
            throw new MessagingException("Some REQURED header field is missing. Invalid Message");
        }
//        headers.setReceivedStamp("Unknown", (String) serverNames.elementAt(0));
        ByteArrayInputStream headersIn = new ByteArrayInputStream(headers.toByteArray());
        sendMail(new MailImpl(getId(), sender, recipients, new SequenceInputStream(headersIn, msg)));
    }

    public synchronized void sendMail(Mail mail) throws MessagingException {
        MailImpl mailimpl = (MailImpl)mail;
        try {
            spool.store(mailimpl);
        } catch (Exception e) {
            try {
                spool.remove(mailimpl);
            } catch (Exception ignored) {
            }
            throw new MessagingException("Exception spooling message: " + e.getMessage());
        }
        logger.log("Mail " + mailimpl.getName() + " pushed in spool", "JamesSystem", logger.INFO);
    }

    public synchronized MailRepository getUserInbox(String userName) {

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

    public String getId() {
        return "Mail" + System.currentTimeMillis() + "-" + count++;
    }

    public static void main(String[] args) {

        System.out.println("ERROR!");
        System.out.println("Cannot execute James as a stand alone application.");
        System.out.println("To run James, you need to have the Avalon framework installed.");
        System.out.println("Please refer to the Readme file to know how to run James.");
    }

    public void destroy() {
        //Does nothing... is this even called?
    }


    //Methods for MailetContext
    public Collection getMailServers(String host) {
        DNSServer dnsServer = (DNSServer) comp.getComponent("DNS_SERVER");
        return dnsServer.findMXRecords(host);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public void setAttribute(String key, Object object) {
        attributes.put(key, object);
    }

    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    public Iterator getAttributeNames() {
        Vector names = new Vector();
        for (Enumeration e = attributes.keys(); e.hasMoreElements(); ) {
            names.add(e.nextElement());
        }
        return names.iterator();
    }

    public void bounce(Mail mail, String message) throws MessagingException {
        bounce(mail, message, getPostmaster());
    }

    public void bounce(Mail mail, String message, MailAddress bouncer) throws MessagingException {
        MimeMessage orig = mail.getMessage();
        //Create the reply message
        MimeMessage reply = (MimeMessage) orig.reply(false);
        //Create the list of recipients in our MailAddress format
        Collection recipients = new HashSet();
        Address addresses[] = reply.getAllRecipients();
        for (int i = 0; i < addresses.length; i++) {
            recipients.add(new MailAddress((InternetAddress)addresses[i]));
        }
        //Change the sender...
        reply.setFrom(bouncer.toInternetAddress());
        try {
            //Create the message body
            MimeMultipart multipart = new MimeMultipart();
            //Add message as the first mime body part
            MimeBodyPart part = new MimeBodyPart();
            part.setContent(message, "text/plain");
            part.setHeader("Content-Type", "text/plain");
            multipart.addBodyPart(part);

            //Add the original message as the second mime body part
            part = new MimeBodyPart();
            part.setContent(orig.getContent(), orig.getContentType());
            part.setHeader("Content-Type", orig.getContentType());
            multipart.addBodyPart(part);

            reply.setContent(multipart);
            reply.setHeader("Content-Type", multipart.getContentType());
        } catch (IOException ioe) {
            throw new MessagingException("Unable to create multipart body");
        }
        //Send it off...
        sendMail(bouncer, recipients, reply);
    }

    public Collection getLocalUsers() {
        Vector userList = new Vector();
        for (Enumeration e = users.list(); e.hasMoreElements(); ) {
            userList.add(e.nextElement());
        }
        return userList;
    }

    public MailAddress getPostmaster() {
        return (MailAddress)context.get(Constants.POSTMASTER);
    }

    public void storeMail(MailAddress sender, MailAddress recipient, MimeMessage message) {
        Collection recipients = new HashSet();
        recipients.add(recipient);
        MailImpl mailImpl = new MailImpl(getId(), sender, recipients, message);
        getUserInbox(recipient.getUser()).store(mailImpl);
    }

    public int getMajorVersion() {
        return 1;
    }

    public int getMinorVersion() {
        return 2;
    }

    public Collection getServerNames() {
        Vector names = (Vector)context.get(Constants.SERVER_NAMES);
        return (Collection)names.clone();
    }

    public String getServerInfo() {
        return "JAMES/1.2";
    }

    public void log(String message) {
        logger.log(message, "Mailets", logger.INFO);
    }

    public void log(String message, Throwable t) {
        System.err.println(message);
        t.printStackTrace(); //DEBUG
        logger.log(message + ": " + t.getMessage(), "Mailets", logger.INFO);
    }

    /**
     * Adds a user to this mail server. Currently just adds user to a
     * UsersRepository.
     * <p> As we move to IMAP support this will also create mailboxes and
     * access control lists.
     *
     * @param userName String representing user name, that is the portion of
     * an email address before the '@<domain>'.
     * @param password String plaintext password
     * @returns boolean true if user added succesfully, else false.
     */

    public boolean addUser(String userName, String password) {
        users.addUser(userName, password);
        return true;
    }

}
