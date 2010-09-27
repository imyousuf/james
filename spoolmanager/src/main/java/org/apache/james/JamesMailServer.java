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

import java.net.UnknownHostException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.ParseException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.domainlist.DomainList;
import org.apache.james.api.domainlist.ManageableDomainList;
import org.apache.james.dnsservice.api.DNSService;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.LifecycleUtil;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.queue.MailQueue;
import org.apache.james.queue.MailQueueFactory;
import org.apache.james.services.MailServer;
import org.apache.mailet.Mail;

/**
 * 
 */
public class JamesMailServer
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

    private MailQueueFactory queueFactory;
    
    private MailQueue queue;

    @Resource(name="domainlist")
    public void setDomainList(DomainList domains) {
        this.domains = domains;
    }
    
    @Resource(name="dnsserver")
    public void setDNSService(DNSService dns) {
        this.dns = dns;
    }
    
    @Resource(name="mailQueueFactory")
    public void setMailQueueFactory(MailQueueFactory queueFactory) {
        this.queueFactory = queueFactory;
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
    
    
    @SuppressWarnings("unchecked")
	@PostConstruct
    public void init() throws Exception {

        logger.info("JAMES init...");                

        queue = queueFactory.getQueue("spool");
        
        if (conf.getKeys("usernames").hasNext()) {
        	throw new ConfigurationException("<usernames> parameter in James block was removed. Please configure this data in UsersRepository block: configuration injected for backward compatibility");
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
                if(((ManageableDomainList) domains).addDomain(defaultDomain) == false) {
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
     * @see org.apache.james.services.MailServer#sendMail(Mail)
     */
    public void sendMail(Mail mail) throws MessagingException {
        try {
            queue.enQueue(mail);
                        
        } catch (Exception e) {
            logger.error("Error storing message: " + e.getMessage(),e);
            LifecycleUtil.dispose(mail);

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
            	return conf.getString("defaultDomain", "localhost");
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
    

}
