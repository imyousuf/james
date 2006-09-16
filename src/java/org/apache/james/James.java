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
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.configuration.DefaultConfiguration;
import org.apache.avalon.framework.container.ContainerUtil;
import org.apache.avalon.framework.context.Context;
import org.apache.avalon.framework.context.Contextualizable;
import org.apache.avalon.framework.context.DefaultContext;
import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.DefaultServiceManager;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.collections.ReferenceMap;

import org.apache.james.context.AvalonContextUtilities;
import org.apache.james.core.MailHeaders;
import org.apache.james.core.MailImpl;
import org.apache.james.core.MailetConfigImpl;
import org.apache.james.services.DNSServer;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailServer;
import org.apache.james.services.SpoolRepository;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.james.transport.mailets.LocalDelivery;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.Mailet;
import org.apache.mailet.MailetContext;
import org.apache.mailet.RFC2822Headers;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
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
public class James
    extends AbstractLogEnabled
    implements Contextualizable, Serviceable, Configurable, Initializable, MailServer, MailetContext {

    /**
     * The software name and version
     */
    private final static String SOFTWARE_NAME_VERSION = Constants.SOFTWARE_NAME + " " + Constants.SOFTWARE_VERSION;

    /**
     * The component manager used both internally by James and by Mailets.
     */
    private DefaultServiceManager compMgr; //Components shared

    /**
     * TODO: Investigate what this is supposed to do.  Looks like it
     *       was supposed to be the Mailet context.
     */
    private DefaultContext context;

    /**
     * The top level configuration object for this server.
     */
    private Configuration conf;

    /**
     * The logger used by the Mailet API.
     */
    private Logger mailetLogger = null;

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
    private Collection serverNames;

    /**
     * Whether to ignore case when looking up user names on this server
     */
    private boolean ignoreCase;

    /**
     * The number of mails generated.  Access needs to be synchronized for
     * thread safety and to ensure that all threads see the latest value.
     */
    private static long count;

    /**
     * The address of the postmaster for this server
     */
    private MailAddress postmaster;

    /**
     * A map used to store mailboxes and reduce the cost of lookup of individual
     * mailboxes.
     */
    private Map mailboxes; //Not to be shared!

    /**
     * A hash table of server attributes
     * These are the MailetContext attributes
     */
    private Hashtable attributes = new Hashtable();

    /**
     * The Avalon context used by the instance
     */
    protected Context           myContext;

    /**
     * Currently used by storeMail to avoid code duplication (we moved store logic to that mailet).
     * TODO We should remove this and its initialization when we remove storeMail method.
     */
    protected Mailet localDeliveryMailet;
    
    /**
     * @see org.apache.avalon.framework.context.Contextualizable#contextualize(Context)
     */
    public void contextualize(final Context context) {
        this.myContext = context;
    }

    /**
     * @see org.apache.avalon.framework.service.Serviceable#service(ServiceManager)
     */
    public void service(ServiceManager comp) {
        compMgr = new DefaultServiceManager(comp);
        mailboxes = new ReferenceMap();
    }

    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure(Configuration conf) {
        this.conf = conf;
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {

        getLogger().info("JAMES init...");

        initializeServices();

        initializeServernamesAndPostmaster();

        Configuration userNamesConf = conf.getChild("usernames");
        ignoreCase = userNamesConf.getAttributeAsBoolean("ignoreCase", false);
        boolean enableAliases = userNamesConf.getAttributeAsBoolean("enableAliases", false);
        boolean enableForwarding = userNamesConf.getAttributeAsBoolean("enableForwarding", false);
        attributes.put(Constants.DEFAULT_ENABLE_ALIASES,new Boolean(enableAliases));
        attributes.put(Constants.DEFAULT_ENABLE_FORWARDING,new Boolean(enableForwarding));
        attributes.put(Constants.DEFAULT_IGNORE_USERNAME_CASE,new Boolean(ignoreCase));

        compMgr.put( UsersRepository.ROLE, localusers);
        getLogger().info("Local users repository opened");

        inboxRootURL = conf.getChild("inboxRepository").getChild("repository").getAttribute("destinationURL");

        getLogger().info("Private Repository LocalInbox opened");

        // Add this to comp
        compMgr.put( MailServer.ROLE, this);

        // For mailet engine provide MailetContext
        //compMgr.put("org.apache.mailet.MailetContext", this);
        // For AVALON aware mailets and matchers, we put the Component object as
        // an attribute
        attributes.put(Constants.AVALON_COMPONENT_MANAGER, compMgr);

        //Temporary get out to allow complex mailet config files to stop blocking sergei sozonoff's work on bouce processing
        java.io.File configDir = AvalonContextUtilities.getFile(myContext, "file://conf/");
        attributes.put("confDir", configDir.getCanonicalPath());

        initializeLocalDeliveryMailet();

        System.out.println(SOFTWARE_NAME_VERSION);
        getLogger().info("JAMES ...init end");
    }

    private void initializeServices() throws Exception {
        // TODO: This should retrieve a more specific named thread pool from
        // Context that is set up in server.xml
        try {
            Store store = (Store) compMgr.lookup( Store.ROLE );
            setStore(store);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Using Store: " + store.toString());
            }
        } catch (Exception e) {
            if (getLogger().isWarnEnabled()) {
                getLogger().warn("Can't get Store: " + e);
            }
        }

        try {
            SpoolRepository spool = (SpoolRepository) compMgr.lookup(SpoolRepository.ROLE);
            setSpool(spool);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Using SpoolRepository: " + spool.toString());
            }
        } catch (Exception e) {
            if (getLogger().isWarnEnabled()) {
                getLogger().warn("Can't get spoolRepository: " + e);
            }
        }

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

        try {
            UsersRepository localusers = (UsersRepository) compMgr.lookup(UsersRepository.ROLE);
            setLocalusers(localusers);
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Using LocalUsersRepository: " + localusers.toString());
            }
        } catch (Exception e) {
            getLogger().error("Cannot open private UserRepository");
            throw e;
        }
    }

    private void initializeServernamesAndPostmaster() throws ConfigurationException, ParseException {
        String hostName = null;
        try {
            hostName = InetAddress.getLocalHost().getHostName();
        } catch  (UnknownHostException ue) {
            hostName = "localhost";
        }

        context = new DefaultContext();
        context.put("HostName", hostName);
        getLogger().info("Local host is: " + hostName);

        // Get the domains and hosts served by this instance
        serverNames = new HashSet();
        Configuration serverConf = conf.getChild("servernames");
        if (serverConf.getAttributeAsBoolean("autodetect") && (!hostName.equals("localhost"))) {
            serverNames.add(hostName.toLowerCase(Locale.US));
        }

        final Configuration[] serverNameConfs =
                conf.getChild( "servernames" ).getChildren( "servername" );
        for ( int i = 0; i < serverNameConfs.length; i++ ) {
            serverNames.add( serverNameConfs[i].getValue().toLowerCase(Locale.US));

            if (serverConf.getAttributeAsBoolean("autodetectIP", true)) {
                try {
                    /* This adds the IP address(es) for each host to support
                    * support <user@address-literal> - RFC 2821, sec 4.1.3.
                    * It might be proper to use the actual IP addresses
                    * available on this server, but we can't do that
                    * without NetworkInterface from JDK 1.4.  Because of
                    * Virtual Hosting considerations, we may need to modify
                    * this to keep hostname and IP associated, rather than
                    * just both in the set.
                    */
                    InetAddress[] addrs = InetAddress.getAllByName(serverNameConfs[i].getValue());
                    for (int j = 0; j < addrs.length ; j++) {
                        serverNames.add(addrs[j].getHostAddress());
                    }
                }
                catch(Exception genericException) {
                    getLogger().error("Cannot get IP address(es) for " + serverNameConfs[i].getValue());
                }
            }
        }
        if (serverNames.isEmpty()) {
            throw new ConfigurationException( "Fatal configuration error: no servernames specified!");
        }

        if (getLogger().isInfoEnabled()) {
            for (Iterator i = serverNames.iterator(); i.hasNext(); ) {
                getLogger().info("Handling mail for: " + i.next());
            }
        }

        String defaultDomain = (String) serverNames.iterator().next();
        context.put(Constants.DEFAULT_DOMAIN, defaultDomain);
        attributes.put(Constants.DEFAULT_DOMAIN, defaultDomain);

        // Get postmaster
        String postMasterAddress = conf.getChild("postmaster").getValue("postmaster").toLowerCase(Locale.US);
        // if there is no @domain part, then add the first one from the
        // list of supported domains that isn't localhost.  If that
        // doesn't work, use the hostname, even if it is localhost.
        if (postMasterAddress.indexOf('@') < 0) {
            String domainName = null;    // the domain to use
            // loop through candidate domains until we find one or exhaust the list
            for ( int i = 0; domainName == null && i < serverNameConfs.length ; i++ ) {
                String serverName = serverNameConfs[i].getValue().toLowerCase(Locale.US);
                if (!("localhost".equals(serverName))) {
                    domainName = serverName;    // ok, not localhost, so use it
                }
            }
            // if we found a suitable domain, use it.  Otherwise fallback to the host name.
            postMasterAddress = postMasterAddress + "@" + (domainName != null ? domainName : hostName);
        }
        this.postmaster = new MailAddress( postMasterAddress );
        context.put( Constants.POSTMASTER, postmaster );

        if (!isLocalServer(postmaster.getHost())) {
            StringBuffer warnBuffer
                    = new StringBuffer(320)
                    .append("The specified postmaster address ( ")
                    .append(postmaster)
                    .append(" ) is not a local address.  This is not necessarily a problem, but it does mean that emails addressed to the postmaster will be routed to another server.  For some configurations this may cause problems.");
            getLogger().warn(warnBuffer.toString());
        }
    }

    private void initializeLocalDeliveryMailet() throws MessagingException {
        // We can safely remove this and the localDeliveryField when we 
        // remove the storeMail method from James and from the MailetContext
        DefaultConfiguration conf = new DefaultConfiguration("mailet", "generated:James.initialize()");
        MailetConfigImpl configImpl = new MailetConfigImpl();
        configImpl.setMailetName("LocalDelivery");
        configImpl.setConfiguration(conf);
        configImpl.setMailetContext(this);
        localDeliveryMailet = new LocalDelivery();
        localDeliveryMailet.init(configImpl);
    }

    public void setStore(Store store) {
        this.store = store;
    }

    public void setSpool(SpoolRepository spool) {
        this.spool = spool;
    }

    public void setLocalusers(UsersRepository localusers) {
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
        Collection recipients = new HashSet();
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
     * Place a mail on the spool for processing
     *
     * @param sender the sender of the mail
     * @param recipients the recipients of the mail
     * @param message the message to send
     *
     * @throws MessagingException if an exception is caught while placing the mail
     *                            on the spool
     */
    public void sendMail(MailAddress sender, Collection recipients, MimeMessage message)
            throws MessagingException {
        sendMail(sender, recipients, message, Mail.DEFAULT);
    }

    /**
     * Place a mail on the spool for processing
     *
     * @param sender the sender of the mail
     * @param recipients the recipients of the mail
     * @param message the message to send
     * @param state the state of the message
     *
     * @throws MessagingException if an exception is caught while placing the mail
     *                            on the spool
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
     * Place a mail on the spool for processing
     *
     * @param sender the sender of the mail
     * @param recipients the recipients of the mail
     * @param msg an <code>InputStream</code> containing the message
     *
     * @throws MessagingException if an exception is caught while placing the mail
     *                            on the spool
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
     * Place a mail on the spool for processing
     *
     * @param mail the mail to place on the spool
     *
     * @throws MessagingException if an exception is caught while placing the mail
     *                            on the spool
     */
    public void sendMail(Mail mail) throws MessagingException {
        try {
            spool.store(mail);
        } catch (Exception e) {
            getLogger().error("Error storing message: " + e.getMessage(),e);
            try {
                spool.remove(mail);
            } catch (Exception ignored) {
                getLogger().error("Error removing message after an error storing it: " + e.getMessage(),e);
            }
            throw new MessagingException("Exception spooling message: " + e.getMessage(), e);
        }
        if (getLogger().isDebugEnabled()) {
            StringBuffer logBuffer =
                new StringBuffer(64)
                        .append("Mail ")
                        .append(mail.getName())
                        .append(" pushed in spool");
            getLogger().debug(logBuffer.toString());
        }
    }

    /**
     * <p>Retrieve the mail repository for a user</p>
     *
     * <p>For POP3 server only - at the moment.</p>
     *
     * @param userName the name of the user whose inbox is to be retrieved
     *
     * @return the POP3 inbox for the user
     */
    public synchronized MailRepository getUserInbox(String userName) {
        MailRepository userInbox = null;

        userInbox = (MailRepository) mailboxes.get(userName);

        if (userInbox != null) {
            return userInbox;
        } else if (mailboxes.containsKey(userName)) {
            // we have a problem
            getLogger().error("Null mailbox for non-null key");
            throw new RuntimeException("Error in getUserInbox.");
        } else {
            // need mailbox object
            if (getLogger().isDebugEnabled()) {
                getLogger().debug("Retrieving and caching inbox for " + userName );
            }
            StringBuffer destinationBuffer =
                new StringBuffer(192)
                        .append(inboxRootURL)
                        .append(userName)
                        .append("/");
            String destination = destinationBuffer.toString();
            try {
                // Copy the inboxRepository configuration and modify the destinationURL
                DefaultConfiguration mboxConf = new DefaultConfiguration(conf
                        .getChild("inboxRepository").getChild("repository"));
                mboxConf.setAttribute("destinationURL", destination);

                userInbox = (MailRepository) store.select(mboxConf);
                if (userInbox!=null) {
                    mailboxes.put(userName, userInbox);
                }
            } catch (Exception e) {
                if (getLogger().isErrorEnabled()) {
                    getLogger().error("Cannot open user Mailbox",e);
                }
                throw new RuntimeException("Error in getUserInbox.",e);
            }
            return userInbox;
        }
    }

    /**
     * Return a new mail id.
     *
     * @return a new mail id
     */
    public String getId() {
        long localCount = -1;
        synchronized (James.class) {
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
     * <p>Get the prioritized list of mail servers for a given host.</p>
     *
     * <p>TODO: This needs to be made a more specific ordered subtype of Collection.</p>
     *
     * @param host
     */
    public Collection getMailServers(String host) {
        return lookupDNSServer().findMXRecords(host);
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

    /**
     * This generates a response to the Return-Path address, or the address of
     * the message's sender if the Return-Path is not available.  Note that
     * this is different than a mail-client's reply, which would use the
     * Reply-To or From header. This will send the bounce with the server's
     * postmaster as the sender.
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
     */

    public void bounce(Mail mail, String message, MailAddress bouncer) throws MessagingException {
        if (mail.getSender() == null) {
            if (getLogger().isInfoEnabled())
                getLogger().info("Mail to be bounced contains a null (<>) reverse path.  No bounce will be sent.");
            return;
        } else {
            // Bounce message goes to the reverse path, not to the Reply-To address
            if (getLogger().isInfoEnabled())
                getLogger().info("Processing a bounce request for a message with a reverse path of " + mail.getSender().toString());
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
        Collection recipients = new HashSet();
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
     * Returns whether that account has a local inbox on this server
     *
     * @param name the name to be checked
     *
     * @return whether the account has a local inbox
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
        if (!isLocalServer(mailAddress.getHost())) {
            return false;
        }
        if (ignoreCase) {
            return localusers.containsCaseInsensitive(mailAddress.getUser());
        } else {
            return localusers.contains(mailAddress.getUser());
        }
    }

    /**
     * Returns the address of the postmaster for this server.
     *
     * @return the <code>MailAddress</code> for the postmaster
     */
    public MailAddress getPostmaster() {
        return postmaster;
    }

    /**
     * Return the major version number for the server
     *
     * @return the major vesion number for the server
     */
    public int getMajorVersion() {
        return 2;
    }

    /**
     * Return the minor version number for the server
     *
     * @return the minor vesion number for the server
     */
    public int getMinorVersion() {
        return 4;
    }

    /**
     * Check whether the mail domain in question is to be
     * handled by this server.
     *
     * @param serverName the name of the server to check
     * @return whether the server is local
     */
    public boolean isLocalServer( final String serverName ) {
        String lowercase = serverName.toLowerCase(Locale.US);
        return "localhost".equals(serverName) || serverNames.contains(lowercase);
    }

    /**
     * Return the type of the server
     *
     * @return the type of the server
     */
    public String getServerInfo() {
        return "Apache JAMES";
    }

    /**
     * Return the logger for the Mailet API
     *
     * @return the logger for the Mailet API
     */
    private Logger getMailetLogger() {
        if (mailetLogger == null) {
            mailetLogger = getLogger().getChildLogger("Mailet");
        }
        return mailetLogger;
    }

    /**
     * Log a message to the Mailet logger
     *
     * @param message the message to pass to the Mailet logger
     */
    public void log(String message) {
        getMailetLogger().info(message);
    }

    /**
     * Log a message and a Throwable to the Mailet logger
     *
     * @param message the message to pass to the Mailet logger
     * @param t the <code>Throwable</code> to be logged
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
     * @see org.apache.james.services.DNSServer#getSMTPHostAddresses(String)
     * @since Mailet API v2.2.0a16-unstable
     * @param domainName - the domain for which to find mail servers
     * @return an Iterator over HostAddress instances, sorted by priority
     */
    public Iterator getSMTPHostAddresses(String domainName) {
        return lookupDNSServer().getSMTPHostAddresses(domainName);
    }

    protected DNSServer lookupDNSServer() {
        DNSServer dnsServer;
        try {
            dnsServer = (DNSServer) compMgr.lookup( DNSServer.ROLE );
        } catch ( final ServiceException cme ) {
            getLogger().error("Fatal configuration error - DNS Servers lost!", cme );
            throw new RuntimeException("Fatal configuration error - DNS Servers lost!");
        }
        return dnsServer;
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
        Collection recipients = new HashSet();
        recipients.add(recipient);
        MailImpl m = new MailImpl(getId(),sender,recipients,msg);
        localDeliveryMailet.service(m);
        ContainerUtil.dispose(m);
    }
}
