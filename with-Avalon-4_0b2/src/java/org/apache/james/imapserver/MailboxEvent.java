/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.imapserver;

import java.util.EventObject;

/**
 * EventObject representing  a change in a Mailbox which needs to be
 * communicated to MailboxEventListeners.
 * Uses include warning of addition/ deletion of messages.
 *
 * <p>Not currently used in this implementation
 *
 * @author  <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public class MailboxEvent 
    extends EventObject {
   
    private String callingMailbox = null;

    public MailboxEvent( final Object source, final String mailbox ) {
        super( source );
        callingMailbox = mailbox;
    }

    public String getMailbox() {       
        return callingMailbox;
    }
}
