/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.mailets;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.mailet.*;
import org.apache.james.*;
import org.apache.james.transport.*;

/**
 * Sends an error message to the sender of a message (that's typically landed in
 * the error mail repository).  You can optionally specify a sender of the error
 * message.  If you do not specify one, it will use the postmaster's address
 *
 * Sample configuration:
 * <mailet match="All" class="ErrorBounce">
 *   <admin-sender>nobounce@localhost</admin-sender>
 * </mailet>
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class ErrorBounce extends GenericMailet {
    MailAddress sender = null;

    public void init() throws MailetException {
        if (getInitParameter("admin-sender") == null) {
            sender = getMailetContext().getPostmaster();
        } else {
            try {
                sender = new MailAddress(getInitParameter("admin-sender"));
            } catch (MessagingException me) {
                throw new MailetException("Error parsing admin-sender email address (" + getInitParameter("admin-sender") + ")");
            }
        }
    }

    /**
     * Sends a message back to the sender indicating what time the server thinks it is.
     */
    public void service(Mail mail) throws MailetException, MessagingException {
        getMailetContext().bounce(mail, mail.getErrorMessage(), sender.toString());
    }

    public String getMailetInfo() {
        return "ErrorBounce Mailet";
    }
}

