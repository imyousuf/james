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

import org.apache.james.util.RFC2822Headers;
import org.apache.james.util.RFC822DateFormat;
import org.apache.james.core.MailImpl;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;


/**
*<P>A mailet providing configurable redirection services<BR>
*This mailet can produce listserver, forward and notify behaviour, with the original
*message intact, attached, appended or left out altogether.<BR>
*This built in functionality is controlled by the configuration as laid out below.</P>
*<P>However it is also designed to be easily subclassed to make authoring redirection
*mailets simple. <BR>
*By extending it and overriding one or more of these methods new behaviour can
*be quickly created without the author having to address any other issue than
*the relevant one:</P>
*<UL>
*<LI>attachError() , should error messages be appended to the message</LI>
*<LI>getAttachmentType(), what should be attached to the message</LI>
*<LI>getInLineType(), what should be included in the message</LI>
*<LI>getMessage(), The text of the message itself</LI>
*<LI>getRecipients(), the recipients the mail is sent to</LI>
*<LI>getReplyTo(), where replys to this message will be sent</LI>
*<LI>getReturnPath(), what to set the Return-Path to</LI>
*<LI>getSender(), who the mail is from</LI>
*<LI>getSubjectPrefix(), a prefix to be added to the message subject</LI>
*<LI>getTo(), a list of people to whom the mail is *apparently* sent</LI>
*<LI>isReply(), should this mailet set the IN_REPLY_TO header to the id of the current message</LI>
*<LI>getPassThrough(), should this mailet allow the original message to continue processing or GHOST it.</LI>
*<LI>isStatic(), should this mailet run the get methods for every mail, or just
*once. </LI>
*</UL>
*<P>The configuration parameters are:</P>
*<TABLE width="75%" border="0" cellspacing="2" cellpadding="2">
*<TR>
*<TD width="20%">&lt;recipients&gt;</TD>
*<TD width="80%">A comma delimited list of email addresses for recipients of
*this message, it will use the &quot;to&quot; list if not specified. These
*addresses will only appear in the To: header if no &quot;to&quot; list is
*supplied.<BR>
*It can include constants &quot;sender&quot; and &quot;postmaster&quot;</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;to&gt;</TD>
*<TD width="80%">A comma delimited list of addresses to appear in the To: header,
*the email will only be delivered to these addresses if they are in the recipients
*list.<BR>
*The recipients list will be used if this is not supplied.<BR>
*It can include constants &quot;sender&quot;, &quot;postmaster&quot; and &quot;unaltered&quot;</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;sender&gt;</TD>
*<TD width="80%">A single email address to appear in the From: header <BR>
*It can include constants &quot;sender&quot; and &quot;postmaster&quot;</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;message&gt;</TD>
*<TD width="80%">A text message to be the body of the email. Can be omitted.</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;inline&gt;</TD>
*<TD width="80%">
*<P>One of the following items:</P>
*<UL>
*<LI>unaltered &nbsp;&nbsp;&nbsp;&nbsp;The original message is the new
* message, for forwarding/aliasing</LI>
*<LI>heads&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The
* headers of the original message are appended to the message</LI>
*<LI>body&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The
* body of the original is appended to the new message</LI>
*<LI>all&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Both
* headers and body are appended</LI>
*<LI>none&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Neither
* body nor headers are appended</LI>
*</UL>
*</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;attachment&gt;</TD>
*<TD width="80%">
*<P>One of the following items:</P>
*<UL>
*<LI>heads&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The headers of the original
* are attached as text</LI>
*<LI>body&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;The body of the original
* is attached as text</LI>
*<LI>all&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Both
* headers and body are attached as a single text file</LI>
*<LI>none&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;Nothing is attached</LI>
*<LI>message &nbsp;The original message is attached as type message/rfc822,
* this means that it can, in many cases, be opened, resent, fw'd, replied
* to etc by email client software.</LI>
*</UL>
*</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;passThrough&gt;</TD>
*<TD width="80%">TRUE or FALSE, if true the original message continues in the
*mailet processor after this mailet is finished. False causes the original
*to be stopped.</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;attachError&gt;</TD>
*<TD width="80%">TRUE or FALSE, if true any error message available to the
*mailet is appended to the message body (except in the case of inline ==
*unaltered)</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;replyto&gt;</TD>
*<TD width="80%">A single email address to appear in the Reply-To: header, can
*also be &quot;sender&quot; or &quot;postmaster&quot;, this header is not
*set if this is omitted.</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;returnPath&gt;</TD>
*<TD width="80%">A single email address to appear in the Return-Path: header, can
*also be &quot;sender&quot; or &quot;postmaster&quot; or &quot;null&quot;; this header is not
*set if this parameter is omitted.</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;prefix&gt;</TD>
*<TD width="80%">An optional subject prefix prepended to the original message
*subject, for example:<BR>
*Undeliverable mail: </TD>
*</TR>
*<TR>
*<TD width="20%">&lt;isReply&gt;</TD>
*<TD width="80%">TRUE or FALSE, if true the IN_REPLY_TO header will be set to the
*id of the current message.</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;static&gt;</TD>
*<TD width="80%">
*<P>TRUE or FALSE.  If this is TRUE it tells the mailet that it can
*reuse all the initial parameters (to, from, etc) without re-calculating
*their values.  This will boost performance where a redirect task
*doesn't contain any dynamic values.  If this is FALSE, it tells the
*mailet to recalculate the values for each e-mail processed.</P>
*<P>This defaults to false.<BR>
*</TD>
*</TR>
*</TABLE>
*
*<P>Example:</P>
*<P> &lt;mailet match=&quot;RecipientIs=test@localhost&quot; class=&quot;Redirect&quot;&gt;<BR>
*&lt;recipients&gt;x@localhost, y@localhost, z@localhost&lt;/recipients&gt;<BR>
*&lt;to&gt;list@localhost&lt;/to&gt;<BR>
*&lt;sender&gt;owner@localhost&lt;/sender&gt;<BR>
*&lt;message&gt;sent on from James&lt;/message&gt;<BR>
*&lt;inline&gt;unaltered&lt;/inline&gt;<BR>
*&lt;passThrough&gt;FALSE&lt;/passThrough&gt;<BR>
*&lt;replyto&gt;postmaster&lt;/replyto&gt;<BR>
*&lt;prefix xml:space="preserve"&gt;[test mailing] &lt;/prefix&gt;<BR>
*&lt;!-- note the xml:space="preserve" to preserve whitespace --&gt;<BR>
*&lt;static&gt;TRUE&lt;/static&gt;<BR>
*&lt;/mailet&gt;<BR>
*</P>
*<P>and:</P>
*<P> &lt;mailet match=&quot;All&quot; class=&quot;Redirect&quot;&gt;<BR>
*&lt;recipients&gt;x@localhost&lt;/recipients&gt;<BR>
*&lt;sender&gt;postmaster&lt;/sender&gt;<BR>
*&lt;message xml:space="preserve"&gt;Message marked as spam:<BR>
*&lt;/message&gt;<BR>
*&lt;inline&gt;heads&lt;/inline&gt;<BR>
*&lt;attachment&gt;message&lt;/attachment&gt;<BR>
*&lt;passThrough&gt;FALSE&lt;/passThrough&gt;<BR>
*&lt;attachError&gt;TRUE&lt;/attachError&gt;<BR>
*&lt;replyto&gt;postmaster&lt;/replyto&gt;<BR>
*&lt;prefix&gt;[spam notification]&lt;/prefix&gt;<BR>
*&lt;static&gt;TRUE&lt;/static&gt;<BR>
*&lt;/mailet&gt;</P>
 *
 *
 */

