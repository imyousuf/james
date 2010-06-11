/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets;

import org.apache.james.util.RFC822Date;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetException;

import javax.mail.Address;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashSet;
import java.util.Set;

/**
 * Sends an error message to the sender of a message (that's typically landed in
 * the error mail repository).  You can optionally specify a sender of the error
 * message.  If you do not specify one, it will use the postmaster's address
 *
 * Sample configuration:
 * <mailet match="All" class="NotifyPostmaster">
 *   <sendingAddress>nobounce@localhost</sendingAddress>
 *   <attachStackTrace>true</attachStackTrace>
 *   <notice>Notice attached to the message (optional)</notice>
 * </mailet>
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 * @author  Ivan Seskar <iseskar@upsideweb.com>
 * @author  Danny Angus <danny@thought.co.uk>
 */
public class NotifySender extends GenericMailet {
    MailAddress notifier = null;
    boolean attachStackTrace = false;
    String noticeText = null;

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
        }
    }

    /**
     * Sends a message back to the sender with the message as to why it failed.
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
        Address[] rcpts = message.getRecipients(Message.RecipientType.TO);
        if (rcpts != null) {
            out.print("  To: ");
            for (int i = 0; i < rcpts.length; i++) {
                out.print(rcpts[i] + " ");
            }
            out.println();
        }
        rcpts = message.getRecipients(Message.RecipientType.CC);
        if (rcpts != null) {
            out.print("  CC: ");
            for (int i = 0; i < rcpts.length; i++) {
                out.print(rcpts[i] + " ");
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
            part.setHeader("Content-Type", "text/plain");
            multipart.addBodyPart(part);

            //Add the original message as the second mime body part
            part = new MimeBodyPart();
            part.setContent(message.getContent(), message.getContentType());
            part.setHeader("Content-Type", message.getContentType());
            multipart.addBodyPart(part);

            //if set, attach the full stack trace
            if (attachStackTrace && mail.getErrorMessage() != null) {
                part = new MimeBodyPart();
                part.setContent(mail.getErrorMessage(), "text/plain");
                part.setHeader("Content-Type", "text/plain");
                multipart.addBodyPart(part);
            }

            reply.setContent(multipart);
            reply.setHeader("Content-Type", multipart.getContentType());
        } catch (IOException ioe) {
            throw new MailetException("Unable to create multipart body");
        }

        //Create the list of recipients in our MailAddress format
        Set recipients = new HashSet();
        recipients.add(mail.getSender());

        //Set additional headers
        if (reply.getHeader("Date")==null){
            reply.setHeader("Date",new RFC822Date().toString());
        }
        String subject = message.getSubject();
        if (subject == null) {
            subject = "";
        }
        if (subject.indexOf("Re:") == 0){
            reply.setSubject(subject);
        } else {
            reply.setSubject("Re:" + subject);
        }
        reply.setHeader("In-Reply-To", message.getMessageID());

        //Send it off...
        getMailetContext().sendMail(notifier, recipients, reply);
    }

    public String getMailetInfo() {
        return "NotifySender Mailet";
    }
}

