/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
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
import java.util.Vector;


import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.apache.james.util.RFC2822Headers;
import org.apache.james.util.RFC822DateFormat;

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
*<LI>getSender(), who the mail is from</LI>
*<LI>getSubjectPrefix(), a prefix to be added to the message subject</LI>
*<LI>getTo(), a list of people to whom the mail is *apparently* sent</LI>
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
*supplied.</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;to&gt;</TD>
*<TD width="80%">A comma delimited list of addresses to appear in the To: header,
*the email will only be delivered to these addresses if they are in the recipients
*list.<BR>
*The recipients list will be used if this is not supplied.</TD>
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
*<TD width="80%">A single email address to appear in the Rely-To: header, can
*also be &quot;sender&quot; or &quot;postmaster&quot;, this header is not
*set if this is omited.</TD>
*</TR>
*<TR>
*<TD width="20%">&lt;prefix&gt;</TD>
*<TD width="80%">An optional subject prefix prepended to the original message
*subject, for example:<BR>
*Undeliverable mail: </TD>
*</TR>
*<TR>
*<TD width="20%">&lt;static&gt;</TD>
*<TD width="80%">
*<P>TRUE or FALSE, if this is true it hints to the mailet that none of the
*parameters are set dynamically, and therefore they can be set once in
*the init method.<BR>
*False tells the mailet to call all the &quot;getters&quot; for every mail
*processed.</P>
*<P>This defaults to false.<BR>
*It should be TRUE in all cases, except where one of the getter methods
*has been overriden to provide dynamic values, such as a listserve which
*might override getRecipients() to get a list from a users repository.</P>
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
 * @author  Danny Angus   <danny@thought.co.uk>
 *
 */
public class Redirect extends GenericMailet {

    /**
     * Controls certain log messages
     */
    private boolean isDebug = false;

    // The values that indicate how to attach the original mail
    // to the redirected mail.

    private static final int UNALTERED           = 0;

    private static final int HEADS               = 1;

    private static final int BODY                = 2;

    private static final int ALL                 = 3;

    private static final int NONE                = 4;

    private static final int MESSAGE             = 5;

    private InternetAddress[] apparentlyTo;
    private String messageText;
    private Collection recipients;
    private MailAddress replyTo;
    private MailAddress sender;

    private RFC822DateFormat rfc822DateFormat = new RFC822DateFormat();

    /**
     *     returns one of these values to indicate how to attach the original message
     *<ul>
     *    <li>BODY : original message body is attached as plain text to the new message</li>
     *    <li>HEADS : original message headers are attached as plain text to the new message</li>
     *    <li>ALL : original is attached as plain text with all headers</li>
     *    <li>MESSAGE : original message is attached as type message/rfc822, a complete mail message.</li>
     *    <li>NONE : original is not attached</li>
     *</ul>
     *
     */
    public int getAttachmentType() {
        if(getInitParameter("attachment") == null) {
            return NONE;
        } else {
            return getTypeCode(getInitParameter("attachment"));
        }
    }

