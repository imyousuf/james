/* ====================================================================
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetException;
import org.apache.mailet.RFC2822Headers;
import org.apache.mailet.dates.RFC822DateFormat;

/**
 * Sends an error message to the sender of a message (that's typically landed in
 * the error mail repository).  You can optionally specify a sender of the error
 * message.  If you do not specify one, it will use the postmaster's address
 *
 * Sample configuration:
 * &lt;mailet match="All" class="NotifyPostmaster"&gt;
 *   &lt;sendingAddress&gt;nobounce@localhost&lt;/sendingAddress&gt;
 *   &lt;attachStackTrace&gt;true&lt;/attachStackTrace&gt;
 *   &lt;notice&gt;Notice attached to the message (optional)&lt;/notice&gt;
 * &lt;/mailet&gt;
 *
 */
public class NotifyPostmaster extends GenericMailet {

    /**
     * The sender address for the reply message
     */
    MailAddress notifier = null;

    /**
     * Whether exception stack traces should be attached to the error
     * messages
     */
    boolean attachStackTrace = false;

    /**
     * The text of the reply notice
     */
    String noticeText = null;

    /**
     * The date format object used to generate RFC 822 compliant date headers
     */
    private RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();

    /**
     * Initialize the mailet, loading all configuration parameters.
     *
     * @throws MessagingException
     */
    public void init() throws MessagingException {
        if (getInitParameter("sendingAddress") == null) {
            notifier = getMailetContext().getPostmaster();
        } else {
            notifier = new MailAddress(getInitParameter("sendingAddress"));
        }
        if (getInitParameter("notice") == null) {
            noticeText = "We were unable to deliver the attached message because of an error in the mail server.";
        } else {
            noticeText = getInitParameter("notice");
        }
        try {
            attachStackTrace = new Boolean(getInitParameter("attachStackTrace")).booleanValue();
        } catch (Exception e) {
            // Ignore exception, default to false
        }
    }

    /**
     * Sends a message to the postmaster with the original message as to why it failed.
     *
     * @param mail the mail being processed
     *
     * @throws MessagingException if an error occurs while formulating the message to the sender
     */
    public void service(Mail mail) throws MessagingException {
        MimeMessage message = mail.getMessage();
        //Create the reply message
        MimeMessage reply = new MimeMessage(Session.getDefaultInstance(System.getProperties(), null));

        //Create the list of recipients in the Address[] format
        InternetAddress[] rcptAddr = new InternetAddress[1];
        rcptAddr[0] = getMailetContext().getPostmaster().toInternetAddress();
        reply.setRecipients(Message.RecipientType.TO, rcptAddr);

        //Set the sender...
        reply.setFrom(notifier.toInternetAddress());

        //Create the message
        StringWriter sout = new StringWriter();
        PrintWriter out = new PrintWriter(sout, true);

        // First add the "local" notice
        // (either from conf or generic error message)
        out.println(noticeText);
        // And then the message from other mailets
        if (mail.getErrorMessage() != null) {
            out.println();
            out.println("Error message below:");
            out.println(mail.getErrorMessage());
        }
        out.println();
        out.println("Message details:");

        if (message.getSubject() != null) {
            out.println("  Subject: " + message.getSubject());
        }
        if (message.getSentDate() != null) {
            out.println("  Sent date: " + message.getSentDate());
        }
        out.println("  MAIL FROM: " + mail.getSender());
        Iterator rcptTo = mail.getRecipients().iterator();
        out.println("  RCPT TO: " + rcptTo.next());
        while (rcptTo.hasNext()) {
            out.println("           " + rcptTo.next());
        }
        String[] addresses = null;
        addresses = message.getHeader(RFC2822Headers.FROM);
        if (addresses != null) {
            out.print("  From: ");
            for (int i = 0; i < addresses.length; i++) {
                out.print(addresses[i] + " ");
            }
            out.println();
        }
        addresses = message.getHeader(RFC2822Headers.TO);
        if (addresses != null) {
            out.print("  To: ");
            for (int i = 0; i < addresses.length; i++) {
                out.print(addresses[i] + " ");
            }
            out.println();
        }
        addresses = message.getHeader(RFC2822Headers.CC);
        if (addresses != null) {
            out.print("  CC: ");
            for (int i = 0; i < addresses.length; i++) {
                out.print(addresses[i] + " ");
            }
            out.println();
        }
        out.println("  Size (in bytes): " + message.getSize());
        if (message.getLineCount() >= 0) {
            out.println("  Number of lines: " + message.getLineCount());
        }


        try {
            //Create the message body
            MimeMultipart multipart = new MimeMultipart();
            //Add message as the first mime body part
            MimeBodyPart part = new MimeBodyPart();
            part.setContent(sout.toString(), "text/plain");
            part.setHeader(RFC2822Headers.CONTENT_TYPE, "text/plain");
            multipart.addBodyPart(part);

            //Add the original message as the second mime body part
            part = new MimeBodyPart();
            part.setContent(message.getContent(), message.getContentType());
            part.setHeader(RFC2822Headers.CONTENT_TYPE, message.getContentType());
            multipart.addBodyPart(part);

            //if set, attach the full stack trace
            if (attachStackTrace && mail.getErrorMessage() != null) {
                part = new MimeBodyPart();
                part.setContent(mail.getErrorMessage(), "text/plain");
                part.setHeader(RFC2822Headers.CONTENT_TYPE, "text/plain");
                multipart.addBodyPart(part);
            }

            reply.setContent(multipart);
            reply.setHeader(RFC2822Headers.CONTENT_TYPE, multipart.getContentType());
        } catch (IOException ioe) {
            throw new MailetException("Unable to create multipart body");
        }

        //Create the list of recipients in our MailAddress format
        Set recipients = new HashSet();
        recipients.add(getMailetContext().getPostmaster());

        //Set additional headers
        if (reply.getHeader(RFC2822Headers.DATE)==null) {
            reply.setHeader(RFC2822Headers.DATE, rfc822DateFormat.format(new Date()));
        }
        String subject = message.getSubject();
        if (subject == null) {
            subject = "";
        }
        if (subject.indexOf("Re:") == 0) {
            reply.setSubject(subject);
        } else {
            reply.setSubject("Re:" + subject);
        }
        reply.setHeader(RFC2822Headers.IN_REPLY_TO, message.getMessageID());

        //Send it off...
        getMailetContext().sendMail(notifier, recipients, reply);
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "NotifyPostmaster Mailet";
    }
}

