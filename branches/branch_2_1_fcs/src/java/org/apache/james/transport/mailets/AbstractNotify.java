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

import org.apache.james.util.RFC2822Headers;
import org.apache.james.util.RFC822DateFormat;
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
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Collection;
import java.util.Iterator;

/**
 * <P>Abstract mailet providing configurable notification services.<BR>
 * This mailet can be subclassed to make authoring notification mailets simple.<BR>
 * <P>Provides the following functionalities to all notification subclasses:</P>
 * <UL>
 * <LI>A common notification message layout.</LI>
 * <LI>A sender of the notification message can optionally be specified.
 * If one is not specified, the postmaster's address will be used.</LI>
 * <LI>A notice text can be specified, and in such case will be inserted into the 
 * notification inline text.</LI>
 * <LI>If the notified message has an "error message" set, it will be inserted into the 
 * notification inline text. If the <CODE>attachStackTrace</CODE> init parameter
 * is set to true, such error message will be attached to the notification message.</LI>
 * <LI>The notified messages are attached in their entirety (headers and
 * content) and the resulting MIME part type is "message/rfc822".</LI>
 * <LI>passThrough is <B>true</B>.</LI>
 * </UL>
 *
 * <P>Sample configuration common to all notification mailet subclasses:</P>
 * <PRE><CODE>
 * &lt;mailet match="All" class="<I>a notification mailet</I>">
 *   &lt;sendingAddress&gt;<I>an address or postmaster</I>&lt;/sendingAddress&gt;
 *   &lt;attachStackTrace&gt;<I>true or false, default=false</I>&lt;/attachStackTrace&gt;
 *   &lt;notice&gt;<I>notice attached to the message (optional)</I>&lt;/notice&gt;
 * &lt;/mailet&gt;
 * </CODE></PRE>
 *
 */
public abstract class AbstractNotify extends AbstractRedirect {
    
    /* ******************************************************************** */
    /* ****************** Begin of getX and setX methods ****************** */
    /* ******************************************************************** */
    
    /**
     * @return true, as all notifications should
     */
    protected boolean getPassThrough() throws MessagingException {
        return true;
    }

    /**
     * @return <CODE>NONE</CODE>
     */
    protected int getInLineType() {
        return NONE;
    }
    
    /**
     * @return <CODE>MESSAGE</CODE>
     */
    protected int getAttachmentType() {
        return MESSAGE;
    }
    
    /**
     * @return the <CODE>notice</CODE> init parameter
     */
    protected String getMessage() {
        if(getInitParameter("notice") == null) {
            return "We were unable to deliver the attached message because of an error in the mail server.";
        } else {
            return getInitParameter("notice");
        }
    }
    
    /**
     * @return the full message to append, built from the Mail object
     */
    protected String getMessage(Mail originalMail) throws MessagingException {
        MimeMessage message = originalMail.getMessage();
        StringWriter sout = new StringWriter();
        PrintWriter out = new PrintWriter(sout, true);
        
        // First add the "local" notice
        // (either from conf or generic error message)
        out.println(getMessage());
        // And then the message from other mailets
        if (originalMail.getErrorMessage() != null) {
            out.println();
            out.println("Error message below:");
            out.println(originalMail.getErrorMessage());
        }
        out.println();
        out.println("Message details:");

        if (message.getSubject() != null) {
            out.println("  Subject: " + message.getSubject());
        }
        if (message.getSentDate() != null) {
            out.println("  Sent date: " + message.getSentDate());
        }
        out.println("  MAIL FROM: " + originalMail.getSender());
        Iterator rcptTo = originalMail.getRecipients().iterator();
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
        
        return sout.toString();
    }
    
    /**
     * @return the value of the <CODE>sendingAddress</CODE> init parameter if not null,
     * the postmaster address otherwise
     */
    protected MailAddress getSender() throws MessagingException {
        if (getInitParameter("sendingAddress") == null) {
            return getMailetContext().getPostmaster();
        } else {
            return new MailAddress(getInitParameter("sendingAddress"));
        }
    }
    
    /**
     * @return the <CODE>attachStackTrace</CODE> init parameter
     */
    protected boolean attachError() {
        boolean attachStackTrace = false;
        try {
            attachStackTrace = new Boolean(getInitParameter("attachStackTrace")).booleanValue();
        } catch (Exception e) {
            // Ignore exception, default to false
        }
        return attachStackTrace;
    }
    
    /**
     * @return true
     */
    protected boolean isReply() {
        return true;
    }

    /* ******************************************************************** */
    /* ******************* End of getX and setX methods ******************* */
    /* ******************************************************************** */
    
}

