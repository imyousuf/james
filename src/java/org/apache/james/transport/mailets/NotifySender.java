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

/**
 * <P>Sends a notification message to the sender of a message.</P>
 * <P>A sender of the notification message can optionally be specified.
 * If one is not specified, the postmaster's address will be used.<BR>
 * The "To:" header of the notification message can be set to "unaltered";
 * if missing will be set to the sender of the notified message.<BR>
 * A notice text can be specified, and in such case will be inserted into the 
 * notification inline text.<BR>
 * If the notified message has an "error message" set, it will be inserted into the 
 * notification inline text. If the <CODE>attachStackTrace</CODE> init parameter
 * is set to true, such error message will be attached to the notification message.<BR>
 * The notified messages are attached in their entirety (headers and
 * content) and the resulting MIME part type is "message/rfc822".</P>
 * <P>Supports the <CODE>passThrough</CODE> init parameter (true if missing).</P>
 *
 * <P>Sample configuration:</P>
 * <PRE><CODE>
 * &lt;mailet match="All" class="NotifySender">
 *   &lt;sender&gt;<I>an address or postmaster or sender or unaltered, default=postmaster</I>&lt;/sender&gt;
 *   &lt;attachError&gt;<I>true or false, default=false</I>&lt;/attachError&gt;
 *   &lt;prefix&gt;<I>optional subject prefix prepended to the original message</I>&lt;/prefix&gt;
 *   &lt;inline&gt;<I>see {@link Resend}, default=none</I>&lt;/inline&gt;
 *   &lt;attachment&gt;<I>see {@link Resend}, default=message</I>&lt;/attachment&gt;
 *   &lt;passThrough&gt;<I>true or false, default=true</I>&lt;/passThrough&gt;
 *   &lt;fakeDomainCheck&gt;<I>true or false, default=true</I>&lt;/fakeDomainCheck&gt;
 *   &lt;to&gt;<I>unaltered (optional, defaults to sender)</I>&lt;/to&gt;
 *   &lt;debug&gt;<I>true or false, default=false</I>&lt;/debug&gt;
 * &lt;/mailet&gt;
 * </CODE></PRE>
 *
 * <P>The behaviour of this mailet is equivalent to using Resend with the following
 * configuration:</P>
 * <PRE><CODE>
 * &lt;mailet match="All" class="Resend">
 *   &lt;sender&gt;<I>an address or postmaster or sender or unaltered</I>&lt;/sender&gt;
 *   &lt;attachError&gt;<I>true or false</I>&lt;/attachError&gt;
 *   &lt;message&gt;<I><B>dynamically built</B></I>&lt;/message&gt;
 *   &lt;prefix&gt;<I>a string</I>&lt;/prefix&gt;
 *   &lt;passThrough&gt;true&lt;/passThrough&gt;
 *   &lt;fakeDomainCheck&gt;<I>true or false</I>&lt;/fakeDomainCheck&gt;
 *   &lt;to&gt;<I>unaltered or sender&lt</I>;/to&gt;
 *   &lt;recipients&gt;<B>sender</B>&lt;/recipients&gt;
 *   &lt;inline&gt;none&lt;/inline&gt;
 *   &lt;attachment&gt;message&lt;/attachment&gt;
 *   &lt;isReply&gt;true&lt;/isReply&gt;
 *   &lt;debug&gt;<I>true or false</I>&lt;/debug&gt;
 * &lt;/mailet&gt;
 * </CODE></PRE>
 * <P><I>notice</I>, <I>senderAddress</I> and <I>attachStackTrace</I> can be used instead of
 * <I><I>message</I>, <I>sender</I> and <I>attachError</I>; such names are kept for backward compatibility.</P>
 *
 * @version CVS $Revision: 1.22 $ $Date: 2003/06/27 14:25:47 $
 */
public class NotifySender extends AbstractNotify {

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "NotifySender Mailet";
    }

    /** Gets the expected init parameters. */
    protected  String[] getAllowedInitParameters() {
        String[] allowedArray = {
//            "static",
            "debug",
            "passThrough",
            "fakeDomainCheck",
            "inline",
            "attachment",
            "message",
            "notice",
            "sendingAddress",
            "prefix",
            "attachError",
            "attachStackTrace",
            "to"
        };
        return allowedArray;
    }
    
    /* ******************************************************************** */
    /* ****************** Begin of getX and setX methods ****************** */
    /* ******************************************************************** */

    /**
     * @return <CODE>SpecialAddress.SENDER</CODE>, indicating the sender of the current mail
     */
    protected Collection getRecipients() {
        Collection newRecipients = new HashSet();
        newRecipients.add(SpecialAddress.SENDER);
        return newRecipients;
    }

    /**
     * @return <CODE>SpecialAddress.UNALTERED</CODE> if specified or <CODE>SpecialAddress.SENDER</CODE> if missing
     */
    protected InternetAddress[] getTo() throws MessagingException {
        String addressList = getInitParameter("to");
        InternetAddress[] iaarray = new InternetAddress[1];
        iaarray[0] = SpecialAddress.SENDER.toInternetAddress();
        if (addressList != null) {
            MailAddress specialAddress = getSpecialAddress(addressList,
                                            new String[] {"sender", "unaltered"});
            if (specialAddress != null) {
                iaarray[0] = specialAddress.toInternetAddress();
            } else {
                log("\"to\" parameter ignored, set to sender");
            }
        }
        return iaarray;
    }

    /* ******************************************************************** */
    /* ******************* End of getX and setX methods ******************* */
    /* ******************************************************************** */
    
}

