/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

/**
 * Interface for objects that are sources for Mailbox Events. Mailbox Events
 * are used to inform registered listeners of events in this Source. For
 * example, if mail is delivered to an Inbox or if another user appends or 
 * deletes a message.
 *
 * <p>Not currently active in this implementation
 *
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
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
 

