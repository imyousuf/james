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

package org.apache.james.mailboxmanager.tracking;

import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.impl.MailboxEventDispatcher;

public class MailboxTracker {
    
    protected String mailboxName;
    
    private MailboxCache mailboxCache;
    
    protected MailboxEventDispatcher eventDispatcher=new MailboxEventDispatcher();
    
    private boolean existing;

    public MailboxTracker(MailboxCache mailboxCache, String mailboxName) {
        this.mailboxName=mailboxName;
        this.mailboxCache=mailboxCache;
    }

    public String getMailboxName() {
        return mailboxName;
    }

    public void signalDeletion() {
        eventDispatcher.mailboxDeleted();
        existing=false;
    }
    
    public void addMailboxListener(MailboxListener subject) {
        eventDispatcher.addMailboxListener(subject);
    }
    
    public void removeMailboxListener(MailboxListener subject) {
        eventDispatcher.removeMailboxListener(subject); 
        if (eventDispatcher.size()==0) {
            mailboxCache.unused(this);
        }
    }
    
    public void mailboxNotFound() {
        mailboxCache.notFound(getMailboxName());
        existing=false;
    }

    public boolean isExisting() {
        return existing;
    }

    public void signalRename(String newName) {
        eventDispatcher.mailboxRenamed(mailboxName, newName);
        mailboxName=newName;
    }
    
    public int getSessionCount() {
        return eventDispatcher.size();
    }

}