public class Redirect extends GenericMailet {
            
    /**
     * Controls certain log messages
     */
    protected boolean isDebug = false;

    private static class AddressMarker {
        public static MailAddress SENDER;
        public static MailAddress TO;
        public static MailAddress RECIPIENTS;
        public static MailAddress DELETE;
        public static MailAddress UNALTERED;
        public static MailAddress NULL;

        static {
            try {
                MailAddress SENDER      = new MailAddress("sender","Address.Marker");
                MailAddress TO          = new MailAddress("to","Address.Marker");
                MailAddress RECIPIENTS  = new MailAddress("recipients","Address.Marker");
                MailAddress DELETE      = new MailAddress("delete","Address.Marker");
                MailAddress UNALTERED   = new MailAddress("unaltered","Address.Marker");
                MailAddress NULL        = new MailAddress("null","Address.Marker");

            } catch (Exception _) {}
        }
    }

    protected static class SpecialAddress {
        public static final MailAddress SENDER      = AddressMarker.SENDER;
        public static final MailAddress TO          = AddressMarker.TO;
        public static final MailAddress RECIPIENTS  = AddressMarker.RECIPIENTS;
        public static final MailAddress DELETE      = AddressMarker.DELETE;
        public static final MailAddress UNALTERED   = AddressMarker.UNALTERED;
        public static final MailAddress NULL        = AddressMarker.NULL;
    }

    // The values that indicate how to attach the original mail
    // to the new mail.

    protected static final int UNALTERED        = 0;

    protected static final int HEADS            = 1;

    protected static final int BODY             = 2;

    protected static final int ALL              = 3;

    protected static final int NONE             = 4;

    protected static final int MESSAGE          = 5;

    private boolean isStatic = false;
    
