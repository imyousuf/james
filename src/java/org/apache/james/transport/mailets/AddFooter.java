/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets;

import java.io.*;
import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.james.*;
import org.apache.james.transport.*;
import org.apache.mailet.*;

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
            String contentType = message.getContentType();
            if (contentType == null || contentType.startsWith("text/plain")) {
                //This is a straight text message... just append the single part normally
                String content = message.getContent().toString();
                content += getFooterText();
                message.setText(content);
            } else {
                //System.err.println(message.getContentType());
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

    public String getMailetInfo() {
        return "AddFooter Mailet";
    }
}
