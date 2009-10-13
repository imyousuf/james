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

package org.apache.james.transport.remotedeliverytester;

import org.apache.avalon.cornerstone.services.store.Store;
import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.Constants;
import org.apache.james.services.SpoolRepository;
import org.apache.mailet.base.test.FakeMailContext;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MailetContext;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;

public class TesterMailetConfig implements MailetConfig {

    private class MailetContextTester extends FakeMailContext {
        private TesterMailetConfig owner;
        private ServiceManager serviceManager;
        private ServiceManager wrappedServiceManager;
        
        public MailetContextTester(TesterMailetConfig owner, ServiceManager serviceManager) {
            this.owner = owner;
            this.serviceManager = serviceManager;
        }

        public Object getAttribute(String name) {
            if (name.equals(Constants.AVALON_COMPONENT_MANAGER)) {
                if (wrappedServiceManager == null) wrappedServiceManager = new ServiceManagerWrapper(owner, serviceManager);
                return wrappedServiceManager;
            } else if (name.equals(Constants.HELLO_NAME)) {
                return "hello.name.com";
            }
            return null;
        }

        public MailetContext setServiceManager(ServiceManager serviceManager) {
            this.serviceManager = serviceManager;
            return this;
        }

        public void log(String message, Throwable t) {
            log(message);
            t.printStackTrace();
        }

        public void log(String message) {
            System.out.println(message);
        }

        public Iterator getSMTPHostAddresses(String domainName) {
            return owner.onMailetContextGetSMTPHostAddresses(domainName);
        }

        public void bounce(Mail mail, String message, MailAddress bouncer) throws MessagingException {
            throw new UnsupportedOperationException();
        }

        public void bounce(Mail mail, String message) throws MessagingException {
            owner.onMailetContextBounce(mail, message);
        }

        public Collection getMailServers(String host) {
            throw new UnsupportedOperationException();
        }

        public void sendMail(Mail mail) throws MessagingException {
            owner.onMailetContextSendMail(mail);
        }

        public void sendMail(MailAddress sender, Collection recipients, MimeMessage msg, String state) throws MessagingException {
            throw new UnsupportedOperationException();
        }

        public void sendMail(MailAddress sender, Collection recipients, MimeMessage msg) throws MessagingException {
            throw new UnsupportedOperationException();
        }

        public void sendMail(MimeMessage msg) throws MessagingException {
            throw new UnsupportedOperationException();
        }

        public void storeMail(MailAddress sender, MailAddress recipient, MimeMessage msg) throws MessagingException {
            throw new UnsupportedOperationException();
        }

    }

    private class ServiceManagerWrapper implements ServiceManager {
        private TesterMailetConfig owner;
        private ServiceManager wrapped;
        private Store wrappedStore;

        public ServiceManagerWrapper(TesterMailetConfig owner, ServiceManager wrapped) {
            this.owner = owner;
            this.wrapped = wrapped;
        }

        public Object lookup(String arg0) throws ServiceException {
            if (arg0.equals(Store.ROLE)) {
                if (wrappedStore == null) wrappedStore = new StoreWrapper(owner, (Store) wrapped.lookup("org.apache.avalon.cornerstone.services.store.Store"));
                return wrappedStore;
            }
            return wrapped.lookup(arg0);
        }

        public boolean hasService(String arg0) {
            return wrapped.hasService(arg0);
        }

        public void release(Object arg0) {
            wrapped.release(arg0);
        }
    }

    private class StoreWrapper implements Store {
        private TesterMailetConfig owner;
        private Store wrapped;
        private SpoolRepository wrappedSpoolRepository;

        public StoreWrapper(TesterMailetConfig owner, Store wrapped) {
            this.owner = owner;
            this.wrapped = wrapped;
        }