    private int attachmentType = NONE;
    private int inLineType = BODY;
    private String messageText;
    private Collection recipients;
    private MailAddress replyTo;
    private MailAddress returnPath;
    private MailAddress sender;
    private String subjectPrefix;
    private InternetAddress[] apparentlyTo;
    private boolean attachError = false;
    private boolean isReply = false;

    private RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();

    /**
     * Returns a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Resend Mailet";
    }

    /**
     * Gets the <CODE>passThrough</CODE> init parameter.
     *
     * @return true to allow the original message to continue through the processor, false to GHOST it
     */
    protected boolean getPassThrough() {
        if(getInitParameter("passThrough") == null) {
            return false;
        } else {
            return new Boolean(getInitParameter("passThrough")).booleanValue();
        }
    }

    /**
     * Gets the <CODE>static</CODE> init parameter.
     * return true to reduce calls to getTo, getSender, getRecipients, getReplyTo, getReturnPath amd getMessage
     * where these values don't change (eg hard coded, or got at startup from the mailet config)<br>
     * return false where any of these methods generate their results dynamically eg in response to the message being processed,
     * or by reference to a repository of users
     * Is a "getX()" method.
     */
    protected boolean isStatic() {
        return isStatic;
    }

    /**
     * Gets the <CODE>inline</CODE> init parameter.
     * May return one of the following values to indicate how to append the original message
     * to build the new message:
     * <ul>
     *    <li>UNALTERED : original message is the new message body</li>
     *    <li>BODY : original message body is appended to the new message</li>
     *    <li>HEADS : original message headers are appended to the new message</li>
     *    <li>ALL : original is appended with all headers</li>
     *    <li>NONE : original is not appended</li>
     * </ul>
     * Is a "getX()" method.
     *
     * @return the inline type value code
     */
    protected int getInLineType() throws MessagingException {
        if(getInitParameter("inline") == null) {
            return BODY;
        } else {
            return getTypeCode(getInitParameter("inline"));
        }
    }

    /**
     * Gets the <CODE>inline</CODE> init parameter,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getInlineType()}
     */
    protected int getInLineType(Mail originalMail) throws MessagingException {
        int inLineType = (isStatic()) ? this.inLineType : getInLineType();
        return inLineType;
    }

    /**
     * Gets the <CODE>attachment</CODE> init parameter.
     * May return one of the following values to indicate how to attach the original message
     * to the new message:
     * <ul>
     *    <li>BODY : original message body is attached as plain text to the new message</li>
     *    <li>HEADS : original message headers are attached as plain text to the new message</li>
     *    <li>ALL : original is attached as plain text with all headers</li>
     *    <li>MESSAGE : original message is attached as type message/rfc822, a complete mail message.</li>
     *    <li>NONE : original is not attached</li>
     * </ul>
     * Is a "getX()" method.
     *
     * @return the attachmentType value code
     */
    protected int getAttachmentType() throws MessagingException {
        if(getInitParameter("attachment") == null) {
            return NONE;
        } else {
            return getTypeCode(getInitParameter("attachment"));
        }
    }

    /**
     * Gets the <CODE>attachment</CODE> init parameter,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getAttachmentType()}
     */
    protected int getAttachmentType(Mail originalMail) throws MessagingException {
        int attachmentType = (isStatic()) ? this.attachmentType : getAttachmentType();
        return attachmentType;
    }

    /**
     * Gets the <CODE>message</CODE> init parameter.
     * Returns a message to which the original message can be attached/appended
     * to build the new message.
     * Is a "getX()" method.
     *
     * @return the message or an empty string if parameter is missing
     */
    protected String getMessage() throws MessagingException {
        if(getInitParameter("message") == null) {
            return "";
        } else {
            return getInitParameter("message");
        }
    }

    /**
     * Gets the <CODE>message</CODE> init parameter,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getMessage()}
     */
    protected String getMessage(Mail originalMail) throws MessagingException {
        String messageText = (isStatic()) ? this.messageText : getMessage();
        return messageText;
    }
    
    /**
     * Gets the <CODE>recipients</CODE> init parameter.
     * Returns the collection of recipients of the new message.
     * If the <CODE>recipients</CODE> init parameter is missing,
     * returns the <CODE>to</CODE> init parameter.
     * Is a "getX()" method.
     *
     * @return the addresses or SENDER or null if missing
     */
    protected Collection getRecipients() throws MessagingException {
        Collection newRecipients = new HashSet();
        String addressList = (getInitParameter("recipients") == null)
                                 ? getInitParameter("to")
                                 : getInitParameter("recipients");
        // if nothing was specified, return null meaning no change
        if (addressList == null) {
            return null;
        }
        if(addressList.compareTo("postmaster") == 0) {
            newRecipients.add(getMailetContext().getPostmaster());
            return newRecipients;
        }
        if(addressList.compareTo("sender") == 0) {
            newRecipients.add(SpecialAddress.SENDER);
            return newRecipients;
        }
        StringTokenizer st = new StringTokenizer(addressList, ",", false);
        while(st.hasMoreTokens()) {
            try {
                newRecipients.add(new MailAddress(st.nextToken()));
            } catch(Exception e) {
                log("add recipient failed in getRecipients");
            }
        }
        return newRecipients;
    }

