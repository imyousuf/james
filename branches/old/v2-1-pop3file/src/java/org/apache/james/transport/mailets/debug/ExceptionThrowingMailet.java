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
import org.apache.mailet.MailetException;

import javax.mail.MessagingException;

/**
 * Debugging purpose Mailet. Just throws an exception.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class ExceptionThrowingMailet extends GenericMailet {

    /**
     * Throw an exception if any mail is processed.
     *
     * @param mail the mail to process
     *
     * @throws MailetException in all cases
     */
    public void service(Mail mail) throws MessagingException {
        throw new MailetException("General protection fault");
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "ExceptionThrowingMailet Mailet";
    }
}