        public Object select(Object arg0) throws ServiceException {
            if ((arg0 instanceof Configuration) && ((Configuration) arg0).getLocation().equals("generated:RemoteDelivery.java")) {
                if (wrappedSpoolRepository == null) wrappedSpoolRepository = new SpoolRepositoryWrapper(owner, (SpoolRepository) wrapped.select(arg0));
                return wrappedSpoolRepository;
            }
            return wrapped.select(arg0);
        }

        public boolean isSelectable(Object arg0) {
            return wrapped.isSelectable(arg0);
        }

        public void release(Object arg0) {
            wrapped.release(arg0);
        }
    }

    private class SpoolRepositoryWrapper implements SpoolRepository {
        private TesterMailetConfig owner;
        private SpoolRepository wrapped;

        public SpoolRepositoryWrapper(TesterMailetConfig owner, SpoolRepository wrapped) {
            this.owner = owner;
            this.wrapped = wrapped;
            owner.setWrappedSpoolRepository(this);
        }

        public void store(Mail mc) throws MessagingException {
            owner.onOutgoingStore(mc);
            wrapped.store(mc);
        }

        public void remove(Mail mail) throws MessagingException {
            owner.onOutgoingRemove(mail);
            wrapped.remove(mail);
        }

        public void remove(String key) throws MessagingException {
            owner.onOutgoingRemove(key);
            wrapped.remove(key);
        }

        public Mail accept(AcceptFilter filter) throws InterruptedException {
            Mail mail = wrapped.accept(filter);
            if (mail != null) owner.onOutgoingAccept(mail);
            return mail;
        }

        public Mail accept() throws InterruptedException {
            return wrapped.accept();
        }

        public Mail accept(long delay) throws InterruptedException {
            return wrapped.accept(delay);
        }

        public Iterator list() throws MessagingException {
            return wrapped.list();
        }

        public boolean lock(String key) throws MessagingException {
            return wrapped.lock(key);
        }

        public void remove(Collection mails) throws MessagingException {
            wrapped.remove(mails);
        }

        public Mail retrieve(String key) throws MessagingException {
            return wrapped.retrieve(key);
        }

        public boolean unlock(String key) throws MessagingException {
            return wrapped.unlock(key);
        }
    }

    private Tester owner;
    private Properties parameters;
    private MailetContextTester mailetContext;
    
    // wrappedSpoolRepository will be set only when this manage the outgoing
    private SpoolRepository wrappedSpoolRepository;

    public TesterMailetConfig(Tester owner, Properties properties, ServiceManager serviceManager) {
        this.owner = owner;
        this.parameters = properties;
        mailetContext = new MailetContextTester(this, serviceManager);
    }

    public SpoolRepository getWrappedSpoolRepository() {
        return wrappedSpoolRepository;
    }

    public void setWrappedSpoolRepository(SpoolRepository wrappedSpoolRepository) {
        this.wrappedSpoolRepository = wrappedSpoolRepository;
    }

    public String getInitParameter(String name) {
        return (String) parameters.get(name);
    }

    public Iterator getInitParameterNames() {
        return parameters.keySet().iterator();
    }

    public MailetContext getMailetContext() {
        return mailetContext;
    }

    public String getMailetName() {
        return "Test";
    }

    public Iterator onMailetContextGetSMTPHostAddresses(String domainName) {
        return owner.onMailetContextGetSMTPHostAddresses(domainName);
    }
    
    public void onMailetContextBounce(Mail mail, String message) {
        owner.onMailetContextBounce(mail, message);
    }

    public void onMailetContextSendMail(Mail mail) {
        owner.onMailetContextSendMail(mail);
    }

    public void onOutgoingAccept(Mail mail) {
        owner.onOutgoingAccept(mail);
    }

    public void onOutgoingRemove(String key) {
        owner.onOutgoingRemove(key);
    }

    public void onOutgoingRemove(Mail mail) {
        owner.onOutgoingRemove(mail);
    }

    public void onOutgoingStore(Mail mc) {
        owner.onOutgoingStore(mc);
    }
}