    /**
     * Gets the <CODE>recipients</CODE> init parameter,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getRecipients()}, replacing SENDER if applicable
     */
    protected Collection getRecipients(Mail originalMail) throws MessagingException {
        // TODO: implement MailAddress.RETURN_PATH
        Collection recipients = (isStatic()) ? this.recipients : getRecipients();
        if (recipients != null && recipients.size() == 1) {
            if (recipients.contains(SpecialAddress.SENDER)) {
                recipients = new ArrayList();
                recipients.add(originalMail.getSender());
            }
        }
        return recipients;
    }
    
    /**
     * Sets the recipients of <I>newMail</I> to <I>recipients</I>.
     */
    protected void setRecipients(Mail newMail, Collection recipients, Mail originalMail) throws MessagingException {
        if (recipients != null) {
            ((MailImpl) newMail).setRecipients(recipients);
            if (isDebug) {
                log("recipients set to: " + arrayToString(recipients.toArray()));
            }
        }
    }
    
    /**
     * Gets the <CODE>to</CODE> init parameter.
     * Returns the "To:" recipients of the new message.
     * If the <CODE>to</CODE> init parameter is missing,
     * returns the <CODE>recipients</CODE> init parameter.
     * Is a "getX()" method.
     *
     * @return the addresses or SENDER or UNALTERED or null meaning no change
     */
    protected InternetAddress[] getTo() throws MessagingException {
        String addressList = (getInitParameter("to") == null)
                                 ? getInitParameter("recipients")
                                 : getInitParameter("to");
        // if nothing was specified, return null meaning no change
        if (addressList == null) {
            return null;
        }
        if(addressList.compareTo("postmaster") == 0) {
            InternetAddress[] iaarray = new InternetAddress[1];
            iaarray[0] = getMailetContext().getPostmaster().toInternetAddress();
            return iaarray;
        }
        if(addressList.compareTo("sender") == 0) {
            InternetAddress[] iaarray = new InternetAddress[1];
            iaarray[0] = SpecialAddress.SENDER.toInternetAddress();
            return iaarray;
        }
        if(addressList.compareTo("unaltered") == 0) {
            InternetAddress[] iaarray = new InternetAddress[1];
            iaarray[0] = SpecialAddress.UNALTERED.toInternetAddress();
            return iaarray;
        }
        StringTokenizer rec       = new StringTokenizer(addressList, ",");
        int tokensn               = rec.countTokens();
        InternetAddress[] iaarray = new InternetAddress[tokensn];
        String tokenx             = "";
        for(int i = 0; i < tokensn; ++i) {
            try {
                tokenx     = rec.nextToken();
                iaarray[i] = new InternetAddress(tokenx);
            } catch(Exception e) {
                log("Internet address exception in getTo()");
            }
        }
        return iaarray;
    }

    /**
     * Gets the <CODE>to</CODE> init parameter,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getTo()}, replacing SENDER and UNALTERED if applicable
     */
    protected InternetAddress[] getTo(Mail originalMail) throws MessagingException {
        InternetAddress[] apparentlyTo = (isStatic()) ? this.apparentlyTo : getTo();
        if (apparentlyTo != null && apparentlyTo.length == 1) {
            if (apparentlyTo[0].equals(SpecialAddress.SENDER.toInternetAddress())) {
                apparentlyTo = new InternetAddress[1];
                apparentlyTo[0] = originalMail.getSender().toInternetAddress();
            } else if (apparentlyTo[0].equals(SpecialAddress.UNALTERED.toInternetAddress())) {
                apparentlyTo = (InternetAddress[]) originalMail.getMessage().getRecipients(Message.RecipientType.TO);
            }
        }
        return apparentlyTo;
    }
    
    /**
     * Sets the "To:" header of <I>newMail</I> to <I>to</I>.
     */
    protected void setTo(Mail newMail, InternetAddress[] to, Mail originalMail) throws MessagingException {
        if (to != null) {
            newMail.getMessage().setRecipients(Message.RecipientType.TO, to);
            if (isDebug) {
                log("apparentlyTo set to: " + arrayToString(to));
            }
        }
    }
    
