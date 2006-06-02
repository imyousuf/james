/***********************************************************************
 * Copyright (c) 2006 The Apache Software Foundation.                  *
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

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;

/**
 * Add an prefix (tag) to the subject of a message <br>
 * <br>
 * 
 * Sample Configuration: <br>
 * <br>
 * &lt;mailet match="RecipientIs=robot@james.apache.org" class="TagMessage"&gt;
 * &lt;subjectPrefix&gt;[robot]&lt;/subjectPrefix&gt; &lt;/mailet&gt; <br>
 * <br>
 */
public class AddSubjectPrefix extends GenericMailet {

    // prefix to add
    private String subjectPrefix = null;

    /**
     * Initialize the mailet.
     */
    public void init() throws MessagingException {
        subjectPrefix = getInitParameter("subjectPrefix");

        if (subjectPrefix == null || subjectPrefix.equals("")) {
            throw new MessagingException(
                    "Please configure a valid subjectPrefix");
        }
    }

    /**
     * Takes the message and adds a prefix to the subject
     * 
     * @param mail
     *            the mail being processed
     * 
     * @throws MessagingException
     *             if an error arises during message processing
     */
    public void service(Mail mail) throws MessagingException {
        MimeMessage m = mail.getMessage();

        String subject = m.getSubject();

        if (subject != null) {
            m.setSubject(subjectPrefix + " " + subject);
        } else {
            m.setSubject(subjectPrefix);
        }
    }

}
