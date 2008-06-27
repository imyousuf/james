/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

package org.apache.james.imapserver;

/**
 * Interface for objects that are sources for Mailbox Events. Mailbox Events
 * are used to inform registered listeners of events in this Source. For
 * example, if mail is delivered to an Inbox or if another user appends or 
 * deletes a message.
 *
 * <p>Not currently active in this implementation
 *
 * @version 0.1 on 14 Dec 2000
 */
public interface MailboxEventSource  {
  
    /**
     * Registers a MailboxEventListener.
     *
     * @param mel MailboxEventListener to be registered with this source.
     */
    void addMailboxEventListener( MailboxEventListener mel );

    /**
     * Deregisters a MailboxEventListener.
     *
     * @param mel MailboxEventListener to be deregistered from this source.
     */
    void removeMailboxEventListener( MailboxEventListener mel );
}
 