    /**
     * Gets the <CODE>replyto</CODE> init parameter.
     * Returns the Reply-To address of the new message.
     * Is a "getX()" method.
     *
     * @return an address or null if parameter is missing or == "sender" (null means "use original")
     */
    protected MailAddress getReplyTo() throws MessagingException {
        String sr = getInitParameter("replyto");
        if(sr != null) {
            MailAddress rv;
            if(sr.compareTo("postmaster") == 0) {
                rv = getMailetContext().getPostmaster();
                return rv;
            }
            if(sr.compareTo("sender") == 0) {
                // means no change
                return null;
            }
            try {
                rv = new MailAddress(sr);
                return rv;
            } catch(Exception e) {
                log("Parse error in getReplyTo " + sr);
            }
        }
        return null;
    }

    /**
     * Gets the <CODE>replyTo</CODE> init parameter,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getReplyTo()}
     */
    protected MailAddress getReplyTo(Mail originalMail) throws MessagingException {
        MailAddress replyTo = (isStatic()) ? this.replyTo : getReplyTo();
        return replyTo;
    }
    
    /**
     * Sets the "Reply-To:" header of <I>newMail</I> to <I>replyTo</I>.
     */
    protected void setReplyTo(Mail newMail, MailAddress replyTo, Mail originalMail) throws MessagingException {
        if(replyTo != null) {
            InternetAddress[] iart = new InternetAddress[1];
            iart[0] = replyTo.toInternetAddress();
            newMail.getMessage().setReplyTo(iart);
            if (isDebug) {
                log("replyTo set to: " + replyTo);
            }
        }
    }
    
    /**
     * Gets the <CODE>returnPath</CODE> init parameter.
     * Returns the Return-Path of the new message.
     * Is a "getX()" method.
     *
     * @return an address or NULL or SENDER or null if parameter is missing (null means "use original")
     */
    protected MailAddress getReturnPath() throws MessagingException {
        String sr = getInitParameter("returnPath");
        if(sr != null) {
            MailAddress rv;
            if(sr.compareTo("postmaster") == 0) {
                return getMailetContext().getPostmaster();
            }
            if(sr.compareTo("NULL") == 0) {
                return SpecialAddress.NULL;
            }
            if(sr.compareTo("sender") == 0) {
                return SpecialAddress.SENDER;
            }
            try {
                rv = new MailAddress(sr);
                return rv;
            } catch(Exception e) {
                log("Parse error in getReturnPath " + sr);
            }
        }
        return null;
    }

    /**
     * Gets the <CODE>returnPath</CODE> init parameter,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getReturnPath()}, replacing SENDER if applicable, but not replacing NULL
     */
    protected MailAddress getReturnPath(Mail originalMail) throws MessagingException {
        MailAddress returnPath = (isStatic()) ? this.returnPath : getReturnPath();
        if (returnPath != null) {
            if (returnPath == SpecialAddress.SENDER) {
                returnPath = originalMail.getSender();
            }
        }
        return returnPath;
    }
    
    /**
     * Sets the "Return-Path:" header of <I>newMail</I> to <I>returnPath</I>.
     */
    protected void setReturnPath(Mail newMail, MailAddress returnPath, Mail originalMail) throws MessagingException {
        if(returnPath != null) {
            String returnPathString;
            if (returnPath == SpecialAddress.NULL) {
                returnPathString = "";
            } else {
                returnPathString = returnPath.toString();
            }
            newMail.getMessage().setHeader(RFC2822Headers.RETURN_PATH, "<" + returnPathString + ">");
            if (isDebug) {
                log("returnPath set to: " + returnPath);
            }
        }
    }
    
    /**
     * Gets the <CODE>sender</CODE> init parameter.
     * Returns the new sender as a MailAddress.
     * Is a "getX()" method.
     *
     * @return an address or null if parameter is missing or == "sender", meaning "use original"
     */
    protected MailAddress getSender() throws MessagingException {
        String sr = getInitParameter("sender");
        if(sr != null) {
            MailAddress rv;
            if(sr.compareTo("postmaster") == 0) {
                rv = getMailetContext().getPostmaster();
                return rv;
            }
            if(sr.compareTo("sender") == 0) {
                // means no change: use FROM header; kept as is for compatibility
                return null;
            }
            try {
                rv = new MailAddress(sr);
                return rv;
            } catch(Exception e) {
                log("Parse error in getSender " + sr);
            }
        }
        return null;
    }

    /**
     * Gets the <CODE>sender</CODE> init parameter,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getSender()}
     */
    protected MailAddress getSender(Mail originalMail) throws MessagingException {
        MailAddress sender = (isStatic()) ? this.sender : getSender();
        return sender;
    }
    
