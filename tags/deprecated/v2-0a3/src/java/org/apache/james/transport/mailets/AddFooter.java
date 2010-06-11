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

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.MimePart;
import java.io.IOException;
import java.util.StringTokenizer;

/**
 * This mailet will attach text to the end of the message (like a footer).  Right
 * now it only supports simple messages without multiple parts.
 */
public class AddFooter extends GenericMailet {

    //This is the plain text version of the footer we are going to add
    String text = "";

    public void init() throws MessagingException {
        text = getInitParameter("text");
    }

    /**
     * Takes the message and attaches a footer message to it.  Right now, it only
     * supports simple messages.  Needs to have additions to make it support
     * messages with alternate content types or with attachments.
     */
    public void service(Mail mail) throws MessagingException {
        try {
            MimeMessage message = mail.getMessage();

            //I want to modify the right message body
            if (message.isMimeType("text/plain")) {
                //This is a straight text message... just append the single part normally
                addToText(message);
            } else if (message.isMimeType("multipart/mixed")) {
                //Find the first body part, and determine what to do then.
                MimeMultipart multipart = (MimeMultipart)message.getContent();
                MimeBodyPart part = (MimeBodyPart)multipart.getBodyPart(0);
                attachFooter(part);
                //We have to do this because of a bug in JavaMail (ref id 4404733)
                message.setContent(multipart);
            } else if (message.isMimeType("multipart/alternative")) {
                //Find the HTML and text message types and add to each
                MimeMultipart multipart = (MimeMultipart)message.getContent();
                for (int i = 0; i < multipart.getCount(); i++) {
                    MimeBodyPart part = (MimeBodyPart)multipart.getBodyPart(i);
                    attachFooter(part);
                }
                //We have to do this because of a bug in JavaMail (ref id 4404733)
                message.setContent(multipart);
            } else {
                //Give up... we won't attach the footer to this message
            }
        } catch (IOException ioe) {
            throw new MessagingException("Could not read message", ioe);
        }
    }

    /**
     * This is exposed as a method for easy subclassing to provide alternate ways
     * to get the footer text.
     */
    public String getFooterText() {
        return text;
    }

    /**
     * This is exposed as a method for easy subclassing to provide alternate ways
     * to get the footer text.  By default, this will take the footer text,
     * converting the linefeeds to &lt;br&gt; tags.
     */
    public String getFooterHTML() {
        String text = getFooterText();
        StringBuffer sb = new StringBuffer();
        StringTokenizer st = new StringTokenizer(text, "\r\n", true);
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.equals("\r")) {
                continue;
            }
            if (token.equals("\n")) {
                sb.append("<br />\n");
            } else {
                sb.append(token);
            }
        }
        return sb.toString();
    }

    public String getMailetInfo() {
        return "AddFooter Mailet";
    }

    protected void addToText(MimePart part) throws MessagingException, IOException {
        String content = part.getContent().toString();
        if (!content.endsWith("\n")) {
            content += "\r\n";
        }
        content += getFooterText();
        part.setText(content);
    }

    protected void addToHTML(MimePart part) throws MessagingException, IOException {
        String content = part.getContent().toString();
        content += "<br>" + getFooterHTML();
        part.setContent(content, part.getContentType());
    }

    protected void attachFooter(MimePart part) throws MessagingException, IOException {
        if (part.isMimeType("text/plain")) {
            addToText(part);
        } else if (part.isMimeType("text/html")) {
            addToHTML(part);
        } else {
            System.err.println(part.getContentType());
        }
        //Give up... we won't attach the footer to this message
    }
}
