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
    int counter = 0;

    public void service(Mail mail) {
        //Do nothing
        counter++;
        log(counter + "");
        mail.setState(Mail.GHOST);
    }

    public String getMailetInfo() {
        return "Counter Mailet";
    }
}
