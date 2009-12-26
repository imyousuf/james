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


package org.apache.james.fetchmail;

import org.apache.avalon.cornerstone.services.scheduler.TimeScheduler;
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
import org.apache.james.bridge.GuiceInjected;
import org.apache.james.services.MailServer;
import org.apache.james.util.ConfigurationAdapter;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonFetchScheduler implements Serviceable, Configurable, Initializable, GuiceInjected, LogEnabled, FetchSchedulerMBean{

    private Log logger;
    private DNSService dns;
    private MailServer mailserver;
    private UsersRepository userRepos;
    private ConfigurationAdapter config;
    private FetchScheduler scheduler;
    private TimeScheduler tscheduler;

    public void service(ServiceManager manager) throws ServiceException {
        dns = (DNSService) manager.lookup(DNSService.ROLE);
        mailserver = (MailServer) manager.lookup(MailServer.ROLE);
        tscheduler = (TimeScheduler) manager.lookup(TimeScheduler.ROLE);
        userRepos = (UsersRepository) manager.lookup(UsersRepository.ROLE);
    }

    public void configure(Configuration config) throws ConfigurationException {
        try {
            this.config = new ConfigurationAdapter(config);
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new ConfigurationException("Unable to convert configuration", e);
        }
    }


    public void initialize() throws Exception {
        scheduler = Guice.createInjector(new Jsr250Module(), new AbstractModule() {

            @Override
            protected void configure() {
                bind(DNSService.class).annotatedWith(Names.named("dnsserver")).toInstance(dns);
                bind(org.apache.commons.configuration.HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(config);
                bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
                bind(TimeScheduler.class).annotatedWith(Names.named("scheduler")).toInstance(tscheduler);
                bind(MailServer.class).annotatedWith(Names.named("James")).toInstance(mailserver);
                bind(UsersRepository.class).annotatedWith(Names.named("localusersrepository")).toInstance(userRepos);
            }
            
        }).getInstance(FetchScheduler.class);
    }

 

    /**
     * @see org.apache.avalon.framework.logger.LogEnabled#enableLogging(org.apache.avalon.framework.logger.Logger)
     */
    public void enableLogging(Logger logger) {
        this.logger = new AvalonLogger(logger);
    }


    public boolean isEnabled() {
        return scheduler.isEnabled();
    }

}
