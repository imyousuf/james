/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.mailets;

import org.apache.mailet.*;
import javax.mail.*;

/**
 * No idea what this class is for..... seems to send processor of a message to
 * another mailet (which I didn't think we were supporting)
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class ToProcessor extends GenericMailet {
    String processor;

    public void init() throws MailetException {
        processor = getInitParameter("processor");
    }

    public void service(Mail mail) throws MailetException, MessagingException {
        log("Sending mail " + mail + " to " + processor);
        getMailetContext().sendMail(mail.getSender(), mail.getRecipients(), mail.getMessage(), processor);
        mail.setState(Mail.GHOST);
    }


    public String getMailetInfo() {
        return "ToProcessor Mailet";
    }
}
