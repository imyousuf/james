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



package org.apache.james.transport.mailets;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.user.api.UsersRepository;
import org.apache.mailet.base.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.MailetContext;

import javax.annotation.Resource;
import javax.mail.MessagingException;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Receives a Mail from JamesSpoolManager and takes care of delivery of the
 * message to local inboxes.
 * 
 * Since James 2.3.0 this mailet is a composition of
 * UserRepositoryAliasingForwarding and ToMultiRepository
 * configurated to mimic the old "LocalDelivery" behaviour.
 */
public class LocalDelivery extends GenericMailet {

    /**
     * Mailet that apply aliasing and forwarding
     */
    private UsersRepositoryAliasingForwarding aliasingMailet;

    /**
     * Mailet that actually store the message
     */
    private SieveMailet sieveMailet;
    
    private UsersRepository usersRepository;


    private MailboxManager mailboxManager;


    
    @Resource(name="usersrepository")
    public void setUsersRepository(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }
    
    
    

    @Resource(name="mailboxmanager")
    public void setMailboxManager(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }
    
    /**
     * Delivers a mail to a local mailbox.
     * 
     * @param mail
     *            the mail being processed
     * 
     * @throws MessagingException
     *             if an error occurs while storing the mail
     */
    public void service(Mail mail) throws MessagingException {
        aliasingMailet.service(mail);
        if (mail.getState() != Mail.GHOST) {
            sieveMailet.service(mail);
        }
    }

    /**
     * Return a string describing this mailet.
     * 
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Local Delivery Mailet";
    }

    /**
     * @see org.apache.mailet.GenericMailet#init()
     */
    public void init() throws MessagingException {
        super.init();
        
        aliasingMailet = new UsersRepositoryAliasingForwarding();
        aliasingMailet.setUsersRepository(usersRepository);
        aliasingMailet.init(getMailetConfig());
        sieveMailet = new SieveMailet();
        MailetConfig m = new MailetConfig() {

            /**
             * @see org.apache.mailet.MailetConfig#getInitParameter(java.lang.String)
             */
            public String getInitParameter(String name) {
            	return null;
            }

            /**
             * @see org.apache.mailet.MailetConfig#getInitParameterNames()
             */
            public Iterator<String> getInitParameterNames() {
            	return new ArrayList<String>().iterator();
            }

            /**
             * @see org.apache.mailet.MailetConfig#getMailetContext()
             */
            public MailetContext getMailetContext() {
                return getMailetConfig().getMailetContext();
            }

            /**
             * @see org.apache.mailet.MailetConfig#getMailetName()
             */
            public String getMailetName() {
                return getMailetConfig().getMailetName();
            }

        };
        sieveMailet.setUsersRepository(usersRepository);
        sieveMailet.setMailboxManager(mailboxManager);
        sieveMailet.init(m);

        sieveMailet.setQuiet(true);
    }

}
