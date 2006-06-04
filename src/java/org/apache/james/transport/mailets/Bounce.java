/***********************************************************************
 * Copyright (c) 2000-2006 The Apache Software Foundation.             *
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

import java.util.Collection;
import java.util.HashSet;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;

import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;


/**
 * <P>Generates a response to the reverse-path address.
 * Note that this is different than a mail-client's
 * reply, which would use the Reply-To or From header.</P>
 * <P>Bounced messages are attached in their entirety (headers and
 * content) and the resulting MIME part type is "message/rfc822".<BR>
 * The reverse-path and the Return-Path header of the response is set to "null" ("<>"),
 * meaning that no reply should be sent.</P>
 * <P>A sender of the notification message can optionally be specified.
 * If one is not specified, the postmaster's address will be used.<BR>
 * A notice text can be specified, and in such case will be inserted into the
 * notification inline text.<BR>
 * If the notified message has an "error message" set, it will be inserted into the
 * notification inline text. If the <CODE>attachStackTrace</CODE> init parameter
 * is set to true, such error message will be attached to the notification message.<BR>
 * <P>Supports the <CODE>passThrough</CODE> init parameter (true if missing).</P>
 *
 * <P>Sample configuration:</P>
 * <PRE><CODE>
 * &lt;mailet match="All" class="Bounce">
 *   &lt;sender&gt;<I>an address or postmaster or sender or unaltered, default=postmaster</I>&lt;/sender&gt;
 *   &lt;attachError&gt;<I>true or false, default=false</I>&lt;/attachError&gt;
 *   &lt;message&gt;<I>notice attached to the original message text (optional)</I>&lt;/message&gt;
 *   &lt;prefix&gt;<I>optional subject prefix prepended to the original message</I>&lt;/prefix&gt;
 *   &lt;inline&gt;<I>see {@link Resend}, default=none</I>&lt;/inline&gt;
 *   &lt;attachment&gt;<I>see {@link Resend}, default=message</I>&lt;/attachment&gt;
 *   &lt;passThrough&gt;<I>true or false, default=true</I>&lt;/passThrough&gt;
 *   &lt;fakeDomainCheck&gt;<I>true or false, default=true</I>&lt;/fakeDomainCheck&gt;
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
 *   &lt;passThrough&gt;true or false&lt;/passThrough&gt;
 *   &lt;fakeDomainCheck&gt;<I>true or false</I>&lt;/fakeDomainCheck&gt;
 *   &lt;recipients&gt;<B>sender</B>&lt;/recipients&gt;
 *   &lt;reversePath&gt;null&lt;/reversePath&gt;
 *   &lt;inline&gt;see {@link Resend}&lt;/inline&gt;
 *   &lt;attachment&gt;see {@link Resend}&lt;/attachment&gt;
 *   &lt;isReply&gt;true&lt;/isReply&gt;
 *   &lt;debug&gt;<I>true or false</I>&lt;/debug&gt;
 * &lt;/mailet&gt;
 * </CODE></PRE>
 * <P><I>notice</I> and <I>sendingAddress</I> can be used instead of
 * <I>message</I> and <I>sender</I>; such names are kept for backward compatibility.</P>
 *
 * @version CVS $Revision$ $Date$
 * @since 2.2.0
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
            "sender",
            "sendingAddress",
            "prefix",
            "attachError",
        };
        return allowedArray;
    }
    
    /* ******************************************************************** */
    /* ****************** Begin of getX and setX methods ****************** */
    /* ******************************************************************** */

    /**
     * @return <CODE>SpecialAddress.REVERSE_PATH</CODE>
     */
    protected Collection getRecipients() {
        Collection newRecipients = new HashSet();
        newRecipients.add(SpecialAddress.REVERSE_PATH);
        return newRecipients;
    }

    /**
     * @return <CODE>SpecialAddress.REVERSE_PATH</CODE>
     */
    protected InternetAddress[] getTo() {
        InternetAddress[] apparentlyTo = new InternetAddress[1];
        apparentlyTo[0] = SpecialAddress.REVERSE_PATH.toInternetAddress();
        return apparentlyTo;
    }

    /**
     * @return <CODE>SpecialAddress.NULL</CODE> (the meaning of bounce)
     */
    protected MailAddress getReversePath(Mail originalMail) {
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
        if (originalMail.getSender() == null) {
            if (isDebug)
                log("Processing a bounce request for a message with an empty reverse-path.  No bounce will be sent.");
            if(!getPassThrough(originalMail)) {
                originalMail.setState(Mail.GHOST);
            }
            return;
        }

        if (isDebug)
            log("Processing a bounce request for a message with a reverse path.  The bounce will be sent to " + originalMail.getSender().toString());

        super.service(originalMail);
    }

}

