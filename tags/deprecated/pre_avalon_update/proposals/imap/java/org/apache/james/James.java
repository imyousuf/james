/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james;

import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.component.*;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.DefaultContext;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.phoenix.Block;
import org.apache.avalon.phoenix.BlockContext;
import org.apache.james.core.MailHeaders;
import org.apache.james.core.MailImpl;
import org.apache.james.imapserver.ACLMailbox;
import org.apache.james.imapserver.Host;
import org.apache.james.services.*;
import org.apache.james.userrepository.DefaultJamesUser;
import org.apache.james.util.RFC822DateFormat;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetContext;

import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

/**
 * Core class for JAMES. Provides three primary services:
 * <br> 1) Instantiates resources, such as user repository, and protocol
 * handlers
 * <br> 2) Handles interactions between components
 * <br> 3) Provides container services for Mailets
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author Serge
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 *
 * This is $Revision: 1.3 $
 * Committed on $Date: 2002/01/17 06:09:00 $ by: $Author: darrell $
 */
public class James
    extends AbstractLogEnabled
    implements Block, Contextualizable, Composable, Configurable,
               Initializable, MailServer, MailetContext {

    //and this is a mistake

    private final static String VERSION = Constants.SOFTWARE_NAME + " " + Constants.SOFTWARE_VERSION;
    private final static boolean DEEP_DEBUG = true;

    private DefaultComponentManager compMgr; //Components shared
    private DefaultContext context;
    private Configuration conf;

    private Logger mailetLogger = null;
    private MailStore mailstore;
    private UsersStore usersStore;
    private SpoolRepository spool;
    private MailRepository localInbox;
    private String inboxRootURL;
    private UsersRepository localusers;
    private Collection serverNames;
    private boolean ignoreCase;
    private boolean enableAliases;
    private boolean enableForwarding;

    // this used to be long, but increment operations on long are not
    // thread safe. Changed to int. 'int' should be ok, because id generation
    // is based on System time and count
    private static int count;
    private MailAddress postmaster;
    private Map mailboxes; //Not to be shared!
    private Hashtable attributes = new Hashtable();

    // IMAP related fields
    private boolean useIMAPstorage = false;
    private Host imapHost;
    protected BlockContext           blockContext;


    public void contextualize(final Context context) {
        this.blockContext = (BlockContext)context;
    }

    public void configure(Configuration conf) {
        this.conf = conf;
    }

    /**
     * Override compose method of AbstractBlock to create new
     * ComponentManager object
     */
    public void compose(ComponentManager comp) {
        compMgr = new DefaultComponentManager(comp);
        mailboxes = new HashMap(31);
    }

    public void initialize() throws Exception {

        getLogger().info("JAMES init...");

        // TODO: This should retrieve a more specific named thread pool from
        // BlockContext that is set up in server.xml
        // workerPool = blockContext.getThreadPool( "default" );
        try {
            mailstore = (MailStore) compMgr.lookup( MailStore.ROLE );
        } catch (Exception e) {
            getLogger().warn("Can't get Store: " + e);
        }
        getLogger().debug("Using MailStore: " + mailstore.toString());
        try {
            usersStore = (UsersStore) compMgr.lookup( UsersStore.ROLE );
        } catch (Exception e) {
            getLogger().warn("Can't get Store: " + e);
        }
        getLogger().debug("Using UsersStore: " + usersStore.toString());
        context = new DefaultContext();

        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch  (UnknownHostException ue) {
            hostName = "localhost";
        }
        context.put("HostName", hostName);
        getLogger().info("Local host is: " + hostName);

        // Get the domains and hosts served by this instance
        serverNames = new Vector();
        Configuration serverConf = conf.getChild("servernames");
        if (serverConf.getAttribute("autodetect").equals("TRUE") && (!hostName.equals("localhost"))) {
            serverNames.add(hostName);
        }

        final Configuration[] serverNameConfs =
            conf.getChild( "servernames" ).getChildren( "servername" );
        for ( int i = 0; i < serverNameConfs.length; i++ ) {
            serverNames.add( serverNameConfs[i].getValue());
        }
        if (serverNames.isEmpty()) {
            throw new ConfigurationException( "Fatal configuration error: no servernames specified!");
        }

        for (Iterator i = serverNames.iterator(); i.hasNext(); ) {
            getLogger().info("Handling mail for: " + i.next());
        }
        context.put(Constants.SERVER_NAMES, this.serverNames);


        // Get postmaster
        String postMasterAddress = conf.getChild("postmaster").getValue("root@localhost");
        this.postmaster = new MailAddress( postMasterAddress );
        context.put( Constants.POSTMASTER, postmaster );

        Configuration userNamesConf = conf.getChild("usernames");
        if (userNamesConf.getAttribute("ignoreCase").equals("TRUE")) {
            ignoreCase = true;
        } else {
            ignoreCase = false;
        }
        if (userNamesConf.getAttribute("enableAliases").equals("TRUE")) {
            enableAliases = true;
        } else {
            enableAliases = false;
        }
        if (userNamesConf.getAttribute("enableForwarding").equals("TRUE")) {
            enableForwarding = true;
        } else {
            enableForwarding = false;
        }

        //Get localusers
        try {
            localusers = (UsersRepository) usersStore.getRepository("LocalUsers");
        } catch (Exception e) {
            getLogger().error("Cannot open private UserRepository");
            throw e;
        }
        //}
        compMgr.put( UsersRepository.ROLE, (Component)localusers);
        getLogger().info("Local users repository opened");

        // Get storage system
        if (conf.getChild("storage").getValue().equals("IMAP")) {
            useIMAPstorage = true;
        }

        //IMAPServer instance is controlled via assembly.xml.
        //Assumption is that assembly.xml will set the correct IMAP Store
        //if IMAP is enabled.
        //if (provideIMAP && (! useIMAPstorage)) {
        //    throw new ConfigurationException ("Fatal configuration error: IMAP service requires IMAP storage ");
        //}

        // Get the LocalInbox repository
        if (useIMAPstorage) {
            try {
                // We will need to use a no-args constructor for flexibility
                imapHost = (Host) compMgr.lookup( Host.ROLE );
            } catch (Exception e) {
                getLogger().error("Exception in IMAP Storage init: " + e.getMessage());
                throw e;
            }
        } else {
            Configuration inboxConf = conf.getChild("inboxRepository");
            Configuration inboxRepConf = inboxConf.getChild("repository");
            try {
                localInbox = (MailRepository) mailstore.select(inboxRepConf);
            } catch (Exception e) {
                getLogger().error("Cannot open private MailRepository");
                throw e;
            }
            inboxRootURL = inboxRepConf.getAttribute("destinationURL");
        }
        getLogger().info("Private Repository LocalInbox opened");

        // Add this to comp
        compMgr.put( MailServer.ROLE, this);

        spool = mailstore.getInboundSpool();
        if (DEEP_DEBUG) getLogger().debug("Got spool");

        // For mailet engine provide MailetContext
        //compMgr.put("org.apache.mailet.MailetContext", this);
        // For AVALON aware mailets and matchers, we put the Component object as
        // an attribute
        attributes.put(Constants.AVALON_COMPONENT_MANAGER, compMgr);

        System.out.println(VERSION);
        getLogger().info("JAMES ...init end");
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
        sendMail(sender, recipients, message, Mail.DEFAULT);
    }

    public void sendMail(MailAddress sender, Collection recipients, MimeMessage message, String state)
            throws MessagingException {
        MailImpl mail = new MailImpl(getId(), sender, recipients, message);
        mail.setState(state);
        sendMail(mail);
    }

    public void sendMail(MailAddress sender, Collection recipients, InputStream msg)
            throws MessagingException {
        // parse headers
        MailHeaders headers = new MailHeaders(msg);

        // if headers do not contains minimum REQUIRED headers fields throw Exception
        if (!headers.isValid()) {
            throw new MessagingException("Some REQURED header field is missing. Invalid Message");
        }
        ByteArrayInputStream headersIn = new ByteArrayInputStream(headers.toByteArray());
        sendMail(new MailImpl(getId(), sender, recipients, new SequenceInputStream(headersIn, msg)));
    }

    public void sendMail(Mail mail) throws MessagingException {
        MailImpl mailimpl = (MailImpl)mail;
        try {
            spool.store(mailimpl);
        } catch (Exception e) {
            try {
                spool.remove(mailimpl);
            } catch (Exception ignored) {
            }
            throw new MessagingException("Exception spooling message: " + e.getMessage(), e);
        }
        getLogger().info("Mail " + mailimpl.getName() + " pushed in spool");
    }

    /**
     * For POP3 server only - at the momment.
     */
    public synchronized MailRepository getUserInbox(String userName) {
        MailRepository userInbox = (MailRepository) null;

        userInbox = (MailRepository) mailboxes.get(userName);

        if (userInbox != null) {
            return userInbox;
        } else if (mailboxes.containsKey(userName)) {
            // we have a problem
            getLogger().error("Null mailbox for non-null key");
            throw new RuntimeException("Error in getUserInbox.");
        } else {
            // need mailbox object
            getLogger().info("Need inbox for " + userName );
            String destination = inboxRootURL + userName + "/";
            DefaultConfiguration mboxConf
                = new DefaultConfiguration("repository", "generated:AvalonFileRepository.compose()");
            mboxConf.setAttribute("destinationURL", destination);
            mboxConf.setAttribute("type", "MAIL");
            try {
                userInbox = (MailRepository) mailstore.select(mboxConf);
                mailboxes.put(userName, userInbox);
            } catch (Exception e) {
                e.printStackTrace();
                getLogger().error("Cannot open user Mailbox" + e);
                throw new RuntimeException("Error in getUserInbox." + e);
            }
            return userInbox;
        }
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

    //Methods for MailetContext

    public Collection getMailServers(String host) {
        DNSServer dnsServer = null;
        try {
            dnsServer = (DNSServer) compMgr.lookup( DNSServer.ROLE );
        } catch ( final ComponentException cme ) {
            getLogger().error("Fatal configuration error - DNS Servers lost!", cme );
            throw new RuntimeException("Fatal configuration error - DNS Servers lost!");
        }
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
            reply.setHeader("Date", RFC822DateFormat.toString(new Date()));
            reply.setContent(multipart);
            reply.setHeader("Content-Type", multipart.getContentType());
        } catch (IOException ioe) {
            throw new MessagingException("Unable to create multipart body", ioe);
        }
        //Send it off...
        sendMail(bouncer, recipients, reply);
    }

    public boolean isLocalUser(String name) {
        if (ignoreCase) {
            return localusers.containsCaseInsensitive(name);
        } else {
            return localusers.contains(name);
        }
    }

    public MailAddress getPostmaster() {
        return postmaster;
    }

    public void storeMail(MailAddress sender, MailAddress recipient, MimeMessage message) {
        String username;
        if (ignoreCase) {
            username = localusers.getRealName(recipient.getUser());
        } else {
            username = recipient.getUser();
        }
        JamesUser user;
        if (enableAliases || enableForwarding) {
            user = (JamesUser) localusers.getUserByName(username);
            if (enableAliases && user.getAliasing()) {
                username = user.getAlias();
            }
            if (enableForwarding && user.getForwarding()) {
                MailAddress forwardTo = user.getForwardingDestination();
                Collection recipients = new HashSet();
                recipients.add(forwardTo);
                try {
                    sendMail(sender, recipients, message);
                    getLogger().info("Mail for " + username + " forwarded to "
                                         +  forwardTo.toString());
                    return;
                } catch (MessagingException me) {
                    getLogger().error("Error forwarding mail to "
                              + forwardTo.toString()
                              + "attempting local delivery");
                }
            }
        }

        if (useIMAPstorage) {
            ACLMailbox mbox = null;
            try {
                String folderName = "#user." + username + ".INBOX";
                getLogger().debug("Want to store to: " + folderName);
                mbox = imapHost.getMailbox(MailServer.MDA, folderName);
                if(mbox.store(message,MailServer.MDA)) {
                    getLogger().info("Message " + message.getMessageID() +" stored in " + folderName);
                } else {
                    throw new RuntimeException("Failed to store mail: ");
                }
                imapHost.releaseMailbox(MailServer.MDA, mbox);
                mbox = null;
            } catch (Exception e) {
                getLogger().error("Exception storing mail: " + e);
                e.printStackTrace();
                if (mbox != null) {
                    imapHost.releaseMailbox(MailServer.MDA, mbox);
                    mbox = null;
                }
                throw new RuntimeException("Exception storing mail: " + e);
            }
        } else {
            Collection recipients = new HashSet();
            recipients.add(recipient);
            MailImpl mailImpl = new MailImpl(getId(), sender, recipients, message);
            getUserInbox(username).store(mailImpl);
        }
    }

    public int getMajorVersion() {
        return 1;
    }

    public int getMinorVersion() {
        return 3;
    }

    public boolean isLocalServer( final String serverName ) {
        return serverNames.contains( serverName );
    }

    public String getServerInfo() {
        return "JAMES/1.3-dev";
    }

    private Logger getMailetLogger() {
        if (mailetLogger == null) {
            mailetLogger = getLogger().getChildLogger("Mailet");
        }
        return mailetLogger;
    }

    public void log(String message) {
        getMailetLogger().info(message);
    }

    public void log(String message, Throwable t) {
        //System.err.println(message);
        //t.printStackTrace(); //DEBUG
        getMailetLogger().info(message,t);
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
        boolean success;
        DefaultJamesUser user = new DefaultJamesUser(userName, "SHA");
        user.setPassword(password);
        user.initialize();
        success = localusers.addUser(user);
        if (useIMAPstorage && success) {
            if ( imapHost.createPrivateMailAccount(userName) ) {
                getLogger().info("New MailAccount created for" + userName);
            }
        }
        return success;
    }
}
