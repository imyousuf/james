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
import java.util.ArrayList;

/**
 * <P>Generates a response to the Return-Path address, or the
 * address of the message's sender if the Return-Path is not
 * available. Note that this is different than a mail-client's
 * reply, which would use the Reply-To or From header.</P>
 * <P>Bounced messages are attached in their entirety (headers and
 * content) and the resulting MIME part type is "message/rfc822".<BR>
 * The Return-Path header of the response is set to "null" ("<>"),
 * meaning that no reply should be sent.</P>
 * <P>A sender of the notification message can optionally be specified.
 * If one is not specified, the postmaster's address will be used.<BR>
 * A notice text can be specified, and in such case will be inserted into the 
 * notification inline text.<BR>
 * If the notified message has an "error message" set, it will be inserted into the 
 * notification inline text. If the <CODE>attachStackTrace</CODE> init parameter
 * is set to true, such error message will be attached to the notification message.<BR>
 * <P>passThrough is <B>true</B>.</P>
 *
 * <P>Sample configuration:</P>
 * <PRE><CODE>
 * &lt;mailet match="All" class="NotifyPostmaster">
 *   &lt;sendingAddress&gt;<I>an address or postmaster</I>&lt;/sendingAddress&gt;
 *   &lt;attachStackTrace&gt;<I>true or false, default=false</I>&lt;/attachStackTrace&gt;
 *   &lt;notice&gt;<I>notice attached to the message (optional)</I>&lt;/notice&gt;
 * &lt;/mailet&gt;
 * </CODE></PRE>
 *
 * <P>The behaviour of this mailet is equivalent to using Redirect with the following
 * configuration:</P>
 * <PRE><CODE>
 * &lt;mailet match="All" class="Redirect">
 *   &lt;sender&gt;<I>an address or postmaster</I>&lt;/sender&gt;
 *   &lt;attachError&gt;<I>true or false, default=false</I>&lt;/attachError&gt;
 *   &lt;message&gt;<I><B>dynamically built</B></I>&lt;/message&gt;
 *   &lt;passThrough&gt;true&lt;/passThrough&gt;
 *   &lt;recipients&gt;<B>sender</B>&lt;/recipients&gt;
 *   &lt;returnPath&gt;null&lt;/returnPath&gt;
 *   &lt;inline&gt;none&lt;/inline&gt;
 *   &lt;attachment&gt;message&lt;/attachment&gt;
 *   &lt;isReply&gt;true&lt;/isReply&gt;
 *   &lt;static&gt;true&lt;/static&gt;
 * &lt;/mailet&gt;
 * </CODE></PRE>
 *
 */
public class Bounce extends AbstractNotify {
    
    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Bounce Mailet";
    }

    /* ******************************************************************** */
    /* ****************** Begin of getX and setX methods ****************** */
    /* ******************************************************************** */
    
    /**
     * @return SpecialAddress.RETURN_PATH
     */
    protected Collection getRecipients() {
        Collection newRecipients = new HashSet();
        newRecipients.add(SpecialAddress.RETURN_PATH);
        return newRecipients;
    }
        
    /**
     * @return SpecialAddress.RETURN_PATH
     */
    protected InternetAddress[] getTo() {
        InternetAddress[] apparentlyTo = new InternetAddress[1];
        apparentlyTo[0] = SpecialAddress.RETURN_PATH.toInternetAddress();
        return apparentlyTo;
    }
    
    /**
     * @return NULL (the meaning of bounce)
     */
    protected MailAddress getReturnPath() {
        return SpecialAddress.NULL;
    }
    
    /* ******************************************************************** */
    /* ******************* End of getX and setX methods ******************* */
    /* ******************************************************************** */
    
    /**
     * Service does the hard work,and redirects the originalMail in the form specified.
     * Checks that the original return path is not empty,
     * and then calls super.service(originalMail), otherwise just returns.
     *
     * @param originalMail the mail to process and redirect
     * @throws MessagingException if a problem arises formulating the redirected mail
     */
    public void service(Mail originalMail) throws MessagingException {
        MailAddress returnAddress = getExistingReturnPath(originalMail);
        if (returnAddress == SpecialAddress.NULL) {
            if (isDebug)
                log("Processing a bounce request for a message with an empty return path.  No bounce will be sent.");
            return;
        } else if (returnAddress == SpecialAddress.SENDER) {
            log("WARNING: Mail to be bounced does not contain a Return-Path header.");
        } else {
            if (isDebug)
                log("Processing a bounce request for a message with a return path header.  The bounce will be sent to " + returnAddress);
        }
        super.service(originalMail);
    }
    
}

