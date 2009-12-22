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
package org.apache.james.imapserver;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.cornerstone.services.sockets.SocketManager;
import org.apache.avalon.cornerstone.services.threads.ThreadManager;
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
import org.apache.james.api.user.UsersRepository;
import org.apache.james.services.FileSystem;
import org.apache.james.services.MailServer;
import org.apache.james.socket.AvalonProtocolServer;
import org.apache.james.socket.JamesConnectionManager;
import org.apache.james.socket.api.ProtocolHandlerFactory;
import org.apache.james.socket.api.ProtocolServer;
import org.apache.james.util.ConfigurationAdapter;
import org.apache.james.bridge.GuiceInjected;
import org.apache.jsieve.mailet.Poster;

import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.google.inject.name.Names;

public class AvalonImapServer implements GuiceInjected, Poster, Initializable, Serviceable, Configurable, LogEnabled {
    private FileSystem filesystem;
    private MailServer mailserver;
    private DNSService dns;
    private Log logger;
    private org.apache.commons.configuration.HierarchicalConfiguration config;
    private UsersRepository userRepos;
    private JamesConnectionManager connectionManager;
    private SocketManager socketManager;
    private Poster poster;
    private ThreadManager threadManager;


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
     * @see org.apache.avalon.framework.service.Serviceable#service(org.apache.avalon.framework.service.ServiceManager)
     */
    public void service(ServiceManager manager) throws ServiceException {
        dns = (DNSService) manager.lookup(DNSService.ROLE);
        mailserver = (MailServer) manager.lookup(MailServer.ROLE);
        filesystem = (FileSystem) manager.lookup(FileSystem.ROLE);
        userRepos = (UsersRepository) manager.lookup(UsersRepository.ROLE);
        socketManager = (SocketManager) manager.lookup(SocketManager.ROLE);
        connectionManager = (JamesConnectionManager) manager.lookup(JamesConnectionManager.ROLE);     
        threadManager = (ThreadManager) manager.lookup(ThreadManager.ROLE);
    }

    /**
     * @see org.apache.avalon.framework.activity.Initializable#initialize()
     */
    public void initialize() throws Exception {
        Injector injector = Guice.createInjector(new IMAPServerModule(), new Jsr250Module());
        injector.getInstance(AvalonProtocolServer.class);
        poster = injector.getInstance(Poster.class);
        //poster = injector.getInstance(ImapServerProtocolHandlerFactory.class);
    }
                 
    /**
     * @see org.apache.avalon.framework.logger.LogEnabled#enableLogging(org.apache.avalon.framework.logger.Logger)
     */
    public void enableLogging(Logger logger) {
        this.logger = new AvalonLogger(logger);
    }
    
    private final class IMAPServerModule extends AbstractModule {

        @Override
        protected void configure() {
            bind(DNSService.class).annotatedWith(Names.named("dnsserver")).toInstance(dns);
            bind(MailServer.class).annotatedWith(Names.named("James")).toInstance(mailserver);
            bind(org.apache.commons.configuration.HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(config);
            bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
            bind(FileSystem.class).annotatedWith(Names.named("filesystem")).toInstance(filesystem);
            bind(UsersRepository.class).annotatedWith(Names.named("localusersrepository")).toInstance(userRepos);
            bind(ImapServerProtocolHandlerFactory.class).in(Singleton.class);
            bind(Poster.class).to(ImapServerProtocolHandlerFactory.class);
            bind(ProtocolHandlerFactory.class).annotatedWith(Names.named("org.apache.james.socket.api.ProtocolHandlerFactory")).to(ImapServerProtocolHandlerFactory.class);
            bind(ProtocolServer.class).annotatedWith(Names.named("org.apache.james.socket.api.ProtocolServer")).to(AvalonProtocolServer.class).in(Singleton.class);
            bind(SocketManager.class).annotatedWith(Names.named("sockets")).toInstance(socketManager);
            bind(JamesConnectionManager.class).annotatedWith(Names.named("connections")).toInstance(connectionManager);
            bind(ThreadManager.class).annotatedWith(Names.named("thread-manager")).toInstance(threadManager);
           
        }
    }

    public void post(String url, MimeMessage mail) throws MessagingException {
        poster.post(url, mail);
    }

}
