/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.mailets;

import org.apache.mailet.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;

/**
 * Replace incoming recipient with specified ones.
 *
 * @author Federico Barbieri <scoobie@pop.systemy.it>
 * @author Serge Knystautas <sergek@lokitech.com>
 */
public class Forward extends GenericMailet {

    private Collection newRecipients;

    public void init () throws MailetException {
        newRecipients = new Vector();
        StringTokenizer st = new StringTokenizer(getMailetConfig().getInitParameter("forwardto"), ",", false);
        try {
            while (st.hasMoreTokens()) {
                newRecipients.add(new MailAddress(st.nextToken()));
            }
        } catch (ParseException pe) {
            throw new MailetException("Invalid mail address specified", pe);
        }
    }

    public void service(Mail mail) throws MailetException, MessagingException {
        getMailetContext().sendMail(mail.getSender(), newRecipients, mail.getMessage());
        mail.setState(Mail.GHOST);
    }

    public String getMailetInfo() {
        return "Forward Mailet";
    }
}

