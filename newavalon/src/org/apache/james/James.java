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

import javax.mail.internet.*;
import javax.mail.Address;
import javax.mail.Session;
import javax.mail.MessagingException;

import org.apache.avalon.*;
import org.apache.avalon.blocks.Block;
//import org.apache.avalon.services.Store;
import org.apache.avalon.util.lang.*;

import org.apache.james.core.*;
import org.apache.james.transport.*;
import org.apache.james.smtpserver.*;
import org.apache.james.dnsserver.*;
import org.apache.james.pop3server.*;
import org.apache.james.remotemanager.*;
import org.apache.james.services.*;
//import org.apache.james.mailrepository.*;

import org.apache.log.LogKit;
import org.apache.log.Logger;

import org.apache.mailet.*;

/**
 * @version 1.0.0, 24/04/1999
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class James implements  Block, MailServer, Configurable, Composer, Initializable, MailetContext {

    private DefaultComponentManager compMgr; //Components shared
    private DefaultContext context;
    private Configuration conf;
    private Logger logger =  LogKit.getLoggerFor("JamesCore");
    private Logger mailetLogger = LogKit.getLoggerFor("Mailets");
    private WorkerPool workerPool;
    private MailStore mailstore;
    private UsersStore usersStore;
    private SpoolRepository spool;
    private MailRepository localInbox;
    private String inboxRootURL;
    private UsersRepository localusers;
    private Collection serverNames;
    private static long count;
    private String helloName;
    private String hostName;
    private Map mailboxes; //Not to be shared!
    private Hashtable attributes = new Hashtable();

    public void configure(Configuration conf) {
        this.conf = conf;
    }

    public void compose(ComponentManager comp) {
	//throws ConfigurationException {
        compMgr = new DefaultComponentManager(comp);
	mailboxes = new HashMap(31);
    }

    public void init() throws Exception {

        logger.info("JAMES init...");
        //threadManager = (ThreadManager) comp.getComponent(Interfaces.THREAD_MANAGER);
	workerPool = ThreadManager.getWorkerPool("whateverNameYouFancy");
	try {
	    mailstore = (MailStore) compMgr.lookup("org.apache.james.services.MailStore");
	} catch (Exception e) {
	    logger.warn("Can't get Store: " + e);
	}
	logger.debug("Using MailStore: " + mailstore.toString());
	try {
	    usersStore = (UsersStore) compMgr.lookup("org.apache.james.services.UsersStore");
	} catch (Exception e) {
	    logger.warn("Can't get Store: " + e);
	}
	logger.debug("Using UsersStore: " + usersStore.toString());
        context = new DefaultContext();
	
	try {
	    hostName = InetAddress.getLocalHost().getHostName();
	} catch  (UnknownHostException ue) {
	    hostName = "localhost";
	}
	logger.info("Local host is: " + hostName);
	

	helloName = null;
	Configuration helloConf = conf.getChild("helloName");
	if (helloConf.getAttribute("autodetect").equals("TRUE")) {
	    helloName = hostName;
	} else {
	    helloName = helloConf.getValue();
	    if (helloName == null || helloName.trim().equals("") )
		helloName = "localhost";
	}
	logger.info("Hello Name is: " + helloName);
        context.put(Constants.HELO_NAME, helloName);

	// Get the domains and hosts served by this instance
        serverNames = new Vector();
	Configuration serverConf = conf.getChild("servernames");
	if (serverConf.getAttribute("autodetect").equals("TRUE") && (!hostName.equals("localhost"))) {
	    serverNames.add(hostName);
	}
        for (Iterator it = conf.getChildren("servernames.servername"); it.hasNext(); ) {
            serverNames.add(((Configuration) it.next()).getValue());
        }
        if (serverNames.isEmpty()) {
	    throw new ConfigurationException ("Fatal configuration error: no servernames specified!");
        }
	
        for (Iterator i = serverNames.iterator(); i.hasNext(); ) {
            logger.info("Handling mail for: " + i.next());
        }
        context.put(Constants.SERVER_NAMES, serverNames);


	// Get postmaster
        String postmaster = conf.getChild("postmaster").getValue("root@localhost");
        context.put(Constants.POSTMASTER, new MailAddress(postmaster));
	
	// Get the LocalInbox repository
        Configuration inboxConf = conf.getChild("inboxRepository");
	Configuration inboxRepConf = inboxConf.getChild("repository");
        try {
            localInbox = (MailRepository) mailstore.select(inboxRepConf);
        } catch (Exception e) {
            logger.error("Cannot open private MailRepository");
            throw e;
        }
	inboxRootURL = inboxRepConf.getAttribute("destinationURL");
        logger.info("Private Repository LocalInbox opened");
            // Add this to comp
        compMgr.put("org.apache.james.services.MailServer", this);

        Configuration spoolConf = conf.getChild("spoolRepository");
	Configuration spoolRepConf = spoolConf.getChild("repository");
        try {
            this.spool = (SpoolRepository) mailstore.select(spoolRepConf);
        } catch (Exception e) {
            logger.error("Cannot open private SpoolRepository");
            throw e;
        }
        logger.info("Private SpoolRepository Spool opened");
        compMgr.put("org.apache.james.services.SpoolRepository", (Component)spool);

      

	try {
	    localusers = (UsersRepository) usersStore.getRepository("LocalUsers");
	} catch (Exception e) {
		logger.error("Cannot open private UserRepository");
		throw e;
	}
	//}
        compMgr.put("org.apache.james.services.UsersRepository", (Component)localusers);
        logger.info("Users Manager Opened");
      

        POP3Server pop3Server = new POP3Server();
        try {
            pop3Server.configure(conf.getChild("pop3Server"));
            pop3Server.contextualize(context);
            pop3Server.compose(compMgr);
        } catch (Exception e) {
            logger.error("Exception in POP3Server init: " + e.getMessage());
            throw e;
        }

        SMTPServer smtpServer = new SMTPServer();
        try {
            smtpServer.configure(conf.getChild("smtpServer"));
            smtpServer.contextualize(context);
            smtpServer.compose(compMgr);
        } catch (Exception e) {
            logger.error("Exception in SMTPServer init: " + e.getMessage());
            throw e;
        }

        DNSServer dnsServer = new DNSServer();
        try {
            dnsServer.configure(conf.getChild("dnsServer"));
            dnsServer.compose(compMgr);
        } catch (Exception e) {
            logger.error("Exception in DNSServer init: " + e.getMessage());
            throw e;
        }
        compMgr.put("DNS_SERVER", dnsServer);

        RemoteManager remoteAdmin = new RemoteManager();
        try {
            remoteAdmin.configure(conf.getChild("remoteManager"));
            remoteAdmin.compose(compMgr);
        } catch (Exception e) {
            logger.error("Exception in RemoteAdmin init: " + e.getMessage());
            throw e;
        }

	// For mailet engine provide MailetContext
        compMgr.put("org.apache.mailet.MailetContext", this);
        // For AVALON aware mailets and matchers, we put the Component object as
        // an attribute
        attributes.put(Constants.AVALON_COMPONENT_MANAGER, compMgr);

	// int threads = conf.getConfiguration("spoolmanagerthreads").getValueAsInt(1);
        //while (threads-- > 0) {
	try {
	    JamesSpoolManager spoolMgr = new JamesSpoolManager();
	    spoolMgr.configure(conf.getChild("spoolmanager"));
	    spoolMgr.contextualize(context);
	    spoolMgr.compose(compMgr);
	    spoolMgr.init();
	    workerPool.execute((Runnable)spoolMgr);
	} catch (Exception e) {
	    logger.error("Exception in SpoolManager init: " + e.getMessage());
	    throw e;
	}
	logger.info("SpoolManager started");
	//}

        pop3Server.init();
        smtpServer.init();
        dnsServer.init();
        remoteAdmin.init();

        logger.info("JAMES ...init end");
    }
    

    public void sendMail(MimeMessage message) throws MessagingException {
        MailAddress sender = new MailAddress((InternetAddress)message.getFrom()[0]);
        Collection recipients = new Vector();
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
        logger.info("Mail " + mailimpl.getName() + " pushed in spool");
    }


    public synchronized MailRepository getUserInbox(String userName) {

        MailRepository userInbox = (MailRepository) null;
        
	userInbox = (MailRepository) mailboxes.get(userName);
       
	if (userInbox != null) {
	    return userInbox;
	} else if (mailboxes.containsKey(userName)) {
	    // we have a problem
	    logger.error("Null mailbox for non-null key");
	    throw new RuntimeException("Error in getUserInbox.");
	} else {
	    // need mailbox object
	    logger.info("Need inbox for " + userName );
	    String destination = inboxRootURL + userName + File.separator;;
	    DefaultConfiguration mboxConf
		= new DefaultConfiguration("repository", "generated:AvalonFileRepository.compose()");
	    mboxConf.addAttribute("destinationURL", destination);
	    mboxConf.addAttribute("type", "MAIL");
	    mboxConf.addAttribute("model", "SYNCHRONOUS");
	    try {
		userInbox = (MailRepository) mailstore.select(mboxConf);
		mailboxes.put(userName, userInbox);
	    } catch (Exception e) {
		logger.error("Cannot open user Mailbox" + e);
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


    public void destroy() {
        //Does nothing... is this even called?
    }


    //Methods for MailetContext

    public Collection getMailServers(String host) {
	DNSServer dnsServer = null;
	try {
	    dnsServer = (DNSServer) compMgr.lookup("DNS_SERVER");
	} catch (CascadingException ex) {
	    logger.error("Fatal configuration error - DNS Servers lost!");
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
        Collection recipients = new Vector();
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
        for (Iterator it = localusers.list(); it.hasNext(); ) {
            userList.add(it.next());
        }
        return userList;
    }

    public MailAddress getPostmaster() {
        return (MailAddress)context.get(Constants.POSTMASTER);
    }

    public void storeMail(MailAddress sender, MailAddress recipient, MimeMessage message) {
        Vector recipients = new Vector();
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
        mailetLogger.info(message);
    }

    public void log(String message, Throwable t) {
        System.err.println(message);
        t.printStackTrace(); //DEBUG
        mailetLogger.info(message + ": " + t.getMessage());
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
        localusers.addUser(userName, password);
        return true;
    }

}
