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

package org.apache.james.mailboxmanager.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.ListResult;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.GeneralMailbox;
import org.apache.james.mailboxmanager.mailbox.GeneralMailboxSession;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.mailboxmanager.mailbox.MailboxSession;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerFactory;
import org.apache.james.services.User;

public class VirtualMailboxManager extends AbstractLogEnabled implements
        MailboxManager {

    private Map mountMap = null;
    
    private User user;
    
        
    
    public VirtualMailboxManager() {
        
    }

    Map getMountMap() {
        return mountMap;
    }
    
    public void setMountMap(Map mountMap) {
        this.mountMap=mountMap;
    }
    
    

    MailboxManager getMailboxManager(String mailboxName) throws MailboxManagerException {
        MailboxManager mailboxManager = null;
        Iterator it = getMountMap().entrySet().iterator();
        while (it.hasNext() && mailboxManager == null) {
            Entry entry = (Entry) it.next();
            String key = (String) entry.getKey();
            if (mailboxName.startsWith(key)) {
                MailboxManagerFactory mailboxManagerFactory = (MailboxManagerFactory) entry.getValue();
                mailboxManager=mailboxManagerFactory.getMailboxManagerInstance(user);
            }
        }
        return mailboxManager;
    }

    public void addMountPoint(String attribute) {
    }

    public void close() {
    }

    public void copyMessages(GeneralMailbox from, GeneralMessageSet set,
            String to) throws MailboxManagerException {
        getMailboxManager(to).copyMessages(from, set, to);
    }

    public void createMailbox(String mailboxName)
            throws MailboxManagerException {
        getMailboxManager(mailboxName).createMailbox(mailboxName);

    }

    public void deleteMailbox(String mailboxName)
            throws MailboxManagerException {
        getMailboxManager(mailboxName).deleteMailbox(mailboxName);

    }

    public boolean existsMailbox(String mailboxName)
            throws MailboxManagerException {
        return getMailboxManager(mailboxName).existsMailbox(mailboxName);
    }

    public GeneralMailboxSession getGeneralMailboxSession(String mailboxName)
            throws MailboxManagerException {
        return getMailboxManager(mailboxName).getGeneralMailboxSession(
                mailboxName);
    }

    public ImapMailboxSession getImapMailboxSession(String mailboxName)
            throws MailboxManagerException {
        return getMailboxManager(mailboxName)
                .getImapMailboxSession(mailboxName);
    }

    public MailboxSession getMailboxSession(String mailboxName)
            throws MailboxManagerException {
        return getMailboxManager(mailboxName).getMailboxSession(mailboxName);
    }

    public ListResult[] list(String base, String expression, boolean subscribed)
            throws MailboxManagerException {
        // TODO call only base matching managers
        List listResults=new ArrayList();
        Iterator it = getMountMap().entrySet().iterator();
        
        while (it.hasNext()) {
            Entry entry = (Entry) it.next();
            MailboxManagerFactory mailboxManagerFactory = (MailboxManagerFactory) entry.getValue();
            MailboxManager mailboxManager=mailboxManagerFactory.getMailboxManagerInstance(user);
            ListResult[] thisListResults=mailboxManager.list(base, expression, subscribed);
            for (int i = 0; i < thisListResults.length; i++) {
                listResults.add(thisListResults[i]);
            }
        }
        
        return (ListResult[]) listResults.toArray(new ListResult[0]);
    }

    public void renameMailbox(String from, String to)
            throws MailboxManagerException {
        // TODO deal with implementation spanning renames
        getMailboxManager(from).renameMailbox(from, to);
    }

    public void setSubscription(String mailboxName, boolean value)
            throws MailboxManagerException {
        getMailboxManager(mailboxName).setSubscription(mailboxName, value);
    }

    public void setUser(User user) {
        this.user=user;
    }

}
