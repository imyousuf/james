/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imapserver;

import java.net.Socket;
import org.apache.avalon.Composer;
import org.apache.avalon.Contextualizable;
import org.apache.avalon.Initializable;
import org.apache.avalon.Stoppable;
import org.apache.avalon.configuration.Configurable;
import org.apache.avalon.configuration.Configuration;
import org.apache.cornerstone.services.Scheduler;
import org.apache.phoenix.Service;

/**
 * An IMAP Handler handles one IMAP connection. TBC - it may spawn worker
 * threads someday
 *
 * @author <a href="mailto:charles@benett1.demon.co.uk">Charles Benett</a>
 * @version 0.1 on 14 Dec 2000
 */
public interface ConnectionHandler
    extends Service, Configurable, Composer, Contextualizable,
    Initializable, Runnable, Stoppable, Scheduler.Target,
    MailboxEventListener {

    /**
     * Prepares Connection Handler object by in/out streams to socket. Used before object is attached to its own thread.
     *
     * @param socket Socket providing connection.
     */
    void parseRequest( Socket socket );
}


