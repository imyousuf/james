/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imap41server;


/**
 * Interface for objects that are sources for Mailbox Events. Mailbox Events
 * are used to inform registered listeners of events in this Source. For
 * example, if mail is delivered to an Inbox or if another user appends or 
 * deletes a message.
 *
 * @author Charles Benett <charles@benett1.demon.co.uk>
 * @version 0.1
 */

public interface MailboxEventSource  {

  
    /**
     * Registers a MailboxEventListener.
     *
     * @param mel MailboxEventListener to be registered with this source.
     */
    public void addMailboxEventListener(MailboxEventListener mel) ;

    /**
     * Deregisters a MailboxEventListener.
     *
     * @param mel MailboxEventListener to be deregistered from this source.
     */
    public void removeMailboxEventListener(MailboxEventListener mel);
}
 

