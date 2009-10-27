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
package org.apache.james.mailrepository;

import java.util.Collection;
import java.util.Iterator;

import javax.mail.MessagingException;

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
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.bridge.GuiceInjected;
import org.apache.james.services.SpoolRepository;
import org.apache.james.util.ConfigurationAdapter;
import org.apache.mailet.Mail;
import org.guiceyfruit.jsr250.Jsr250Module;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.name.Names;

public class AvalonMailStoreRepository implements GuiceInjected, Serviceable, Configurable, Initializable, LogEnabled, SpoolRepository{

    
    private Store store;
    private ConfigurationAdapter config;
    private AvalonLogger logger;
    private SpoolRepository repos;
    
    public void service(ServiceManager manager) throws ServiceException {
        store = (Store) manager.lookup(Store.ROLE);
       
    }
    
    public void configure(Configuration arg0) throws ConfigurationException {
        try {
            this.config = new ConfigurationAdapter(arg0);
        } catch (org.apache.commons.configuration.ConfigurationException e) {
            throw new ConfigurationException("Unable to convert config", e);
        }
    }

    public void initialize() throws Exception {
        repos = Guice.createInjector(new Jsr250Module(), new AbstractModule() {
            
            @Override
            protected void configure() {
                bind(Log.class).annotatedWith(Names.named("org.apache.commons.logging.Log")).toInstance(logger);
                bind(HierarchicalConfiguration.class).annotatedWith(Names.named("org.apache.commons.configuration.Configuration")).toInstance(config);
                bind(Store.class).annotatedWith(Names.named("org.apache.avalon.cornerstone.services.store.Store")).toInstance(store);
            }
        }).getInstance(MailStoreSpoolRepository.class);
    }

    public void enableLogging(Logger arg0) {
        this.logger = new AvalonLogger(arg0);
    }


    public Mail accept() throws InterruptedException {
        return repos.accept();
    }

    public Mail accept(long delay) throws InterruptedException {
        return repos.accept(delay);
    }

    public Mail accept(AcceptFilter filter) throws InterruptedException {
        return repos.accept(filter);
    }

    public Iterator<String> list() throws MessagingException {
        return repos.list();
    }

    public boolean lock(String key) throws MessagingException {
        return repos.lock(key);
    }

    public void remove(Mail mail) throws MessagingException {
        repos.remove(mail);
    }

    public void remove(Collection<Mail> mails) throws MessagingException {
        repos.remove(mails);
    }

    public void remove(String key) throws MessagingException {
        repos.remove(key);
    }

    public Mail retrieve(String key) throws MessagingException {
        return repos.retrieve(key);
    }

    public void store(Mail mc) throws MessagingException {
        repos.store(mc);
        
    }

    public boolean unlock(String key) throws MessagingException {
        return repos.unlock(key);
    }
}
