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

package org.apache.james.mailboxmanager.repository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MailImpl;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.MailboxSession;
import org.apache.james.mailboxmanager.mailstore.MailStoreMailboxManager;
import org.apache.james.mailboxmanager.mailstore.MailstoreMailboxCache;
import org.apache.james.services.MailRepository;
import org.apache.mailet.Mail;

public class MailRepositoryMailboxSession implements MailboxSession {

    private MailRepository target;

    private MailstoreMailboxCache cache;

    private String name;


    public MailRepositoryMailboxSession(MailstoreMailboxCache cache,
            MailRepository target, String name) {
        this.cache = cache;
        this.target = target;
        this.name = name;
    }

    public void close() throws MailboxManagerException {
        cache.releaseSession(this);
        target=null;
    }

    public boolean isWriteable() {
        return true;
    }

    public int getMessageCount() throws MailboxManagerException {
        return list().size();
    }

    public String getName() throws MailboxManagerException {
        return name;
    }

    public Collection list() throws MailboxManagerException {
        try {
            ArrayList list = new ArrayList();
            for (Iterator iter = target.list(); iter.hasNext();) {
                list.add(iter.next());
            }
            return list;
        } catch (MessagingException e) {
            throw new MailboxManagerException(e);
        }
    }

    public void remove(String key) throws MailboxManagerException {
        try {
            target.remove(key);
        } catch (MessagingException e) {
            throw new MailboxManagerException(e);
        }
    }

    public MimeMessage retrieve(String key) throws MailboxManagerException {
        try {
            return target.retrieve(key).getMessage();
        } catch (MessagingException e) {
            throw new MailboxManagerException(e);
        }
    }

    public String store(MimeMessage message) throws MailboxManagerException {
        try {
            Mail mail=new MailImpl(message);
            target.store(mail);
            return mail.getName();
        } catch (MessagingException e) {
            throw new MailboxManagerException(e);
        }
        
    }

    public String update(String key, MimeMessage message)
            throws MailboxManagerException {
        try {
            Mail mail=new MailImpl(key,null,null,message);
            target.store(mail);
            return key;
        } catch (MessagingException e) {
            throw new MailboxManagerException(e);
        }
    }
    
}
