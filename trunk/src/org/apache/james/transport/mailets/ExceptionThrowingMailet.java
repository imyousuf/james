/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets;

import javax.mail.*;
import org.apache.mailet.*;

/**
 * Debugging purpose Mailet. Just throws an exception.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class ExceptionThrowingMailet extends GenericMailet {

    public void service(Mail mail) throws MessagingException {
        throw new MailetException("General protection fault");
    }

    public String getMailetInfo() {
        return "ExceptionThrowingMailet Mailet";
    }
}