    /**
     * Sets the sender and the "From:" header of <I>newMail</I> to <I>sender</I>.
     * If sender is null will set such values to the ones in <I>originalMail</I>.
     */
    protected void setSender(Mail newMail, MailAddress sender, Mail originalMail) throws MessagingException {
        if (sender == null) {
            MailAddress originalSender = new MailAddress(((InternetAddress) originalMail.getMessage().getFrom()[0]).getAddress());
            newMail.getMessage().setHeader(RFC2822Headers.FROM, originalMail.getMessage().getHeader(RFC2822Headers.FROM, ","));
            ((MailImpl) newMail).setSender(originalSender);
        } else {
            newMail.getMessage().setFrom(sender.toInternetAddress());
            ((MailImpl) newMail).setSender(sender);
            if (isDebug) {
                log("sender set to: " + sender);
            }
        }
    }
    
    /**
     * Gets the <CODE>prefix</CODE> init parameter.
     * Returns a prefix for the new message subject.
     * Is a "getX()" method.
     *
     * @return the prefix or an empty string if parameter is missing
     */
    protected String getSubjectPrefix() throws MessagingException {
        if(getInitParameter("prefix") == null) {
            return "";
        } else {
            return getInitParameter("prefix");
        }
    }

    /**
     * Gets the <CODE>subjectPrefix</CODE> init parameter,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getSubjectPrefix()}
     */
    protected String getSubjectPrefix(Mail originalMail) throws MessagingException {
        String subjectPrefix = (isStatic()) ? this.subjectPrefix : getSubjectPrefix();
        return subjectPrefix;
    }
    
    /**
     * Builds the subject of <I>newMail</I> appending the subject
     * of <I>originalMail</I> to <I>subjectPrefix</I>.
     */
    protected void setSubjectPrefix(Mail newMail, String subjectPrefix, Mail originalMail) throws MessagingException {
        String subject = originalMail.getMessage().getSubject();
        if (subject == null) {
            subject = "";
        }
        newMail.getMessage().setSubject(subjectPrefix + subject);
        if (isDebug) {
            log("subjectPrefix set to: " + subjectPrefix);
        }
    }
    
    /**
     * Gets the <CODE>attachError</CODE> init parameter.
     * Returns a boolean indicating whether to append a description of any error to the main body part
     * of the new message, if getInlineType does not return "UNALTERED".
     * Is a "getX()" method.
     *
     * @return true or false; false if init parameter missing
     */
    protected boolean attachError() throws MessagingException {
        if(getInitParameter("attachError") == null) {
            return false;
        } else {
            return new Boolean(getInitParameter("attachError")).booleanValue();
        }
    }

    /**
     * Gets the <CODE>attachError</CODE> init parameter,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #attachError()}
     */
    protected boolean attachError(Mail originalMail) throws MessagingException {
        boolean attachError = (isStatic()) ? this.attachError : attachError();
        return attachError;
    }

    /**
     * Gets the <CODE>isReply</CODE> init parameter.
     * Returns a boolean indicating whether the new message must be considered
     * a reply to the original message, setting the IN_REPLY_TO header of the new
     * message to the id of the original message.
     * Is a "getX()" method.
     *
     * @return true or false; false if init parameter missing
     */
    protected boolean isReply() throws MessagingException {
        if(getInitParameter("isReply") == null) {
            return false;
        }
        return new Boolean(getInitParameter("isReply")).booleanValue();
    }

    /**
     * Gets the <CODE>isReply</CODE> init parameter,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #isReply()}
     */
    protected boolean isReply(Mail originalMail) throws MessagingException {
        boolean isReply = (isStatic()) ? this.isReply : isReply();
        return isReply;
    }
    
    /**
     * Sets the "In-Reply-To:" header of <I>newMail</I> to the "Message-Id:" of
     * <I>originalMail</I>, if <I>isReply</I> is true.
     */
    protected void setIsReply(Mail newMail, boolean isReply, Mail originalMail) throws MessagingException {
        if (isReply) {
            String messageId = originalMail.getMessage().getMessageID();
            if (messageId != null) {
                newMail.getMessage().setHeader(RFC2822Headers.IN_REPLY_TO, messageId);
                if (isDebug) {
                    log("IN_REPLY_TO set to: " + messageId);
                }
            }
        }
    }
    
