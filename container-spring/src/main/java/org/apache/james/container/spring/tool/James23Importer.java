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
package org.apache.james.container.spring.tool;

import java.util.Iterator;

import javax.annotation.Resource;
import javax.mail.MessagingException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.commons.logging.Log;
import org.apache.james.lifecycle.api.Configurable;
import org.apache.james.lifecycle.api.LogEnabled;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailrepository.api.MailRepository;
import org.apache.james.mailrepository.api.MailRepositoryStore;
import org.apache.james.mailrepository.api.MailRepositoryStore.MailRepostoryStoreException;
import org.apache.james.user.api.UsersRepository;
import org.apache.mailet.Mail;

public class James23Importer implements Configurable, LogEnabled {
    
    /**
     * The logger.
     */
    private Log log;
    
    /**
     * The configuration.
     */
    private HierarchicalConfiguration config;

    /**
     * James 2.3 user repository defined by configuration.
     */
    @Resource(name = "importjames23-usersrepository")
    private UsersRepository james23UsersRepository;
    
    /**
     * James 3.0 users repository.
     */
    @Resource(name="usersrepository")
    private UsersRepository usersRepository;

    /**
     * The mail repository store needed to select the james 2.3 mail repository.
     */
    @Resource(name="mailrepositorystore")
    private MailRepositoryStore mailRepositoryStore;

    /**
     * The mailbox manager needed to copy the mails to.
     */
    @Resource(name="mailboxmanager")
    private MailboxManager mailboxManager;

    /**
     * Copy 2.3 users to 3.0 users (taking virtualDomains into account)
     * Copy 2.3 mails to 3.0 mails.
     * 
     * TODO: This is just a skeleton to talk about the architecture (conf,...)
     * 
     * @throws MailRepostoryStoreException
     * @throws MessagingException
     */
    public void importFromJames23() throws MailRepostoryStoreException, MessagingException {
        
        String james23MailRepositoryPath = config.getString("repositoryPath");
        
        Iterator<String> j23uIt = james23UsersRepository.list();
        
        while (j23uIt.hasNext()) {
            String user = j23uIt.next();
            System.out.println("James 2.3 user:" + user);
            MailRepository mailRepository = mailRepositoryStore.select(james23MailRepositoryPath + "/" + user);
            Iterator<String> sr = mailRepository.list();
            while (sr.hasNext()) {
                Mail mail = mailRepository.retrieve(sr.next());
                System.out.println(mail.getMessage().getSubject() 
                        + ": " + mail.getMessage().getSize() 
                        + " - " + mail.getMessage().getLineCount());
            }
        }
        
    }

    /* (non-Javadoc)
     * @see org.apache.james.lifecycle.api.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.log = log;
    }

    /* (non-Javadoc)
     * @see org.apache.james.lifecycle.api.Configurable#configure(org.apache.commons.configuration.HierarchicalConfiguration)
     */
    public void configure(HierarchicalConfiguration config) throws ConfigurationException {
        this.config = config;
    }
    
}
