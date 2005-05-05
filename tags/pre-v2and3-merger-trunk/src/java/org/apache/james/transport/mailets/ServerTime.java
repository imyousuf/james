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

import java.util.HashSet;
import java.util.Set;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

/**
 * Returns the current time for the mail server.  Sample configuration:
 * &lt;mailet match="RecipientIs=time@cadenza.lokitech.com" class="ServerTime"&gt;
 * &lt;/mailet&gt;
 *
 */
public class ServerTime extends GenericMailet {
    /**
     * Sends a message back to the sender indicating what time the server thinks it is.
     *
     * @param mail the mail being processed
     *
     * @throws MessagingException if an error is encountered while formulating the reply message
     */
    public void service(Mail mail) throws javax.mail.MessagingException {
        MimeMessage response = (MimeMessage)mail.getMessage().reply(false);
        response.setSubject("The time is now...");
        StringBuffer textBuffer =
            new StringBuffer(128)
                    .append("This mail server thinks it's ")
                    .append((new java.util.Date()).toString())
                    .append(".");
        response.setText(textBuffer.toString());

        Set recipients = new HashSet();
        Address addresses[] = response.getAllRecipients();
        for (int i = 0; i < addresses.length; i++) {
            recipients.add(new MailAddress((InternetAddress)addresses[0]));
        }

        MailAddress sender = new MailAddress((InternetAddress)response.getFrom()[0]);
        getMailetContext().sendMail(sender, recipients, response);
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "ServerTime Mailet";
    }
}

