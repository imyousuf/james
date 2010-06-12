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

import org.apache.james.services.SpoolRepository;
import org.apache.mailet.base.test.FakeMailContext;
import org.apache.mailet.HostAddress;
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
        
        public MailetContextTester(TesterMailetConfig owner) {
            this.owner = owner;
        }

        public Object getAttribute(String name) {
            return null;
        }

        public void log(String message, Throwable t) {
            log(message);
            t.printStackTrace();
        }

        public void log(String message) {
            System.out.println(message);
        }

        public Iterator<HostAddress> getSMTPHostAddresses(String domainName) {
            return owner.onMailetContextGetSMTPHostAddresses(domainName);
        }

        public void bounce(Mail mail, String message, MailAddress bouncer) throws MessagingException {
            throw new UnsupportedOperationException();
        }

        public void bounce(Mail mail, String message) throws MessagingException {
            owner.onMailetContextBounce(mail, message);
        }

        public Collection<String> getMailServers(String host) {
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


    private Tester owner;
    private Properties parameters;
    private MailetContextTester mailetContext;
    
    // wrappedSpoolRepository will be set only when this manage the outgoing
    private SpoolRepository wrappedSpoolRepository;

    public TesterMailetConfig(Tester owner, Properties properties) {
        this.owner = owner;
        this.parameters = properties;
        mailetContext = new MailetContextTester(this);
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
