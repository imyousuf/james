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

/**
 * A simple in memory counter.  Designed to count messages sent to this recipient
 * for debugging purposes.
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public class Counter extends GenericMailet {

    /**
     * The number of mails processed by this mailet
     */
    int counter = 0;

    /**
     * Count processed mails, marking each mail as completed after counting.
     *
     * @param mail the mail to process
     */
    public void service(Mail mail) {
        counter++;
        log(counter + "");
        mail.setState(Mail.GHOST);
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Counter Mailet";
    }
}
