/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;

/**
 * Simplest Mailet which destroys any incoming messages.
 *
 * @author  Federico Barbieri <scoobie@pop.systemy.it>
 */
public class Null extends GenericMailet {

    /**
     * Set this mail to GHOST state, indicating that no further processing 
     * should take place.
     *
     * @param mail the mail to process
     */
    public void service(Mail mail) {
        mail.setState(Mail.GHOST);
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Null Mailet";
    }
}

