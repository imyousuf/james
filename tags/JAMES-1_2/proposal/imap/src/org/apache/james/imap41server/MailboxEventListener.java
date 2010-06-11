/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imap41server;

import java.util.EventListener;

/**
 * Interface for objects that need to be informed of changes in a Mailbox..
 * 
 * @author Charles Benett <charles@benett1.demon.co.uk>
 * @version 0.1
 */
public interface MailboxEventListener extends EventListener {

    public void receiveEvent(MailboxEvent me);

}
