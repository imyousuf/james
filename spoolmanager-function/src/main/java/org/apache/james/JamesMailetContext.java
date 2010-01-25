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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Locale;
import java.util.Vector;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.ParseException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.DefaultConfigurationBuilder;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.dnsservice.TemporaryResolutionException;
import org.apache.james.api.domainlist.DomainList;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.core.MailImpl;
import org.apache.james.lifecycle.Configurable;
import org.apache.james.lifecycle.LifecycleUtil;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.services.MailServer;
import org.apache.james.transport.MailetConfigImpl;
import org.apache.james.transport.mailets.LocalDelivery;
import org.apache.mailet.HostAddress;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetContext;
import org.apache.mailet.base.RFC2822Headers;

public class JamesMailetContext implements MailetContext, LogEnabled, Configurable {

    private MailServer mailServer;

    /**
     * A hash table of server attributes These are the MailetContext attributes
     */
    private Hashtable<String, Object> attributes = new Hashtable<String, Object>();
    private DNSService dns;

    private Log log;

    private UsersRepository localusers;

    private LocalDelivery localDeliveryMailet;

    /**
     * The address of the postmaster for this server
     */
    private MailAddress postmaster;

    private DomainList domains;

    private HierarchicalConfiguration conf;

    @Resource(name = "James")
    public void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }

    @Resource(name = "dnsserver")
    public void setDNSService(DNSService dns) {
        this.dns = dns;
    }

    @Resource(name = "localusersrepository")
    public void setUsersRepository(UsersRepository localusers) {
        this.localusers = localusers;
    }

    @Resource(name = "domainlist")
    public void setDomainList(DomainList domains) {
        this.domains = domains;
    }

    @PostConstruct
    public void init() throws Exception {

        // used by RemoteDelivery for HELO
        attributes.put(Constants.DEFAULT_DOMAIN, mailServer.getDefaultDomain());

        initializeLocalDeliveryMailet();

        initPostmaster();
    }

    /**
     * @see org.apache.mailet.MailetContext#getMailServers(String)
     */
    public Collection<String> getMailServers(String host) {
        try {
            return dns.findMXRecords(host);
        } catch (TemporaryResolutionException e) {
            // TODO: We only do this to not break backward compatiblity. Should
            // fixed later
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
     * @see org.apache.mailet.MailetContext#setAttribute(java.lang.String,
     *      java.lang.Object)
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
        for (Enumeration e = attributes.keys(); e.hasMoreElements();) {
            names.add(e.nextElement().toString());
        }
        return names.iterator();
    }

    /**
     * This generates a response to the Return-Path address, or the address of
     * the message's sender if the Return-Path is not available. Note that this
     * is different than a mail-client's reply, which would use the Reply-To or
     * From header. This will send the bounce with the server's postmaster as
     * the sender.
     * 
     * @see org.apache.mailet.MailetContext#bounce(Mail, String)
     */
    public void bounce(Mail mail, String message) throws MessagingException {
        bounce(mail, message, getPostmaster());
    }

    /**
     * This generates a response to the Return-Path address, or the address of
     * the message's sender if the Return-Path is not available. Note that this
     * is different than a mail-client's reply, which would use the Reply-To or
     * From header.
     * 
     * Bounced messages are attached in their entirety (headers and content) and
     * the resulting MIME part type is "message/rfc822".
     * 
     * The attachment to the subject of the original message (or "No Subject" if
     * there is no subject in the original message)
     * 
     * There are outstanding issues with this implementation revolving around
     * handling of the return-path header.
     * 
     * MIME layout of the bounce message:
     * 
     * multipart (mixed)/ contentPartRoot (body) = mpContent (alternative)/ part
     * (body) = message part (body) = original
     * 
     * @see org.apache.mailet.MailetContext#bounce(Mail, String, MailAddress)
     */

    public void bounce(Mail mail, String message, MailAddress bouncer) throws MessagingException {
        if (mail.getSender() == null) {
            if (log.isInfoEnabled())
                log.info("Mail to be bounced contains a null (<>) reverse path.  No bounce will be sent.");
            return;
        } else {
            // Bounce message goes to the reverse path, not to the Reply-To
            // address
            if (log.isInfoEnabled())
                log.info("Processing a bounce request for a message with a reverse path of " + mail.getSender().toString());
        }

        MailImpl reply = rawBounce(mail, message);
        // Change the sender...
        reply.getMessage().setFrom(bouncer.toInternetAddress());
        reply.getMessage().saveChanges();
        // Send it off ... with null reverse-path
        reply.setSender(null);
        sendMail(reply);
        LifecycleUtil.dispose(reply);
    }

    /**
     * Generates a bounce mail that is a bounce of the original message.
     * 
     * @param bounceText
     *            the text to be prepended to the message to describe the bounce
     *            condition
     * 
     * @return the bounce mail
     * 
     * @throws MessagingException
     *             if the bounce mail could not be created
     */
    private MailImpl rawBounce(Mail mail, String bounceText) throws MessagingException {
        // This sends a message to the james component that is a bounce of the
        // sent message
        MimeMessage original = mail.getMessage();
        MimeMessage reply = (MimeMessage) original.reply(false);
        reply.setSubject("Re: " + original.getSubject());
        reply.setSentDate(new Date());
        Collection<MailAddress> recipients = new HashSet<MailAddress>();
        recipients.add(mail.getSender());
        InternetAddress addr[] = { new InternetAddress(mail.getSender().toString()) };
        reply.setRecipients(Message.RecipientType.TO, addr);
        reply.setFrom(new InternetAddress(mail.getRecipients().iterator().next().toString()));
        reply.setText(bounceText);
        reply.setHeader(RFC2822Headers.MESSAGE_ID, "replyTo-" + mail.getName());
        return new MailImpl("replyTo-" + mail.getName(), new MailAddress(mail.getRecipients().iterator().next().toString()), recipients, reply);
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
                return isLocalEmail(new MailAddress(name, "localhost"));
            } else {
                return isLocalEmail(new MailAddress(name));
            }
        } catch (ParseException e) {
            log("Error checking isLocalUser for user " + name);
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
        if (mailServer.supportVirtualHosting() == false) {
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
     * Performs DNS lookups as needed to find servers which should or might
     * support SMTP. Returns an Iterator over HostAddress, a specialized
     * subclass of javax.mail.URLName, which provides location information for
     * servers that are specified as mail handlers for the given hostname. This
     * is done using MX records, and the HostAddress instances are returned
     * sorted by MX priority. If no host is found for domainName, the Iterator
     * returned will be empty and the first call to hasNext() will return false.
     * 
     * @see org.apache.james.api.dnsservice.DNSService#getSMTPHostAddresses(String)
     * @since Mailet API v2.2.0a16-unstable
     * @param domainName
     *            - the domain for which to find mail servers
     * @return an Iterator over HostAddress instances, sorted by priority
     */
    public Iterator<HostAddress> getSMTPHostAddresses(String domainName) {
        try {
            return dns.getSMTPHostAddresses(domainName);
        } catch (TemporaryResolutionException e) {
            // TODO: We only do this to not break backward compatiblity. Should
            // fixed later
            return Collections.unmodifiableCollection(new ArrayList<HostAddress>(0)).iterator();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.mailet.MailetContext#getServerInfo()
     */
    public String getServerInfo() {
        return "Apache JAMES";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.mailet.MailetContext#isLocalServer(java.lang.String)
     */
    public boolean isLocalServer(String name) {
        return mailServer.isLocalServer(name);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.mailet.MailetContext#log(java.lang.String)
     */
    public void log(String arg0) {
        log.info(arg0);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.mailet.MailetContext#log(java.lang.String, java.lang.Throwable)
     */
    public void log(String arg0, Throwable arg1) {
        log.info(arg0, arg1);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.mailet.MailetContext#sendMail(javax.mail.internet.MimeMessage)
     */
    public void sendMail(MimeMessage message) throws MessagingException {
        mailServer.sendMail(message);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.mailet.MailetContext#sendMail(org.apache.mailet.Mail)
     */
    public void sendMail(Mail mail) throws MessagingException {
        mailServer.sendMail(mail);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.mailet.MailetContext#sendMail(org.apache.mailet.MailAddress, java.util.Collection, javax.mail.internet.MimeMessage)
     */
    public void sendMail(MailAddress sender, Collection recipients, MimeMessage msg) throws MessagingException {
        mailServer.sendMail(sender, recipients, msg);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.mailet.MailetContext#sendMail(org.apache.mailet.MailAddress, java.util.Collection, javax.mail.internet.MimeMessage, java.lang.String)
     */
    public void sendMail(MailAddress sender, Collection recipients, MimeMessage message, String state) throws MessagingException {
        MailImpl mail = new MailImpl(mailServer.getId(), sender, recipients, message);
        try {
            mail.setState(state);
            sendMail(mail);
        } finally {
            LifecycleUtil.dispose(mail);
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
     * This method has been moved to LocalDelivery (the only client of the
     * method). Now we can safely remove it from the Mailet API and from this
     * implementation of MailetContext.
     * 
     * The local field localDeliveryMailet will be removed when we remove the
     * storeMail method.
     * 
     * @deprecated since 2.2.0 look at the LocalDelivery code to find out how to
     *             do the local delivery.
     * @see org.apache.mailet.MailetContext#storeMail(org.apache.mailet.MailAddress,
     *      org.apache.mailet.MailAddress, javax.mail.internet.MimeMessage)
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
        MailImpl m = new MailImpl(mailServer.getId(), sender, recipients, msg);
        localDeliveryMailet.service(m);
        LifecycleUtil.dispose(m);
    }

    public void setLog(Log log) {
        this.log = log;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.apache.james.lifecycle.Configurable#configure(org.apache.commons.
     * configuration.HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration conf) throws ConfigurationException {
        this.conf = conf;
    }

    private void initPostmaster() throws Exception {
        // Get postmaster
        String postMasterAddress = conf.getString("postmaster", "postmaster").toLowerCase(Locale.US);
        // if there is no @domain part, then add the first one from the
        // list of supported domains that isn't localhost. If that
        // doesn't work, use the hostname, even if it is localhost.
        if (postMasterAddress.indexOf('@') < 0) {
            String domainName = null; // the domain to use
            // loop through candidate domains until we find one or exhaust the
            // list
            Iterator<String> i = domains.getDomains().iterator();
            while (i.hasNext()) {
                String serverName = i.next().toLowerCase(Locale.US);
                if (!("localhost".equals(serverName))) {
                    domainName = serverName; // ok, not localhost, so use it
                    continue;
                }
            }
            // if we found a suitable domain, use it. Otherwise fallback to the
            // host name.
            postMasterAddress = postMasterAddress + "@" + (domainName != null ? domainName : mailServer.getDefaultDomain());
        }
        this.postmaster = new MailAddress(postMasterAddress);

        if (!isLocalServer(postmaster.getDomain())) {
            StringBuffer warnBuffer = new StringBuffer(320).append("The specified postmaster address ( ").append(postmaster).append(
                    " ) is not a local address.  This is not necessarily a problem, but it does mean that emails addressed to the postmaster will be routed to another server.  For some configurations this may cause problems.");
            log.warn(warnBuffer.toString());
        }
    }
}