    /**
     * Mailet initialization routine.
     * Will setup static values for each "x" initialization parameter in config.xml,
     * using getX(), if isStatic() returns true.
     *
     */
    public void init() throws MessagingException {
        if (isDebug) {
            log("Redirect init");
        }
        isDebug = (getInitParameter("debug") == null) ? false : new Boolean(getInitParameter("debug")).booleanValue();
        
        isStatic = (getInitParameter("static") == null) ? false : new Boolean(getInitParameter("static")).booleanValue();

        if(isStatic()) {
            attachmentType  = getAttachmentType();
            inLineType      = getInLineType();
            messageText     = getMessage();
            recipients      = getRecipients();
            replyTo         = getReplyTo();
            returnPath      = getReturnPath();
            sender          = getSender();
            subjectPrefix   = getSubjectPrefix();
            apparentlyTo    = getTo();
            attachError     = attachError();
            isReply         = isReply();
            if (isDebug) {
                StringBuffer logBuffer =
                    new StringBuffer(1024)
                            .append("static, sender=").append(sender)
                            .append(", replyTo=").append(replyTo)
                            .append(", returnPath=").append(returnPath)
                            .append(", message=").append(messageText)
                            .append(", recipients=").append(arrayToString(recipients.toArray()))
                            .append(", subjectPrefix=").append(subjectPrefix)
                            .append(", apparentlyTo=").append(arrayToString(apparentlyTo))
                            .append(", attachError=").append(attachError)
                            .append(", isReply=").append(isReply)
                            .append(", attachmentType=").append(attachmentType)
                            .append(", inLineType=").append(inLineType)
                            .append(" ");
                log(logBuffer.toString());
            }
        }
    }

    /**
     * Service does the hard work,and redirects the originalMail in the form specified.
     *
     * @param originalMail the mail to process and redirect
     * @throws MessagingException if a problem arises formulating the redirected mail
     */
    public void service(Mail originalMail) throws MessagingException {
        
        boolean keepMessageId = false;

        // duplicates the Mail object, to be able to modify the new mail keeping the original untouched
        Mail newMail = ((MailImpl) originalMail).duplicate(newName((MailImpl)originalMail));
        
        if (isDebug) {
            MailImpl newMailImpl = (MailImpl) newMail;
            log("New mail - sender: " + newMailImpl.getSender()
                       + ", recipients: " + arrayToString(newMailImpl.getRecipients().toArray())
                       + ", name: " + newMailImpl.getName()
                       + ", remoteHost: " + newMailImpl.getRemoteHost()
                       + ", remoteAddr: " + newMailImpl.getRemoteAddr()
                       + ", state: " + newMailImpl.getState()
                       + ", lastUpdated: " + newMailImpl.getLastUpdated()
                       + ", errorMessage: " + newMailImpl.getErrorMessage());
        }
        
        //Create the message
        if(getInLineType(originalMail) != UNALTERED) {
            if (isDebug) {
                log("Alter message inline=:" + getInLineType(originalMail));
            }
            newMail.setMessage(new MimeMessage(Session.getDefaultInstance(System.getProperties(),
                                                               null)));
            
            // handle the new message if altered
            buildAlteredMessage(newMail, originalMail);
            
            setTo(newMail, getTo(originalMail), originalMail);
        
        } else {
            // if we need the original, create a copy of this message to redirect
            if (getPassThrough()) {
                newMail.setMessage(new MimeMessage(originalMail.getMessage()));
            }
            if (isDebug) {
                log("Message resent unaltered.");
            }
            keepMessageId = true;
        }
        
        //Set additional headers
        
        setRecipients(newMail, getRecipients(originalMail), originalMail);
        
        setSubjectPrefix(newMail, getSubjectPrefix(originalMail), originalMail);
        
        if(newMail.getMessage().getHeader(RFC2822Headers.DATE) == null) {
            newMail.getMessage().setHeader(RFC2822Headers.DATE, rfc822DateFormat.format(new Date()));
        }
        
        setReplyTo(newMail, getReplyTo(originalMail), originalMail);

        setReturnPath(newMail, getReturnPath(originalMail), originalMail);

        setSender(newMail, getSender(originalMail), originalMail);
        
        setIsReply(newMail, isReply(originalMail), originalMail);

        newMail.getMessage().saveChanges();
        
        if (keepMessageId) {
            setMessageId(newMail, originalMail);
        }

        //Send it off...
        getMailetContext().sendMail(newMail);
        
        if(!getPassThrough()) {
            originalMail.setState(Mail.GHOST);
        }
    }

    private static final java.util.Random random = new java.util.Random();  // Used to generate new mail names
    /**
     * Create a unique new primary key name.
     *
     * @param mail the mail to use as the basis for the new mail name
     * 
     * @return a new name
     */
    private String newName(MailImpl mail) {
        StringBuffer nameBuffer =
                                 new StringBuffer(64)
                                 .append(mail.getName())
                                 .append("-!")
                                 .append(random.nextInt(1048576));
        return nameBuffer.toString();
    }

