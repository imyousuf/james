/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets;

import javax.mail.internet.MimeMessage;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;

/**
 * Adds a specified header and value to the message.
 *
 * Sample configuration:
 *
 * &lt;mailet match="All" class="AddHeader"&gt;
 *   &lt;name&gt;X-MailetHeader&lt;/name&gt;
 *   &lt;value&gt;TheHeaderValue&lt;/value&gt;
 * &lt;/mailet&gt;
 *
 * @version 1.0.0, 2002-09-11
 */
public class AddHeader
       extends GenericMailet {

    /**
     * The name of the header to be added.
     */
    private String headerName;

    /**
     * The value to be set for the header.
     */
    private String headerValue;

    /**
     * Initialize the mailet.
     */
    public void init() {
        headerName = getInitParameter("name");
        headerValue = getInitParameter("value");
    }

    /**
     * Takes the message and adds a header to it.
     *
     * @param mail the mail being processed
     *
     * @throws MessagingException if an error arises during message processing
     */
    public void service(Mail mail) {
        try {
            MimeMessage message = mail.getMessage () ;

            //Set the header name and value (supplied at init time).
            message.setHeader(headerName, headerValue);
            message.saveChanges();
        } catch (javax.mail.MessagingException me) {
            log (me.getMessage());
        }
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "AddHeader Mailet" ;
    }

}

