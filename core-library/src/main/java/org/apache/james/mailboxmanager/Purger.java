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

package org.apache.james.mailboxmanager;

import javax.mail.Flags;

/**
 * bound to a quota object and will be called when quota has exceeded
 * 
 * <h4>Milestone 7</h4>
 * 
 * The first action of the Purger is expunging all \Deleted messages. If
 * the quota limit is still exceeded it will start with the oldest message.
 * 
 */

public interface Purger {

    boolean isEnabled();

    void setEnabled(boolean enabled);

    /**
     * <p>
     * If set, purged messages are moved to this destination. If moving fails
     * for any reason, messages will not be purged.
     * </p>
     * <p>
     * If not set (null) messages will be expunged from the mailbox at once
     * </p>
     * 
     * @return destination mailbox, null if messages will be expunged.
     */

    String getDestination();

    /**
     * @param destination
     *            destination mailbox, null if messages will be expunged.
     */

    void setDestination(String destination);

    /**
     * The minimal age in days of messages that are allowed to be purged.
     * 
     * @return minimal age in days. 0 if all message could be purged
     */

    int getMinimalAge();

    /**
     * 
     * @param days minimal age in days. 0 if all message could be purged
     */
    void setMinimalAge(int days);

    /**
     * <p>Message that are considered for purging have to have one of this flags.</p>
     * <p>A common example could be the \Seen Flag to only purge seen messages</p>
     * 
     * @return flags to purge
     */
    
    Flags getDoPurgeFlags();
    
    /**
     * @param flags flags to purge
     */

    void setDoPurgeFlags(Flags flags);

    /**
     * <p>Messages that have one of this Flags will not be purged.</p>
     * <p>A common example would be marking interesting/important messages a mailing list. This could be
     * done with the \Flagged flag that is supported by various clients</p>  
     * 
     * @return flags not to purge, may be null
     */
    Flags getDontPurgeFlags();

    /**
     * 
     * @param flags flags not to purge, may be null
     */
    
    void setDontPurgeFlags(Flags flags);

    /**
     * Presists the changes. Implementations may decide to persist changes at once. 
     */
    
    void save();

}