    /**
     * returns one of these values to indicate how to append the original message
     *<ul>
     *    <li>UNALTERED : original message is the new message body</li>
     *    <li>BODY : original message body is appended to the new message</li>
     *    <li>HEADS : original message headers are appended to the new message</li>
     *    <li>ALL : original is appended with all headers</li>
     *    <li>NONE : original is not appended</li>
     *</ul>
     */
    public int getInLineType() {
        if(getInitParameter("inline") == null) {
            return BODY;
        } else {
            return getTypeCode(getInitParameter("inline"));
        }
    }

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Resend Mailet";
    }

    /**
     * must return either an empty string, or a message to which the redirect can be attached/appended
     */
    public String getMessage() {
        if(getInitParameter("message") == null) {
            return "";
        } else {
            return getInitParameter("message");
        }
    }

    /**
     * return true to allow thie original message to continue through the processor, false to GHOST it
     */
    public boolean getPassThrough() {
        if(getInitParameter("passThrough") == null) {
            return false;
        } else {
            return new Boolean(getInitParameter("passThrough")).booleanValue();
        }
    }

    /**
     * must return a Collection of recipient MailAddresses
     */
    public Collection getRecipients() {
        Collection newRecipients           = new HashSet();
        String addressList                 = (getInitParameter("recipients") == null)
                                                 ? getInitParameter("to")
                                                 : getInitParameter("recipients");
        StringTokenizer st                 = new StringTokenizer(addressList, ",", false);
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
     * Returns the reply to address as a string.
     *
     * @return the replyto address for the mail as a string
     */
    public MailAddress getReplyTo() {
        String sr = getInitParameter("replyto");
        if(sr != null) {
            MailAddress rv;
            if(sr.compareTo("postmaster") == 0) {
                rv = getMailetContext().getPostmaster();
                return rv;
            }
            if(sr.compareTo("sender") == 0) {
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
     * returns the senders address, as a MailAddress
     */
    public MailAddress getSender() {
        String sr = getInitParameter("sender");
        if(sr != null) {
            MailAddress rv;
            if(sr.compareTo("postmaster") == 0) {
                rv = getMailetContext().getPostmaster();
                return rv;
            }
            if(sr.compareTo("sender") == 0) {
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
     * return true to reduce calls to getTo, getSender, getRecipients, getReplyTo amd getMessage
     * where these values don't change (eg hard coded, or got at startup from the mailet config)<br>
     * return false where any of these methods generate their results dynamically eg in response to the message being processed,
     * or by refrence to a repository of users
     */
    public boolean isStatic() {
        if(getInitParameter("static") == null) {
            return false;
        }
        return new Boolean(getInitParameter("static")).booleanValue();
    }

    /**
     * return a prefix for the message subject
     */
    public String getSubjectPrefix() {
        if(getInitParameter("prefix") == null) {
            return "";
        } else {
            return getInitParameter("prefix");
        }
    }

    /**
     * returns an array of InternetAddress 'es for the To: header
     */
    public InternetAddress[] getTo() {
        String addressList        = (getInitParameter("to") == null)
                                        ? getInitParameter("recipients") : getInitParameter("to");
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
     * return true to append a description of any error to the main body part
     * if getInlineType does not return "UNALTERED"
     */
    public boolean attachError() {
        if(getInitParameter("attachError") == null) {
            return false;
        } else {
            return new Boolean(getInitParameter("attachError")).booleanValue();
        }
    }

    /**
      * init will setup static values for sender, recipients, message text, and reply to
      * <br> if isStatic() returns true
      * it calls getSender(), getReplyTo(), getMessage(), and getRecipients() and getTo()
      *
      */
    public void init() throws MessagingException {
        if (isDebug) {
            log("Redirect init");
        }
        isDebug = (getInitParameter("debug") == null) ? false : new Boolean(getInitParameter("debug")).booleanValue();
        if(isStatic()) {
            sender       = getSender();
            replyTo      = getReplyTo();
            messageText  = getMessage();
            recipients   = getRecipients();
            apparentlyTo = getTo();
            if (isDebug) {
                StringBuffer logBuffer =
                    new StringBuffer(1024)
                            .append("static, sender=")
                            .append(sender)
                            .append(", replyTo=")
                            .append(replyTo)
                            .append(", message=")
                            .append(messageText)
                            .append(" ");
                log(logBuffer.toString());
            }
        }
    }

    /**
     * Service does the hard work,and redirects the mail in the form specified
     *
     * @param mail the mail to process and redirect
     * @throws MessagingException if a problem arising formulating the redirected mail
     */
    public void service(Mail mail) throws MessagingException {
        if(!isStatic()) {
            sender       = getSender();
            replyTo      = getReplyTo();
            messageText  = getMessage();
            recipients   = getRecipients();
            apparentlyTo = getTo();
        }

        MimeMessage message = mail.getMessage();
        MimeMessage reply = null;
        //Create the message
        if(getInLineType() != UNALTERED) {
            if (isDebug) {
                log("Alter message inline=:" + getInLineType());
            }
            reply = new MimeMessage(Session.getDefaultInstance(System.getProperties(),
                                                               null));
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
            if(messageText != null) {
                out.println(messageText);
            }
            switch(getInLineType()) {
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
            MimeMultipart multipart = new MimeMultipart();
            //Add message as the first mime body part
            MimeBodyPart part       = new MimeBodyPart();
            part.setText(sout.toString());
            part.setDisposition("inline");
            multipart.addBodyPart(part);
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
                part.setDisposition("Attachment");
                multipart.addBodyPart(part);
            }
            reply.setContent(multipart);
            reply.setHeader(RFC2822Headers.CONTENT_TYPE, multipart.getContentType());
            reply.setRecipients(Message.RecipientType.TO, apparentlyTo);
        } else {
            // if we need the original, create a copy of this message to redirect
            reply = getPassThrough() ? new MimeMessage(message) : message;
            if (isDebug) {
                log("Message resent unaltered.");
            }
        }
        //Set additional headers
        reply.setSubject(getSubjectPrefix() + message.getSubject());
        if(reply.getHeader(RFC2822Headers.DATE) == null) {
            reply.setHeader(RFC2822Headers.DATE, rfc822DateFormat.format(new Date()));
        }
        
        //
         
        if(replyTo != null) {
            InternetAddress[] iart = new InternetAddress[1];
            iart[0] = replyTo.toInternetAddress();
            reply.setReplyTo(iart);
        }
        if(sender == null) {
            reply.setHeader(RFC2822Headers.FROM, message.getHeader(RFC2822Headers.FROM, ","));
            sender = new MailAddress(((InternetAddress)message.getFrom()[0]).getAddress());
        } else {
            reply.setFrom(sender.toInternetAddress());
        }
        //Send it off...
        getMailetContext().sendMail(sender, recipients, reply);
        if(!getPassThrough()) {
            mail.setState(Mail.GHOST);
        }
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
}
