/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2000-2003 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Apache", "Jakarta", "JAMES" and "Apache Software Foundation"
 *    must not be used to endorse or promote products derived from this
 *    software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache",
 *    nor may "Apache" appear in their name, without prior written
 *    permission of the Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * Portions of this software are based upon public domain software
 * originally written at the National Center for Supercomputing Applications,
 * University of Illinois, Urbana-Champaign.
 */

package org.apache.james.transport.mailets;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.HashSet;
import java.util.Set;

/**
 * Returns the current time for the mail server.  Sample configuration:
 * <mailet match="RecipientIs=time@cadenza.lokitech.com" class="ServerTime">
 * </mailet>
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
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

        // Someone manually checking the server time by hand may send
        // an formatted message, lacking From and To headers.  If the
        // response fields are null, try setting them from the SMTP
        // MAIL FROM/RCPT TO commands used to send the inquiry.

        if (response.getFrom() == null) {
            response.setFrom(((MailAddress)mail.getRecipients().iterator().next()).toInternetAddress());
        }

        if (response.getAllRecipients() == null) {
            response.setRecipients(MimeMessage.RecipientType.TO, mail.getSender().toString());
        }

        response.saveChanges();
        getMailetContext().sendMail(response);
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

