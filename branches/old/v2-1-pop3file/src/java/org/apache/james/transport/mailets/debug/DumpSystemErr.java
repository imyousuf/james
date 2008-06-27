/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets.debug;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;

/**
 * Debugging purpose Mailet.  Sends the message to System.err
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class DumpSystemErr extends GenericMailet {

    /**
     * Writes the message to System.err .
     *
     * @param mail the mail to process
     *
     * @throws MessagingException if an error occurs while writing the message
     */
    public void service(Mail mail) throws MessagingException {
        try {
            MimeMessage message = mail.getMessage();
            message.writeTo(System.err);
        } catch (IOException ioe) {
            log("error printing message", ioe);
        }
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Dumps message to System.err";
    }
}
