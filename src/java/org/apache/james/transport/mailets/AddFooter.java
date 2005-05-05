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

import org.apache.james.core.MailImpl;
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

    /**
     * This is the plain text version of the footer we are going to add
     */
    String text = "";

    /**
     * Initialize the mailet
     */
    public void init() throws MessagingException {
        text = getInitParameter("text");
    }

    /**
     * Takes the message and attaches a footer message to it.  Right now, it only
     * supports simple messages.  Needs to have additions to make it support
     * messages with alternate content types or with attachments.
     *
     * @param mail the mail being processed
     *
     * @throws MessagingException if an error arises during message processing
     */
    public void service(Mail mail) throws MessagingException {
        try {
            MimeMessage message = mail.getMessage();
//            log("Trying to add footer to mail " + ((MailImpl)mail).getName());
            if (attachFooter(message)) {
                message.saveChanges();
//                log("Message after saving: " + message.getContent().toString());
                /*
                java.io.ByteArrayOutputStream bodyOs = new java.io.ByteArrayOutputStream(512);
                java.io.OutputStream bos;
                java.io.InputStream bis;
                try {
                    bis = message.getRawInputStream();
                    bos = bodyOs;
                    log("Using getRawInputStream()");
                } catch(javax.mail.MessagingException me) {
                    bos = javax.mail.internet.MimeUtility.encode(bodyOs, message.getEncoding());
                    bis = message.getInputStream();
                    log("Using getInputStream()");
                }

                try {
                    byte[] block = new byte[1024];
                    int read = 0;
                    while ((read = bis.read(block)) > -1) {
                        bos.write(block, 0, read);
                    }
                    bos.flush();
                }
                finally {
                    org.apache.avalon.excalibur.io.IOUtil.shutdownStream(bis);             
                }
                log("Message from stream: " + bodyOs.toString());
                */
            } else {
                log("Unable to add footer to mail " + ((MailImpl)mail).getName());
            }
        } catch (IOException ioe) {
            throw new MessagingException("Could not read message", ioe);
        }
    }

    /**
     * This is exposed as a method for easy subclassing to provide alternate ways
     * to get the footer text.
     *
     * @return the footer text
     */
    public String getFooterText() {
        return text;
    }

    /**
     * This is exposed as a method for easy subclassing to provide alternate ways
     * to get the footer text.  By default, this will take the footer text,
     * converting the linefeeds to &lt;br&gt; tags.
     *
     * @return the HTML version of the footer text
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

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "AddFooter Mailet";
    }

    /**
     * Prepends the content of the MimePart as text to the existing footer
     *
     * @param part the MimePart to attach
     *
     * @throws MessagingException
     * @throws IOException
     */
    protected void addToText(MimePart part) throws MessagingException, IOException {
//        log("Trying to add footer to " + part.getContent().toString());
        String content = part.getContent().toString();
        if (!content.endsWith("\n")) {
            content += "\r\n";
        }
        content += getFooterText();
        part.setText(content);
//        log("After adding footer: " + part.getContent().toString());
    }

    /**
     * Prepends the content of the MimePart as HTML to the existing footer
     *
     * @param part the MimePart to attach
     *
     * @throws MessagingException
     * @throws IOException
     */
    protected void addToHTML(MimePart part) throws MessagingException, IOException {
//        log("Trying to add footer to " + part.getContent().toString());
        String content = part.getContent().toString();

        /* This HTML part may have a closing <BODY> tag.  If so, we
         * want to insert out footer immediately prior to that tag.
         */
        int index = content.lastIndexOf("</body>");
        if (index == -1) index = content.lastIndexOf("</BODY>");
        String insert = "<br>" + getFooterHTML();
        content = index == -1 ? content + insert : content.substring(0, index) + insert + content.substring(index);
   
        part.setContent(content, part.getContentType());
//        log("After adding footer: " + part.getContent().toString());
    }

    /**
     * Attach a footer a MimePart
     *
     * @param part the MimePart to which the footer is to be attached
     *
     * @return whether a footer was successfully attached
     * @throws MessagingException
     * @throws IOException
     */
    protected boolean attachFooter(MimePart part) throws MessagingException, IOException {
//        log("Content type is " + part.getContentType());
        if (part.isMimeType("text/plain")) {
            addToText(part);
            return true;
        } else if (part.isMimeType("text/html")) {
            addToHTML(part);
            return true;
        } else if (part.isMimeType("multipart/mixed")) {
            //Find the first body part, and determine what to do then.
            MimeMultipart multipart = (MimeMultipart)part.getContent();
            MimeBodyPart firstPart = (MimeBodyPart)multipart.getBodyPart(0);
            boolean isFooterAttached = attachFooter(firstPart);
            //We have to do this because of a bug in JavaMail (ref id 4404733)
            part.setContent(multipart);
            return isFooterAttached;
        } else if (part.isMimeType("multipart/alternative")) {
            MimeMultipart multipart = (MimeMultipart)part.getContent();
            int count = multipart.getCount();
//            log("number of alternatives = " + count);
            boolean isFooterAttached = false;
            for (int index = 0; index < count; index++) {
//                log("processing alternative #" + index);
                MimeBodyPart mimeBodyPart = (MimeBodyPart)multipart.getBodyPart(index);
                isFooterAttached |= attachFooter(mimeBodyPart);
            }
            //We have to do this because of a bug in JavaMail (ref id 4404733)
            part.setContent(multipart);
            return isFooterAttached;
        } else {
            //Give up... we won't attach the footer to this MimePart
            return false;
        }
    }
}
