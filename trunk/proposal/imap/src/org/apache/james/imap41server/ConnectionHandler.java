/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.imap41server;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

import org.apache.avalon.*;
import org.apache.mailet.Mail;
import org.apache.avalon.blocks.*;
import org.apache.avalon.utils.*;
import org.apache.james.*;
import org.apache.james.transport.*;
import org.apache.james.usermanager.*;

import javax.mail.MessagingException;
import javax.mail.internet.*;

/**
 * An IMAP Handler handles one IMAP connection. TBC - it may spawn worker
 * threads someday
 *
 * @author Federico Barbieri <scoobie@systemy.it>
 * @author  Charles Benett <charles@benett1.demon.co.uk>
 * @version 0.1
 */
public interface ConnectionHandler
    extends Composer, Stoppable, Configurable, Service, TimeServer.Bell,
	       Contextualizable, MailboxEventListener {

    /**
     * Prepares Connection Handler object by in/out streams to socket. Used before object is attached to its own thread.
     *
     * @param socket Socket providing connection.
     */
    public void parseRequest(Socket socket);

}


