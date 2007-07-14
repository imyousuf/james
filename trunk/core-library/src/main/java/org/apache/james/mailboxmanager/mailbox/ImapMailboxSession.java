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
import org.apache.james.mailboxmanager.Quota;
import org.apache.james.mailboxmanager.acl.MailboxRights;


/**
 * This is the Mailbox from the view of the user<br />
 * 
 * <p>Not sure whether it should extend ImapMailbox or provide it with a getMailbox() method.</p>
 * <p>If it extends ImapMailbox it requires an adapter but it would be possible to check rights
 * and maybe quota on access.</p> 
 * <p>Another requirements for sessions is to keep the message numbers stable. Maybe message numbers
 * should only be provided by the session</p>
 */

public interface ImapMailboxSession extends ImapMailbox, MailboxListener, GeneralMailboxSession, EventQueueingSessionMailbox {
    
    
    /**
     *
     * @return the effective rights to this mailbox
     */
    MailboxRights myRights();
    /**
     * 
     * @return the quota that is assigned to this mailbox
     */
    Quota[] getQuota();

    boolean isSelectable();
    
}
