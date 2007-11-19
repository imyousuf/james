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

package org.apache.james.mailboxmanager.mailbox;

import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;



/**
 * 
 * An EventTriggerMailbox will fire an event of the types defined in
 * MailboxListener. When the underlaying store is modified by mupltiple
 * instances it has to keep track of last known status and deliver events as
 * soon as it detects external operations
 * 
 */
public interface EventTriggerMailbox {

    /**
     * Implementations of Mailbox may interpret the fact that someone is
     * listening and do some caching and even postpone persistence until
     * everyone has removed itself.
     * 
     * @param listener
     * @throws MailboxManagerException 
     */
    void addListener(MailboxListener listener) throws MailboxManagerException;

    void removeListener(MailboxListener listener);

}
