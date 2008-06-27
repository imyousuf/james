/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imapserver;

import java.io.Serializable;
import java.util.EventListener;

/**
 * Interface for objects that need to be informed of changes in a Mailbox.
 *
 * <p>Not currently active in this implementaiton
 * 
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public interface MailboxEventListener extends EventListener, Serializable {

    public void receiveEvent(MailboxEvent me);

}
