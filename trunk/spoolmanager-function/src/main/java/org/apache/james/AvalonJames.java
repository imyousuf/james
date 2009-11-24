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

import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.activity.Initializable;
import org.apache.avalon.framework.configuration.Configurable;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.logger.LogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.avalon.framework.service.Serviceable;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.api.dnsservice.DNSService;
import org.apache.james.api.domainlist.DomainList;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.bridge.GuiceInjected;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailRepository;
import org.apache.james.services.MailServer;
import org.apache.james.services.SpoolRepository;
import org.apache.james.util.ConfigurationAdapter;
import org.apache.mailet.HostAddress;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetContext;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonJames implements MailServer, MailetContext, Serviceable, Configurable, Initializable, GuiceInjected, LogEnabled{

    private ConfigurationAdapter config;
    private Log logger;
    private Log mailetLogger;
    private James james;
    private DNSService dns;
    private DomainList domainList;
    private Store store;
    private FileSystem fs;
    private SpoolRepository spoolRepos;
    private UsersRepository userRepos;

    public boolean addUser(String userName, String password) {
        return james.addUser(userName, password);
    }

    public String getDefaultDomain() {
        return james.getDefaultDomain();
    }

    public String getHelloName() {
        return james.getHelloName();
    }

    public String getId() {
        return james.getId();
    }

    public MailRepository getUserInbox(String userName) {
        return james.getUserInbox(userName);
    }

    public boolean isLocalServer(String serverName) {
        return james.isLocalServer(serverName);
    }

    public void sendMail(MailAddress sender, Collection recipients, MimeMessage msg) throws MessagingException {
        james.sendMail(sender,recipients, msg);
    }

    public void sendMail(MailAddress sender, Collection<MailAddress> recipients, InputStream msg) throws MessagingException {
        james.sendMail(sender,recipients,msg);
    }

    public void sendMail(Mail mail) throws MessagingException {
        james.sendMail(mail);
    }

    public void sendMail(MimeMessage message) throws MessagingException {        
        james.sendMail(message);
    }

    public boolean supportVirtualHosting() {
        return james.supportVirtualHosting();
    }

    public void bounce(Mail arg0, String arg1) throws MessagingException {
        james.bounce(arg0, arg1);
    }

    public void bounce(Mail mail, String message, MailAddress bouncer) throws MessagingException {
        james.bounce(mail, message, bouncer);
    }

    public Object getAttribute(String key) {
        return james.getAttribute(key);
    }

    public Iterator<String> getAttributeNames() {
        return james.getAttributeNames();
    }

    public Collection<String> getMailServers(String arg0) {
        return james.getMailServers(arg0);
    }

    public int getMajorVersion() {
        return james.getMajorVersion();
    }

    public int getMinorVersion() {
        return james.getMinorVersion();
    }

    public MailAddress getPostmaster() {
        return james.getPostmaster();
    }

    public Iterator<HostAddress> getSMTPHostAddresses(String arg0) {
        return james.getSMTPHostAddresses(arg0);
    }

    public String getServerInfo() {
        return james.getServerInfo();
    }

    public boolean isLocalEmail(MailAddress mailAddress) {
        return james.isLocalEmail(mailAddress);
    }

    public boolean isLocalUser(String name) {
        return james.isLocalUser(name);
    }

    public void log(String arg0) {
        james.log(arg0);
    }

    public void log(String message, Throwable t) {
        james.log(message, t);
    }

    public void removeAttribute(String arg0) {
        james.removeAttribute(arg0);
    }

    public void sendMail(MailAddress sender, Collection recipients, MimeMessage message, String state) throws MessagingException {
        james.sendMail(sender, recipients, message, state);
    }

    public void setAttribute(String arg0, Object arg1) {
        james.setAttribute(arg0, arg1);
        
    }

    @SuppressWarnings("deprecation")
    public void storeMail(MailAddress sender, MailAddress recipient, MimeMessage msg) throws MessagingException {
        james.storeMail(sender, recipient, msg);
    }

    public void service(ServiceManager manager) throws ServiceException {
        dns = (DNSService) manager.lookup(DNSService.ROLE);
        domainList = (DomainList) manager.lookup(DomainList.ROLE);
        store = (Store) manager.lookup(Store.ROLE);
        fs = (FileSystem) manager.lookup(FileSystem.ROLE);
        spoolRepos = (SpoolRepository) manager.lookup(SpoolRepository.ROLE);
        userRepos = (UsersRepository) manager.lookup(UsersRepository.ROLE);
    }


    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(org.apache.avalon.framework.configuration.Configuration)
     */
    public void configure(Configuration config) throws ConfigurationException {
        try {
            this.config = new ConfigurationAdapter(config);
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new ConfigurationException("Unable to convert configuration", e);
        }
    }


    /**
     * @see org.apache.avalon.framework.logger.LogEnabled#enableLogging(org.apache.avalon.framework.logger.Logger)
     */
    public void enableLogging(Logger logger) {
        this.logger = new AvalonLogger(logger);
        this.mailetLogger = new AvalonLogger(logger.getChildLogger("Mailet"));
    }

    
    public void initialize() throws Exception {
        james = Guice.createInjector(new Jsr250Module(), new AbstractModule() {

            @Override
            protected void configure() {
                bind(DNSService.class).annotatedWith(Names.named("org.apache.james.api.dnsservice.DNSService")).toInstance(dns);
                bind(org.apache.commons.configuration.HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(config);
                bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
                bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log@MailetLog")).toInstance(mailetLogger);
                bind(Store.class).annotatedWith(Names.named("org.apache.avalon.cornerstone.services.store.Store")).toInstance(store);
                bind(FileSystem.class).annotatedWith(Names.named("org.apache.james.services.FileSystem")).toInstance(fs);
                bind(UsersRepository.class).annotatedWith(Names.named("org.apache.james.api.user.UsersRepository")).toInstance(userRepos);
                bind(SpoolRepository.class).annotatedWith(Names.named("org.apache.james.services.SpoolRepository")).toInstance(spoolRepos);
                bind(DomainList.class).annotatedWith(Names.named("org.apache.james.api.domainlist.DomainList")).toInstance(domainList);
            }
            
        }).getInstance(James.class);
    }

}
