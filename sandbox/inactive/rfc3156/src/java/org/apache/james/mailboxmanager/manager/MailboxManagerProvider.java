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

package org.apache.james.mailboxmanager.manager;

import java.util.Map;

import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.Namespace;
import org.apache.james.mailboxmanager.Namespaces;
import org.apache.james.mailboxmanager.mailbox.MailboxSession;
import org.apache.james.services.User;

public interface MailboxManagerProvider {
    
    public MailboxManager getMailboxManagerInstance(User user) throws MailboxManagerException;
 

    /**
     * <b>WARNING</b> this really deletes everything. Useful for testing
     * 
     * @throws MailboxManagerException
     */
    public void deleteEverything() throws MailboxManagerException;
    
    MailboxSession getInboxSession(User user) throws MailboxManagerException;

    /** 
     *  @param authUser the authorized User for checking credentials 
     *  @param mailboxName a logical/hierarchical mailbox name *
     *  @throws MailboxManagerException 
     */ 

    MailboxSession getMailboxSession(User authUser, String mailboxName,
            boolean autoCreate) throws MailboxManagerException;
    
    /**
     * removes all data (mailboxes, quota, acls...) that is associated 
     * with this user.
     * 
     * @param authUser the authorized User for checking credentials 
     * @param targetUser the user whos data will be deleted
     */
    
    void deleteAllUserData(User authUser,User targetUser);

    /**
     * The Namespaces a user has access to.
     * @param forUser TODO
     * @param user
     * 
     * @return
     */
    Namespaces getNamespaces(User forUser);

    /**
     * To get the Inbox you can just to a mailbox
     * defaultNameSpace=ImapMailboxRepository.getPersonalDefaultNameSpace(user)
     * inbox=defaultNameSpace.getName()+defaultNameSpace.getHierarchyDelimter()+"INBOX";
     * TODO add a convinience method to get directly a session mailbox for a users inbox
     * @param forUser TODO
     * 
     * @return
     */
    Namespace getPersonalDefaultNamespace(User forUser);

    /**
     * key: <b>String</b> - mailbox name <br />
     * value: <b>Integer</b> - count of open sessions <br />
     * <br />
     * useful for testing
     * @return Map of mailbox name/open session count
     */
    
    Map getOpenMailboxSessionCountMap();
}
