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

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.ArrayList;


import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.james.core.MailImpl;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;


/**
 * <P>A mailet providing configurable redirection services.</P>
 * <P>Can produce listserver, forward and notify behaviour, with the original
 * message intact, attached, appended or left out altogether.
 * Can be used as a replacement to {@link Redirect}, having more consistent defaults,
 * and new options available.<BR>
 * Use <CODE>Resend</CODE> if you need full control, <CODE>Redirect</CODE> if
 * the more automatic behaviour of some parameters is appropriate.</P>
 * <P>This built in functionality is controlled by the configuration as laid out below.
 * In the table please note that the parameters controlling message headers
 * accept the <B>&quot;unaltered&quot;</B> value, whose meaning is to keep the associated
 * header unchanged and, unless stated differently, corresponds to the assumed default
 * if the parameter is missing.</P>
 * <P>The configuration parameters are:</P>
 * <TABLE width="75%" border="1" cellspacing="2" cellpadding="2">
 * <TR valign=top>
 * <TD width="20%">&lt;recipients&gt;</TD>
 * <TD width="80%">
 * A comma delimited list of email addresses for recipients of
 * this message;.<BR>
 * It can include constants &quot;sender&quot;, &quot;postmaster&quot;, &quot;reversePath&quot;, &quot;recipients&quot; and &quot;unaltered&quot;.<BR>
 * Default: &quot;unaltered&quot;.
 * </TD>
 * </TR>
 * <TR valign=top>
 * <TD width="20%">&lt;to&gt;</TD>
 * <TD width="80%">
 * A comma delimited list of addresses to appear in the To: header;.<BR>
 * It can include constants &quot;sender&quot;, &quot;postmaster&quot;, &quot;reversePath&quot;, &quot;to&quot;, &quot;null&quot; and &quot;unaltered&quot;;
 * if &quot;null&quot; is specified alone it will remove this header.<BR>
 * Default: &quot;unaltered&quot;.
 * </TD>
 * </TR>
 * <TR valign=top>
 * <TD width="20%">&lt;sender&gt;</TD>
 * <TD width="80%">
 * A single email address to appear in the From: header and become the sender.<BR>
 * It can include constants &quot;sender&quot;, &quot;postmaster&quot; and &quot;unaltered&quot;;
 * &quot;sender&quot; is equivalent to &quot;unaltered&quot;.<BR>
 * Default: &quot;unaltered&quot;.
 * </TD>
 * </TR>
 * <TR valign=top>
 * <TD width="20%">&lt;message&gt;</TD>
 * <TD width="80%">
 * A text message to insert into the body of the email.<BR>
 * Default: no message is inserted.
 * </TD>
 * </TR>
 * <TR valign=top>
 * <TD width="20%">&lt;inline&gt;</TD>
 * <TD width="80%">
 * <P>One of the following items:</P>
 * <UL>
 * <LI>unaltered &nbsp;&nbsp;&nbsp;&nbsp;The original message is the new
 * message, for forwarding/aliasing</LI>
 * <LI>heads&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The
 * headers of the original message are appended to the message</LI>
 * <LI>body&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The
 * body of the original is appended to the new message</LI>
 * <LI>all&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Both
 * headers and body are appended</LI>
 * <LI>none&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Neither
 * body nor headers are appended</LI>
 * </UL>
 * Default: &quot;unaltered&quot;.
 * </TD>
 * </TR>
 * <TR valign=top>
 * <TD width="20%">&lt;attachment&gt;</TD>
 * <TD width="80%">
 * <P>One of the following items:</P>
 * <UL>
 * <LI>heads&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The headers of the original
 * are attached as text</LI>
 * <LI>body&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The body of the original
 * is attached as text</LI>
 * <LI>all&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Both
 * headers and body are attached as a single text file</LI>
 * <LI>none&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Nothing is attached</LI>
 * <LI>message &nbsp;The original message is attached as type message/rfc822,
 * this means that it can, in many cases, be opened, resent, fw'd, replied
 * to etc by email client software.</LI>
 * </UL>
 * Default: &quot;none&quot;.
 * </TD>
 * </TR>
 * <TR valign=top>
 * <TD width="20%">&lt;passThrough&gt;</TD>
 * <TD width="80%">
 * true or false, if true the original message continues in the
 * mailet processor after this mailet is finished. False causes the original
 * to be stopped.<BR>
 * Default: false.
 * </TD>
 * </TR>
 * <TR valign=top>
 * <TD width="20%">&lt;fakeDomainCheck&gt;</TD>
 * <TD width="80%">
 * true or false, if true will check if the sender domain is valid.<BR>
 * Default: true.
 * </TD>
 * </TR>
 * <TR valign=top>
 * <TD width="20%">&lt;attachError&gt;</TD>
 * <TD width="80%">
 * true or false, if true any error message available to the
 * mailet is appended to the message body (except in the case of inline ==
 * unaltered).<BR>
 * Default: false.
 * </TD>
 * </TR>
 * <TR valign=top>
 * <TD width="20%">&lt;replyTo&gt;</TD>
 * <TD width="80%">
 * A single email address to appear in the Reply-To: header.<BR>
 * It can include constants &quot;sender&quot;, &quot;postmaster&quot; &quot;null&quot; and &quot;unaltered&quot;;
 * if &quot;null&quot; is specified it will remove this header.<BR>
 * Default: &quot;unaltered&quot;.
 * </TD>
 * </TR>
 * <TR valign=top>
 * <TD width="20%">&lt;reversePath&gt;</TD>
 * <TD width="80%">
 * A single email address to appear in the Return-Path: header.<BR>
 * It can include constants &quot;sender&quot;, &quot;postmaster&quot; &quot;null&quot; and &quot;unaltered&quot;;
 * if &quot;null&quot; is specified then it will set it to <>, meaning &quot;null return path&quot;.<BR>
 * Default: &quot;unaltered&quot;.
 * </TD>
 * </TR>
 * <TR valign=top>
 * <TD width="20%">&lt;subject&gt;</TD>
 * <TD width="80%">
 * An optional string to use as the subject.<BR>
 * Default: keep the original message subject.
 * </TD>
 * </TR>
 * <TR valign=top>
 * <TD width="20%">&lt;prefix&gt;</TD>
 * <TD width="80%">
 * An optional subject prefix prepended to the original message
 * subject, or to a new subject specified with the <I>&lt;subject&gt;</I> parameter.<BR>
 * For example: <I>[Undeliverable mail]</I>.<BR>
 * Default: &quot;&quot;.
 * </TD>
 * </TR>
 * <TR valign=top>
 * <TD width="20%">&lt;isReply&gt;</TD>
 * <TD width="80%">
 * true or false, if true the IN_REPLY_TO header will be set to the
 * id of the current message.<BR>
 * Default: false.
 * </TD>
 * </TR>
 * <TR valign=top>
 * <TD width="20%">&lt;debug&gt;</TD>
 * <TD width="80%">
 * true or false.  If this is true it tells the mailet to write some debugging
 * information to the mailet log.<BR>
 * Default: false.
 * </TD>
 * </TR>
 * </TABLE>
 *
 * <P>Example:</P>
 * <PRE><CODE>
 *  &lt;mailet match=&quot;RecipientIs=test@localhost&quot; class=&quot;Resend&quot;&gt;
 *    &lt;recipients&gt;x@localhost, y@localhost, z@localhost&lt;/recipients&gt;
 *    &lt;to&gt;list@localhost&lt;/to&gt;
 *    &lt;sender&gt;owner@localhost&lt;/sender&gt;
 *    &lt;message&gt;sent on from James&lt;/message&gt;
 *    &lt;inline&gt;unaltered&lt;/inline&gt;
 *    &lt;passThrough&gt;FALSE&lt;/passThrough&gt;
 *    &lt;replyTo&gt;postmaster&lt;/replyTo&gt;
 *    &lt;prefix xml:space="preserve"&gt;[test mailing] &lt;/prefix&gt;
 *    &lt;!-- note the xml:space="preserve" to preserve whitespace --&gt;
 *    &lt;static&gt;TRUE&lt;/static&gt;
 * &lt;/mailet&gt;
 * </CODE></PRE>
 * 
 * <P>and:</P>
 *
 * <PRE><CODE>
 *  &lt;mailet match=&quot;All&quot; class=&quot;Resend&quot;&gt;
 *    &lt;recipients&gt;x@localhost&lt;/recipients&gt;
 *    &lt;sender&gt;postmaster&lt;/sender&gt;
 *    &lt;message xml:space="preserve"&gt;Message marked as spam:&lt;/message&gt;
 *    &lt;inline&gt;heads&lt;/inline&gt;
 *    &lt;attachment&gt;message&lt;/attachment&gt;
 *    &lt;passThrough&gt;FALSE&lt;/passThrough&gt;
 *    &lt;attachError&gt;TRUE&lt;/attachError&gt;
 *    &lt;replyTo&gt;postmaster&lt;/replyTo&gt;
 *    &lt;prefix&gt;[spam notification]&lt;/prefix&gt;
 *  &lt;/mailet&gt;
 * </CODE></PRE>
 *
 * <P>The following example forwards the message without any modification, based on the defaults:</P>
 * <PRE><CODE>
 *  &lt;mailet match=&quot;All&quot; class=&quot;Resend&quot/;&gt;
 * </CODE></PRE>
 * <P><I>replyto</I> can be used instead of
 * <I>replyTo</I>; such name is kept for backward compatibility.</P>
 * <P><B>WARNING: as the message (or a copy of it) is reinjected in the spool without any modification,
 * the preceding example is very likely to cause a "configuration loop" in your system,
 * unless some other mailet has previously modified something (a header for instance) that could force the resent
 * message follow a different path so that it does not return here unchanged.</B></P>
 *
 * @version CVS $Revision: 1.3 $ $Date: 2003/07/04 16:46:12 $
 * @since 2.2.0
 */

public class Resend extends AbstractRedirect {

    /**
     * Returns a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Redirect Mailet";
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
            "recipients",
            "to",
            "replyTo",
            "replyto",
            "reversePath",
            "sender",
            "subject",
            "prefix",
            "attachError",
            "isReply"
        };
        return allowedArray;
    }

    /* ******************************************************************** */
    /* ****************** Begin of getX and setX methods ****************** */
    /* ******************************************************************** */

    /* ******************************************************************** */
    /* ******************* End of getX and setX methods ******************* */
    /* ******************************************************************** */

}
