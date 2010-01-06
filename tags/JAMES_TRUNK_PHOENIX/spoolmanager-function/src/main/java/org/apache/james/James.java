/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/



package org.apache.james;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.container.ContainerUtil;

import org.apache.commons.collections.map.ReferenceMap;
import org.apache.commons.configuration.CombinedConfiguration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;

import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.dnsservice.TemporaryResolutionException;
import org.apache.james.api.domainlist.DomainList;
import org.apache.james.api.domainlist.ManageableDomainList;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.core.MailHeaders;
import org.apache.james.core.MailImpl;
import org.apache.james.impl.jamesuser.JamesUsersRepository;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailServer;
import org.apache.james.services.SpoolRepository;
import org.apache.james.transport.MailetConfigImpl;
import org.apache.james.transport.mailets.LocalDelivery;
import org.apache.mailet.HostAddress;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetContext;
import org.apache.mailet.base.RFC2822Headers;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Vector;

/**
 * Core class for JAMES. Provides three primary services:
 * <br> 1) Instantiates resources, such as user repository, and protocol
 * handlers
 * <br> 2) Handles interactions between components
 * <br> 3) Provides container services for Mailets
 *
 *
 * @version This is $Revision$

 */
@SuppressWarnings("unchecked")
public class James
    implements MailServer, MailetContext {

    /**
     * The software name and version
     */
    private final static String SOFTWARE_NAME_VERSION = Constants.SOFTWARE_NAME + " " + Constants.SOFTWARE_VERSION;

    /**
     * The top level configuration object for this server.
     */
    private HierarchicalConfiguration conf = null;

    /**
     * The logger used by the Mailet API.
     */
    private Log mailetLogger = null;

    /**
     * The mail store containing the inbox repository and the spool.
     */
    private Store store;

    /**
     * The spool used for processing mail handled by this server.
     */
    private SpoolRepository spool;

    /**
     * The root URL used to get mailboxes from the repository
     */
    private String inboxRootURL;

    /**
     * The user repository for this mail server.  Contains all the users with inboxes
     * on this server.
     */
    private UsersRepository localusers;

    /**
     * The collection of domain/server names for which this instance of James
     * will receive and process mail.
     */
    private Collection<String> serverNames;

    /**
     * The number of mails generated.  Access needs to be synchronized for
     * thread safety and to ensure that all threads see the latest value.
     */
    private static int count = 0;
    private static final Object countLock = new Object();

    /**
     * The address of the postmaster for this server
     */
    private MailAddress postmaster;

    /**
     * A map used to store mailboxes and reduce the cost of lookup of individual
     * mailboxes.
     */
    private Map<String,MailRepository> mailboxes = new ReferenceMap();


    /**
     * A hash table of server attributes
     * These are the MailetContext attributes
     */
    private Hashtable<String,Object> attributes = new Hashtable<String,Object>();

    /**
     * Currently used by storeMail to avoid code duplication (we moved store logic to that mailet).
     * TODO We should remove this and its initialization when we remove storeMail method.
     */
    protected Mailet localDeliveryMailet;

    private FileSystem fileSystem;

    private DomainList domains;
    
    private boolean virtualHosting = false;
    
    private String defaultDomain = null;
    
    private String helloName = null;

    private Log logger;

    private DNSService dns;


    /**
     * Sets the fileSystem service
     * 
     * @param system the new service
     */
    @Resource(name="filesystem")
    public void setFileSystem(FileSystem system) {
        this.fileSystem = system;
    }
    
    @Resource(name="domainlist")
    public void setDomainList(DomainList domains) {
        this.domains = domains;
    }
    
    @Resource(name="dnsserver")
    public void setDNSService(DNSService dns) {
        this.dns = dns;
    }
    
    @Resource(name="org.apache.commons.logging.Log")
    public final void setLogger(Log logger) {
        this.logger = logger;
    }
    
    @Resource(name="org.apache.commons.configuration.Configuration")
    public final void setConfiguration(HierarchicalConfiguration config) {
        this.conf = config;
    }
    
    
    @Resource(name="org.apache.commons.logging.Log@MailetLog")
    public final void setMailetLogger(Log mailetLogger) {
        this.mailetLogger = mailetLogger;
    }
    
    
    @PostConstruct
    public void init() throws Exception {

        logger.info("JAMES init...");

        initializeServices();

        if (conf.getKeys("usernames").hasNext()) {
            HierarchicalConfiguration userNamesConf = conf.configurationAt("usernames");

            if (localusers instanceof JamesUsersRepository) {
                logger.warn("<usernames> parameter in James block is deprecated. Please configure this data in UsersRepository block: configuration injected for backward compatibility");
                ((JamesUsersRepository) localusers).setIgnoreCase(userNamesConf.getBoolean("[@ignoreCase]", false));
                ((JamesUsersRepository) localusers).setEnableAliases(userNamesConf.getBoolean("[@enableAliases]", false));
                ((JamesUsersRepository) localusers).setEnableForwarding(userNamesConf.getBoolean("[@enableForwarding]", false));
            } else {
                logger.error("<usernames> parameter is no more supported. Backward compatibility is provided when using an AbstractUsersRepository but this repository is a "+localusers.getClass().toString());
            }
        }
        
        if (conf.getKeys("servernames").hasNext()) {
            HierarchicalConfiguration serverConf = conf.configurationAt("servernames");
            if (domains instanceof ManageableDomainList) {
                logger.warn("<servernames> parameter in James block is deprecated. Please configure this data in domainlist block: configuration injected for backward compatibility");
                ManageableDomainList dom = (ManageableDomainList) domains;
                dom.setAutoDetect(serverConf.getBoolean("[@autodetect]",true));    
                dom.setAutoDetectIP(serverConf.getBoolean("[@autodetectIP]", true));
            
                List<String> serverNameConfs = serverConf.getList( "servername" );
                for ( int i = 0; i < serverNameConfs.size(); i++ ) {
                    dom.addDomain( serverNameConfs.get(i).toLowerCase(Locale.US));
                }
            } else {
                logger.error("<servernames> parameter is no more supported. Backward compatibility is provided when using an XMLDomainList");
            }
        }

        initializeServernamesAndPostmaster();

        // We don't need this. UsersRepository.ROLE is already in the compMgr we received
        // We've just looked up it from the cmpManager
        // compMgr.put( UsersRepository.ROLE, localusers);
        // getLogger().info("Local users repository opened");

        inboxRootURL = conf.configurationAt("inboxRepository.repository").getString("[@destinationURL]");

        logger.info("Private Repository LocalInbox opened");
        
        virtualHosting = conf.getBoolean("enableVirtualHosting", false);

        logger.info("VirtualHosting supported: " + virtualHosting);
        
        defaultDomain = conf.getString("defaultDomain",null);
        if (defaultDomain == null && virtualHosting) {
            throw new ConfigurationException("Please configure a defaultDomain if using VirtualHosting");
        }
        
        logger.info("Defaultdomain: " + defaultDomain);
        
        if (conf.getKeys("helloName").hasNext()) {
            HierarchicalConfiguration helloNameConfig = conf.configurationAt("helloName");
            boolean autodetect = helloNameConfig.getBoolean("[@autodetect]", true);
            if (autodetect) {
                try {
                    helloName = dns.getHostName(dns.getLocalHost());
                } catch (UnknownHostException e) {
                    helloName = "localhost";
                }
            } else {
                // Should we use the defaultdomain here ?
                helloName = conf.getString("helloName",defaultDomain);
            }
            attributes.put(Constants.HELLO_NAME, helloName);
        }

        //Temporary get out to allow complex mailet config files to stop blocking sergei sozonoff's work on bouce processing
        String confDir = conf.getString("configuration-directory", null);
        
        // defaults to the old behaviour
        if (confDir == null) confDir = "file://conf/";
        java.io.File configDir = fileSystem.getFile(confDir);
        attributes.put("confDir", configDir.getCanonicalPath());

        try {
            attributes.put(Constants.HOSTADDRESS, dns.getLocalHost().getHostAddress());
            attributes.put(Constants.HOSTNAME, dns.getLocalHost().getHostName());
        } catch (java.net.UnknownHostException _) {
            attributes.put(Constants.HOSTADDRESS, "127.0.0.1");
            attributes.put(Constants.HOSTNAME, "localhost");
        }
        
        initializeLocalDeliveryMailet();

        System.out.println(SOFTWARE_NAME_VERSION);
        logger.info("JAMES ...init end");
    }

    private void initializeServices() throws Exception {
        // TODO: This should retrieve a more specific named thread pool from
        // Context that is set up in server.xml
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Using Store: " + store.toString());
            }
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Can't get Store: " + e);
            }
        }

        try {
            if (logger.isDebugEnabled()) {
                logger.debug("Using SpoolRepository: " + spool.toString());
            }
        } catch (Exception e) {
            if (logger.isWarnEnabled()) {
                logger.warn("Can't get spoolRepository: " + e);
            }
        }

        /*
        try {
            // lookup the usersStore.
            // This is not used by James itself, but we check we received it here
            // because mailets will try to lookup this later.
            UsersStore usersStore = (UsersStore) compMgr.lookup( UsersStore.ROLE );
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Using UsersStore: " + usersStore.toString());
            }
        } catch (Exception e) {
            if (getLogger().isWarnEnabled()) {
                getLogger().warn("Can't get Store: " + e);
            }
        }
        */
        if (logger.isDebugEnabled()) {
            logger.debug("Using LocalUsersRepository: " + localusers.toString());
        }    
    }

    private void initializeServernamesAndPostmaster() throws ConfigurationException, ParseException {
        String defaultDomain = getDefaultDomain();
        if (domains.containsDomain(defaultDomain) == false) {
            if (domains instanceof ManageableDomainList) {
                if(((ManageableDomainList) domains).addDomain(defaultDomain) != false) {
                    throw new ConfigurationException("Configured defaultdomain could not get added to DomainList");
                }
            } else {
                throw new ConfigurationException("Configured defaultDomain not exist in DomainList");
            }
        }
        serverNames = domains.getDomains();

        if (serverNames == null || serverNames.size() == 0) throw new ConfigurationException("No domainnames configured");
        
        // used by RemoteDelivery for HELO
        attributes.put(Constants.DEFAULT_DOMAIN, defaultDomain);

        // Get postmaster
        String postMasterAddress = conf.getString("postmaster","postmaster").toLowerCase(Locale.US);
        // if there is no @domain part, then add the first one from the
        // list of supported domains that isn't localhost.  If that
        // doesn't work, use the hostname, even if it is localhost.
        if (postMasterAddress.indexOf('@') < 0) {
            String domainName = null;    // the domain to use
            // loop through candidate domains until we find one or exhaust the list
            Iterator<String> i = serverNames.iterator();
            while (i.hasNext()) {
                String serverName = i.next().toLowerCase(Locale.US);
                if (!("localhost".equals(serverName))) {
                    domainName = serverName; // ok, not localhost, so use it
                    continue;
                }
            }
            // if we found a suitable domain, use it.  Otherwise fallback to the host name.
            postMasterAddress = postMasterAddress + "@" + (domainName != null ? domainName : defaultDomain);
        }
        this.postmaster = new MailAddress( postMasterAddress );

        if (!isLocalServer(postmaster.getDomain())) {
            StringBuffer warnBuffer
                    = new StringBuffer(320)
                    .append("The specified postmaster address ( ")
                    .append(postmaster)
                    .append(" ) is not a local address.  This is not necessarily a problem, but it does mean that emails addressed to the postmaster will be routed to another server.  For some configurations this may cause problems.");
            logger.warn(warnBuffer.toString());
        }
    }

    private void initializeLocalDeliveryMailet() throws MessagingException {
        // We can safely remove this and the localDeliveryField when we 
        // remove the storeMail method from James and from the MailetContext
        DefaultConfigurationBuilder conf = new DefaultConfigurationBuilder();
        MailetConfigImpl configImpl = new MailetConfigImpl();
        configImpl.setMailetName("LocalDelivery");
        configImpl.setConfiguration(conf);
        configImpl.setMailetContext(this);
        localDeliveryMailet = new LocalDelivery();
        localDeliveryMailet.init(configImpl);
    }

    /**
     * Set Store to use
     * 
     * @param store the Store to use
     */
    @Resource(name="mailstore")
    public void setStore(Store store) {
        this.store = store;
    }

    /**
     * Set the SpoolRepository to use
     * 
     * @param spool the SpoleRepository to use
     */
    @Resource(name="spoolrepository")
    public void setSpoolRepository(SpoolRepository spool) {
        this.spool = spool;
    }

    /**
     * Set the UsersRepository to use
     * 
     * @param localusers the UserRepository to use
     */
    @Resource(name="localusersrepository")
    public void setUsersRepository(UsersRepository localusers) {
        this.localusers = localusers;
    }

    /**
     * Place a mail on the spool for processing
     *
     * @param message the message to send
     *
     * @throws MessagingException if an exception is caught while placing the mail
     *                            on the spool
     */
    public void sendMail(MimeMessage message) throws MessagingException {
        MailAddress sender = new MailAddress((InternetAddress)message.getFrom()[0]);
        Collection<MailAddress> recipients = new HashSet<MailAddress>();
        Address addresses[] = message.getAllRecipients();
        if (addresses != null) {
            for (int i = 0; i < addresses.length; i++) {
                // Javamail treats the "newsgroups:" header field as a
                // recipient, so we want to filter those out.
                if ( addresses[i] instanceof InternetAddress ) {
                    recipients.add(new MailAddress((InternetAddress)addresses[i]));
                }
            }
        }
        sendMail(sender, recipients, message);
    }

    /**
     * @see org.apache.james.services.MailServer#sendMail(MailAddress, Collection, MimeMessage)
     */
    public void sendMail(MailAddress sender, Collection recipients, MimeMessage message)
            throws MessagingException {
        sendMail(sender, recipients, message, Mail.DEFAULT);
    }

    /**
     * @see org.apache.mailet.MailetContext#sendMail(MailAddress, Collection, MimeMessage, String)
     */
    public void sendMail(MailAddress sender, Collection recipients, MimeMessage message, String state)
            throws MessagingException {
        MailImpl mail = new MailImpl(getId(), sender, recipients, message);
        try {
            mail.setState(state);
            sendMail(mail);
        } finally {
            ContainerUtil.dispose(mail);
        }
    }

    /**
     * @see org.apache.james.services.MailServer#sendMail(MailAddress, Collection, InputStream)
     */
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

    /**
     * @see org.apache.james.services.MailServer#sendMail(Mail)
     */
    public void sendMail(Mail mail) throws MessagingException {
        try {
            spool.store(mail);
        } catch (Exception e) {
            logger.error("Error storing message: " + e.getMessage(),e);
            try {
                spool.remove(mail);
            } catch (Exception ignored) {
                logger.error("Error removing message after an error storing it: " + e.getMessage(),e);
            }
            throw new MessagingException("Exception spooling message: " + e.getMessage(), e);
        }
        if (logger.isDebugEnabled()) {
            StringBuffer logBuffer =
                new StringBuffer(64)
                        .append("Mail ")
                        .append(mail.getName())
                        .append(" pushed in spool");
            logger.debug(logBuffer.toString());
        }
    }

    /**
     * @see org.apache.james.services.MailServer#getUserInbox(java.lang.String)
     */
    public synchronized MailRepository getUserInbox(String userName) {
        MailRepository userInbox = null;
        
        if (virtualHosting == false && (userName.indexOf("@") < 0) == false) {
            userName = userName.split("@")[0];
        }

        userInbox = (MailRepository) mailboxes.get(userName);

        if (userInbox != null) {
            return userInbox;
        /*
         * we're using a ReferenceMap with HARD keys and SOFT values
         * so it could happen to find a null value after a second pass
         * of a full GC and we should simply lookup it again
         */
//        } else if (mailboxes.containsKey(userName)) {
//            // we have a problem
//            getLogger().error("Null mailbox for non-null key");
//            throw new RuntimeException("Error in getUserInbox.");
        } else {
            // need mailbox object
            if (logger.isDebugEnabled()) {
                logger.debug("Retrieving and caching inbox for " + userName );
            }

            StringBuffer destinationBuffer = new StringBuffer(192);
                  
            if (virtualHosting == true && inboxRootURL.startsWith("file://") && !(userName.indexOf("@") < 0)) {
                String userArgs[] = userName.split("@");
                            
                // build the url like : file://var/mail/inboxes/domain/username/
                destinationBuffer.append(inboxRootURL).append(userArgs[1]).append("/").append(userArgs[0]).append("/");
            } else {
                destinationBuffer.append(inboxRootURL).append(userName).append("/");
            }
                 
            String destination = destinationBuffer.toString();
            try {
                // Copy the inboxRepository configuration and modify the destinationURL
                CombinedConfiguration mboxConf = new CombinedConfiguration();
                mboxConf.addConfiguration(conf.configurationAt("inboxRepository.repository"));
                mboxConf.setProperty("[@destinationURL]", destination);

                userInbox = (MailRepository) store.select(mboxConf);
                if (userInbox!=null) {
                    mailboxes.put(userName, userInbox);
                }
            } catch (Exception e) {
                if (logger.isErrorEnabled()) {
                    logger.error("Cannot open user Mailbox",e);
                }
                throw new RuntimeException("Error in getUserInbox.",e);
            }
            return userInbox;
        }
    }

    /**
     * <p>Note that this method ensures that James cannot be run in a distributed
     * fashion.</p>
     * <p>Two instances may return the same ID. 
     * There are various ways that this could be fixed. 
     * The most obvious would be to add a unique prefix. 
     * The best approach would be for each instance to be configured
     * with a name which would then be combined with the network
     * address (for example, james.name@mail.example.org) to create a
     * unique James instance identifier.
     * </p><p> 
     * Alternatively, using a data store backed identifier (for example, from a sequence
     * when DB backed) should be enough to gaurantee uniqueness. This would imply
     * that the Mail interface or the spool store should be responsible for creating
     * new Mail implementations with ID preassigned. 
     * </p><p>
     * It would be useful for each 
     * James cluster to have a unique name. Perhaps a random number could be generated by 
     * the spool store upon first initialisation.
     * </p><p>
     * This ID is most likely
     * to be used as message ID so this is probably useful in any case.
     * </p>
     * 
     * @see org.apache.james.services.MailServer#getId()
     */
    public String getId() {
        
        final long localCount;
        synchronized (countLock) {
            localCount = count++;
        }
        StringBuffer idBuffer =
            new StringBuffer(64)
                    .append("Mail")
                    .append(System.currentTimeMillis())
                    .append("-")
                    .append(localCount);
        return idBuffer.toString();
    }

    /**
     * The main method.  Should never be invoked, as James must be called
     * from within an Avalon framework container.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        System.out.println("ERROR!");
        System.out.println("Cannot execute James as a stand alone application.");
        System.out.println("To run James, you need to have the Avalon framework installed.");
        System.out.println("Please refer to the Readme file to know how to run James.");
    }

    //Methods for MailetContext

    /**
     * @see org.apache.mailet.MailetContext#getMailServers(String)
     */
    public Collection<String> getMailServers(String host) {
        try {
            return dns.findMXRecords(host);
        } catch (TemporaryResolutionException e) {
            //TODO: We only do this to not break backward compatiblity. Should fixed later
            return Collections.unmodifiableCollection(new ArrayList<String>(0));
        }
    }

    /**
     * @see org.apache.mailet.MailetContext#getAttribute(java.lang.String)
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * @see org.apache.mailet.MailetContext#setAttribute(java.lang.String, java.lang.Object)
     */
    public void setAttribute(String key, Object object) {
        attributes.put(key, object);
    }

    /**
     * @see org.apache.mailet.MailetContext#removeAttribute(java.lang.String)
     */
    public void removeAttribute(String key) {
        attributes.remove(key);
    }

    /**
     * @see org.apache.mailet.MailetContext#getAttributeNames()
     */
    public Iterator<String> getAttributeNames() {
        Vector<String> names = new Vector<String>();
        for (Enumeration e = attributes.keys(); e.hasMoreElements(); ) {
            names.add(e.nextElement().toString());
        }
        return names.iterator();
    }

    /**
     * This generates a response to the Return-Path address, or the address of
     * the message's sender if the Return-Path is not available.  Note that
     * this is different than a mail-client's reply, which would use the
     * Reply-To or From header. This will send the bounce with the server's
     * postmaster as the sender.
     * 
     * @see org.apache.mailet.MailetContext#bounce(Mail, String)
     */
    public void bounce(Mail mail, String message) throws MessagingException {
        bounce(mail, message, getPostmaster());
    }

    /**
     * This generates a response to the Return-Path address, or the
     * address of the message's sender if the Return-Path is not
     * available.  Note that this is different than a mail-client's
     * reply, which would use the Reply-To or From header.
     *
     * Bounced messages are attached in their entirety (headers and
     * content) and the resulting MIME part type is "message/rfc822".
     *
     * The attachment to the subject of the original message (or "No
     * Subject" if there is no subject in the original message)
     *
     * There are outstanding issues with this implementation revolving
     * around handling of the return-path header.
     *
     * MIME layout of the bounce message:
     *
     * multipart (mixed)/
     *     contentPartRoot (body) = mpContent (alternative)/
     *           part (body) = message
     *     part (body) = original
     *
     * @see org.apache.mailet.MailetContext#bounce(Mail, String, MailAddress) 
     */

    public void bounce(Mail mail, String message, MailAddress bouncer) throws MessagingException {
        if (mail.getSender() == null) {
            if (logger.isInfoEnabled())
                logger.info("Mail to be bounced contains a null (<>) reverse path.  No bounce will be sent.");
            return;
        } else {
            // Bounce message goes to the reverse path, not to the Reply-To address
            if (logger.isInfoEnabled())
                logger.info("Processing a bounce request for a message with a reverse path of " + mail.getSender().toString());
        }

        MailImpl reply = rawBounce(mail,message);
        //Change the sender...
        reply.getMessage().setFrom(bouncer.toInternetAddress());
        reply.getMessage().saveChanges();
        //Send it off ... with null reverse-path
        reply.setSender(null);
        sendMail(reply);
        ContainerUtil.dispose(reply);
    }

    /**
     * Generates a bounce mail that is a bounce of the original message.
     *
     * @param bounceText the text to be prepended to the message to describe the bounce condition
     *
     * @return the bounce mail
     *
     * @throws MessagingException if the bounce mail could not be created
     */
    private MailImpl rawBounce(Mail mail, String bounceText) throws MessagingException {
        //This sends a message to the james component that is a bounce of the sent message
        MimeMessage original = mail.getMessage();
        MimeMessage reply = (MimeMessage) original.reply(false);
        reply.setSubject("Re: " + original.getSubject());
        reply.setSentDate(new Date());
        Collection<MailAddress> recipients = new HashSet<MailAddress>();
        recipients.add(mail.getSender());
        InternetAddress addr[] = { new InternetAddress(mail.getSender().toString())};
        reply.setRecipients(Message.RecipientType.TO, addr);
        reply.setFrom(new InternetAddress(mail.getRecipients().iterator().next().toString()));
        reply.setText(bounceText);
        reply.setHeader(RFC2822Headers.MESSAGE_ID, "replyTo-" + mail.getName());
        return new MailImpl(
            "replyTo-" + mail.getName(),
            new MailAddress(mail.getRecipients().iterator().next().toString()),
            recipients,
            reply);
    }

    /**
     * @see org.apache.mailet.MailetContext#isLocalUser(String)
     */
    public boolean isLocalUser(String name) {
        if (name == null) {
            return false;
        }
        try {
            if (name.indexOf("@") == -1) {
                return isLocalEmail(new MailAddress(name,"localhost"));
            } else {
                return isLocalEmail(new MailAddress(name));
            }
        } catch (ParseException e) {
            log("Error checking isLocalUser for user "+name);
            return false;
        }
    }
    
    /**
     * @see org.apache.mailet.MailetContext#isLocalEmail(org.apache.mailet.MailAddress)
     */
    public boolean isLocalEmail(MailAddress mailAddress) {
    String userName = mailAddress.toString();
        if (!isLocalServer(mailAddress.getDomain())) {
            return false;
        }
        if (virtualHosting == false) {
            userName = mailAddress.getLocalPart();
        }
        return localusers.contains(userName);
    }

    /**
     * @see org.apache.mailet.MailetContext#getPostmaster()
     */
    public MailAddress getPostmaster() {
        return postmaster;
    }

    /**
     * @see org.apache.mailet.MailetContext#getMajorVersion()
     */
    public int getMajorVersion() {
        return 2;
    }

    /**
     * @see org.apache.mailet.MailetContext#getMinorVersion()
     */
    public int getMinorVersion() {
        return 4;
    }

    /**
     * @see org.apache.james.services.MailServer#isLocalServer(java.lang.String)
     */
    public boolean isLocalServer( final String serverName ) {
        String lowercase = serverName.toLowerCase(Locale.US);
       
        // Check if the serverName is localhost or the DomainList implementation contains the serverName. This
        // allow some implementations to act more dynamic
        if ("localhost".equals(serverName) || domains.containsDomain(lowercase)){
            return  true;
        } else {
            return false;
        }
    }

    /**
     * @see org.apache.mailet.MailetContext#getServerInfo()
     */
    public String getServerInfo() {
        return "Apache JAMES";
    }

    /**
     * Return the logger for the Mailet API
     *
     * @return the logger for the Mailet API
     */
    private Log getMailetLogger() {
        return mailetLogger;
    }

    /**
     * @see org.apache.mailet.MailetContext#log(java.lang.String)
     */
    public void log(String message) {
        getMailetLogger().info(message);
    }

    /**
     * @see org.apache.mailet.MailetContext#log(java.lang.String, java.lang.Throwable)
     */
    public void log(String message, Throwable t) {
        getMailetLogger().info(message,t);
    }

    /**
     * Adds a user to this mail server. Currently just adds user to a
     * UsersRepository.
     *
     * @param userName String representing user name, that is the portion of
     * an email address before the '@<domain>'.
     * @param password String plaintext password
     * @return boolean true if user added succesfully, else false.
     * 
     * @deprecated we deprecated this in the MailServer interface and this is an implementation
     * this component depends already depends on a UsersRepository: clients could directly 
     * use the addUser of the usersRepository.
     */
    public boolean addUser(String userName, String password) {
        return localusers.addUser(userName, password);
    }

    /**
     * Performs DNS lookups as needed to find servers which should or might
     * support SMTP.
     * Returns an Iterator over HostAddress, a specialized subclass of
     * javax.mail.URLName, which provides location information for
     * servers that are specified as mail handlers for the given
     * hostname.  This is done using MX records, and the HostAddress
     * instances are returned sorted by MX priority.  If no host is
     * found for domainName, the Iterator returned will be empty and the
     * first call to hasNext() will return false.
     *
     * @see org.apache.james.api.dnsservice.DNSService#getSMTPHostAddresses(String)
     * @since Mailet API v2.2.0a16-unstable
     * @param domainName - the domain for which to find mail servers
     * @return an Iterator over HostAddress instances, sorted by priority
     */
    public Iterator<HostAddress> getSMTPHostAddresses(String domainName) {
        try {
            return dns.getSMTPHostAddresses(domainName);
        } catch (TemporaryResolutionException e) {
            //TODO: We only do this to not break backward compatiblity. Should fixed later
            return Collections.unmodifiableCollection(new ArrayList<HostAddress>(0)).iterator();
        }
    }

    /**
     * This method has been moved to LocalDelivery (the only client of the method).
     * Now we can safely remove it from the Mailet API and from this implementation of MailetContext.
     *
     * The local field localDeliveryMailet will be removed when we remove the storeMail method.
     * 
     * @deprecated since 2.2.0 look at the LocalDelivery code to find out how to do the local delivery.
     * @see org.apache.mailet.MailetContext#storeMail(org.apache.mailet.MailAddress, org.apache.mailet.MailAddress, javax.mail.internet.MimeMessage)
     */
    public void storeMail(MailAddress sender, MailAddress recipient, MimeMessage msg) throws MessagingException {
        if (recipient == null) {
            throw new IllegalArgumentException("Recipient for mail to be spooled cannot be null.");
        }
        if (msg == null) {
            throw new IllegalArgumentException("Mail message to be spooled cannot be null.");
        }
        Collection<MailAddress> recipients = new HashSet<MailAddress>();
        recipients.add(recipient);
        MailImpl m = new MailImpl(getId(),sender,recipients,msg);
        localDeliveryMailet.service(m);
        ContainerUtil.dispose(m);
    }
   
    /**
     * @see org.apache.james.services.MailServer#supportVirtualHosting()
     */
    public boolean supportVirtualHosting() {
        return virtualHosting;
    }

    /**
     * @see org.apache.james.services.MailServer#getDefaultDomain()
     */
    public String getDefaultDomain() {
        if (defaultDomain == null) {
            List<String> domainList = domains.getDomains();
            if (domainList == null || domainList.isEmpty()) {
                return "localhost";
            } else {
                return (String) domainList.get(0);
            }  
        } else {
            return defaultDomain;
        }
    }

    /**
     * @see org.apache.james.services.MailServer#getHelloName()
     */
    public String getHelloName() {
        if (helloName != null) {
            return helloName;
        } else {
            String hello = (String) getAttribute(Constants.HELLO_NAME);   
            if (hello == null) {
                return defaultDomain;
            } else {
                return hello;
            }
        }
    }
}
