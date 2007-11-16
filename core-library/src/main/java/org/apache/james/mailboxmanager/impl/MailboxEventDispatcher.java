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

import java.util.Iterator;
import java.util.Set;

import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MessageResult;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

public class MailboxEventDispatcher implements MailboxListener {

    private final Set listeners = new CopyOnWriteArraySet();

    public void addMailboxListener(MailboxListener mailboxListener) {
        listeners.add(mailboxListener);
    }

    public void removeMailboxListener(MailboxListener mailboxListener) {
        listeners.remove(mailboxListener);
    }

    public void added(MessageResult result) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            MailboxListener mailboxListener = (MailboxListener) iter.next();
            mailboxListener.added(result);
        }
    }

    public void expunged(MessageResult mr) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            MailboxListener mailboxListener = (MailboxListener) iter.next();
            mailboxListener.expunged(mr);
        }
    }

    public void flagsUpdated(MessageResult result, MailboxListener silentListener) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            MailboxListener mailboxListener = (MailboxListener) iter.next();
            mailboxListener.flagsUpdated(result, silentListener);
        }
    }

    public void mailboxDeleted() {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            MailboxListener mailboxListener = (MailboxListener) iter.next();
            mailboxListener.mailboxDeleted();
        }
    }

    public void mailboxRenamed(String origName, String newName) {
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            MailboxListener mailboxListener = (MailboxListener) iter.next();
            mailboxListener.mailboxRenamed(origName,origName);
        }
    }
    
    public int size() {
        return listeners.size();
    }

}
