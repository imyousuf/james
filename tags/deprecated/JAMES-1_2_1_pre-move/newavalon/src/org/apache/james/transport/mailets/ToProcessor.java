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
 * Sample configuration:
 * <mailet match="All" class="ToProcessor">
 *   <processor>spam</processor>
 *   <notice>Notice attached to the message (optional)</notice>
 * </mailet>
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class ToProcessor extends GenericMailet {
    String processor;
    String noticeText = null;

    public void init() throws MailetException {
        processor = getInitParameter("processor");
        if (processor == null) {
            throw new MailetException("processor parameter is required");
        }
        noticeText = getInitParameter("notice");
    }

    public void service(Mail mail) throws MessagingException {
        log("Sending mail " + mail + " to " + processor);
        mail.setState(processor);
        if (noticeText != null) {
            if (mail.getErrorMessage() == null) {
                mail.setErrorMessage(noticeText);
            } else {
                mail.setErrorMessage(mail.getErrorMessage() + "\r\n" + noticeText);
            }
        }
    }


    public String getMailetInfo() {
        return "ToProcessor Mailet";
    }
}
