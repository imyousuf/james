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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

import org.apache.camel.ExchangePattern;
import org.apache.camel.ProducerTemplate;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.domainlist.DomainList;
import org.apache.james.api.domainlist.ManageableDomainList;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.core.MailHeaders;
import org.apache.james.core.MailImpl;
import org.apache.james.impl.jamesuser.JamesUsersRepository;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.LifecycleUtil;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.services.MailServer;
import org.apache.james.transport.camel.InMemoryMail;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

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
public abstract class AbstractMailServer
    implements MailServer, LogEnabled, Configurable {

    /**
     * The software name and version
     */
    private final static String SOFTWARE_NAME_VERSION = Constants.SOFTWARE_NAME + " " + Constants.SOFTWARE_VERSION;

    /**
     * The top level configuration object for this server.
     */
    private HierarchicalConfiguration conf = null;


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

    private DomainList domains;
    
    private boolean virtualHosting = false;
    
    private String defaultDomain = null;
    
    private String helloName = null;

    private Log logger;

    private DNSService dns;

    private ProducerTemplate producerTemplate;

    @Resource(name="domainlist")
    public void setDomainList(DomainList domains) {
        this.domains = domains;
    }
    
    @Resource(name="dnsserver")
    public void setDNSService(DNSService dns) {
        this.dns = dns;
    }
    
    @Resource(name="producerTemplate")
    public void setProducerTemplate(ProducerTemplate producerTemplate) {
        this.producerTemplate = producerTemplate;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public final void setLog(Log logger) {
        this.logger = logger;
    }
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        this.conf = (HierarchicalConfiguration)config;
    }
    
    
    @PostConstruct
    public void init() throws Exception {

        logger.info("JAMES init...");

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

        initializeServernames();

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
        }


        System.out.println(SOFTWARE_NAME_VERSION);
        logger.info("JAMES ...init end");
    }


    private void initializeServernames() throws ConfigurationException, ParseException {
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
        try {
            sendMail(sender, recipients, message.getInputStream());
        } catch (IOException e) {
            throw new MessagingException("Unable to send message",e);
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
        Mail newMail = null;
        try {
        	newMail = new InMemoryMail(mail);
            producerTemplate.sendBody(getToUri(mail), ExchangePattern.InOnly, newMail);
                        
        } catch (Exception e) {
            logger.error("Error storing message: " + e.getMessage(),e);
            throw new MessagingException("Exception spooling message: " + e.getMessage(), e);
        } finally {
            LifecycleUtil.dispose(newMail);
            LifecycleUtil.dispose(mail);
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
            return getDefaultDomain();
        }
    }
    
    /**
     * Return the camel endpoint uri which should get used for the given mail
     * 
     * @param mail
     * @return toUri
     * 
     */
    protected abstract String getToUri(Mail mail);
}
