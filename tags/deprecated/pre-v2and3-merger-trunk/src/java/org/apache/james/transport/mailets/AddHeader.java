/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
 * All rights reserved.                                                *
 * ------------------------------------------------------------------- *
 * Licensed under the Apache License, Version 2.0 (the "License"); you *
 * may not use this file except in compliance with the License. You    *
 * may obtain a copy of the License at:                                *
 *                                                                     *
 *     http://www.apache.org/licenses/LICENSE-2.0                      *
 *                                                                     *
 * Unless required by applicable law or agreed to in writing, software *
 * distributed under the License is distributed on an "AS IS" BASIS,   *
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or     *
 * implied.  See the License for the specific language governing       *
 * permissions and limitations under the License.                      *
 ***********************************************************************/

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