    /**
     * A private method to convert types from string to int.
     *
     * @param param the string type
     *
     * @return the corresponding int enumeration
     */
    private int getTypeCode(String param) {
        int code;
        param = param.toLowerCase(Locale.US);
        if(param.compareTo("unaltered") == 0) {
            return UNALTERED;
        }
        if(param.compareTo("heads") == 0) {
            return HEADS;
        }
        if(param.compareTo("body") == 0) {
            return BODY;
        }
        if(param.compareTo("all") == 0) {
            return ALL;
        }
        if(param.compareTo("none") == 0) {
            return NONE;
        }
        if(param.compareTo("message") == 0) {
            return MESSAGE;
        }
        return NONE;
    }

    /**
     * Utility method for obtaining a string representation of an array of Objects.
     */
    private String arrayToString(Object[] array) {
        StringBuffer sb = new StringBuffer(1024);
        sb.append("[");
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Builds the message of the newMail in case it has to be altered.
     *
     * @param originalMail the original Mail object
     * @param newMail the Mail object to build
     */
    protected void buildAlteredMessage(Mail newMail, Mail originalMail) throws MessagingException {

        MimeMessage message = originalMail.getMessage();
        
        StringWriter sout = new StringWriter();
        PrintWriter out   = new PrintWriter(sout, true);
        Enumeration heads = message.getAllHeaderLines();
        String head       = "";
        StringBuffer headBuffer = new StringBuffer(1024);
        while(heads.hasMoreElements()) {
            headBuffer.append(heads.nextElement().toString()).append("\n");
        }
        head = headBuffer.toString();
        boolean all = false;
        
        String messageText = getMessage(originalMail);
        if(messageText != null) {
            out.println(messageText);
        }
        
        switch(getInLineType(originalMail)) {
            case ALL: //ALL:
                all = true;
            case HEADS: //HEADS:
                out.println("Message Headers:");
                out.println(head);
                if(!all) {
                    break;
                }
            case BODY: //BODY:
                out.println("Message:");
                try {
                    out.println(message.getContent().toString());
                } catch(Exception e) {
                    out.println("body unavailable");
                }
                break;
            default:
            case NONE: //NONE:
                break;
        }

        try {
            //Create the message body
            MimeMultipart multipart = new MimeMultipart("mixed");

            // Create the message
            MimeMultipart mpContent = new MimeMultipart("alternative");
            MimeBodyPart contentPartRoot = new MimeBodyPart();
            contentPartRoot.setContent(mpContent);

            multipart.addBodyPart(contentPartRoot);

            MimeBodyPart part = new MimeBodyPart();
            part.setText(sout.toString());
            part.setDisposition("inline");
            mpContent.addBodyPart(part);
            if(getAttachmentType() != NONE) {
                part = new MimeBodyPart();
                switch(getAttachmentType()) {
                    case HEADS: //HEADS:
                        part.setText(head);
                        break;
                    case BODY: //BODY:
                        try {
                            part.setText(message.getContent().toString());
                        } catch(Exception e) {
                            part.setText("body unavailable");
                        }
                        break;
                    case ALL: //ALL:
                        StringBuffer textBuffer =
                            new StringBuffer(1024)
                                .append(head)
                                .append("\n\n")
                                .append(message.toString());
                        part.setText(textBuffer.toString());
                        break;
                    case MESSAGE: //MESSAGE:
                        part.setContent(message, "message/rfc822");
                        break;
                }
                if ((message.getSubject() != null) && (message.getSubject().trim().length() > 0)) {
                    part.setFileName(message.getSubject().trim());
                } else {
                    part.setFileName("No Subject");
                }
                part.setDisposition("Attachment");
                multipart.addBodyPart(part);
            }
            //if set, attach the full stack trace
            if (attachError(originalMail) && originalMail.getErrorMessage() != null) {
                part = new MimeBodyPart();
                part.setContent(originalMail.getErrorMessage(), "text/plain");
                part.setHeader(RFC2822Headers.CONTENT_TYPE, "text/plain");
                part.setFileName("Reasons");
                part.setDisposition(javax.mail.Part.ATTACHMENT);
                multipart.addBodyPart(part);
            }
            newMail.getMessage().setContent(multipart);
            newMail.getMessage().setHeader(RFC2822Headers.CONTENT_TYPE, multipart.getContentType());
            
        } catch (Exception ioe) {
            throw new MessagingException("Unable to create multipart body", ioe);
        }
    }
    
    /**
     * Sets the message id of originalMail into newMail.
     */
    private void setMessageId(Mail newMail, Mail originalMail) throws MessagingException {
        String messageId = originalMail.getMessage().getMessageID();
        if (messageId != null) {
            newMail.getMessage().setHeader(RFC2822Headers.MESSAGE_ID, messageId);
            if (isDebug) {
                log("MESSAGE_ID restored to: " + messageId);
            }
        }
    }
}
