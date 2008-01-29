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

package org.apache.james.imapserver.processor.imap4rev1;

import java.util.Collection;

/**
 * Processes IMAP subscriptions.
 */
public interface IMAPSubscriber {

    /**
     * Subscribes the user to the given mailbox.
     * @param user the user name, not null
     * @param mailbox the mailbox name, not null
     */
    public void subscribe(String user, String mailbox) throws SubscriptionException;
    
    /**
     * Unsubscribes the user from the given mailbox.
     * @param user the user name, not null
     * @param mailbox the mailbox name, not null
     */
    public void unsubscribe(String user, String mailbox) throws SubscriptionException;
    
    /**
     * Lists current subscriptions for the given user.
     * @param user the user name, not null
     * @return a <code>Collection<String></code> of mailbox names
     */
    public Collection subscriptions(String user) throws SubscriptionException;
}
