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
 * <P>A mailet providing configurable redirection services<BR>
 * This mailet can produce listserver, forward and notify behaviour, with the original
 * message intact, attached, appended or left out altogether.<BR>
 * This built in functionality is controlled by the configuration as laid out below.</P>
 * <P>The configuration parameters are:</P>
 * <TABLE width="75%" border="0" cellspacing="2" cellpadding="2">
 * <TR>
 * <TD width="20%">&lt;recipients&gt;</TD>
 * <TD width="80%">A comma delimited list of email addresses for recipients of
 * this message, it will use the &quot;to&quot; list if not specified. These
 * addresses will only appear in the To: header if no &quot;to&quot; list is
 * supplied.<BR>
 * It can include constants &quot;sender&quot;, &quot;postmaster&quot; and &quot;returnPath&quot;</TD>
 * </TR>
 * <TR>
 * <TD width="20%">&lt;to&gt;</TD>
 * <TD width="80%">A comma delimited list of addresses to appear in the To: header,
 * the email will only be delivered to these addresses if they are in the recipients
 * list.<BR>
 * The recipients list will be used if this is not supplied.<BR>
 * It can include constants &quot;sender&quot;, &quot;postmaster&quot;, &quot;returnPath&quot; and &quot;unaltered&quot;</TD>
 * </TR>
 * <TR>
 * <TD width="20%">&lt;sender&gt;</TD>
 * <TD width="80%">A single email address to appear in the From: header <BR>
 * It can include constants &quot;sender&quot; and &quot;postmaster&quot;</TD>
 * </TR>
 * <TR>
 * <TD width="20%">&lt;message&gt;</TD>
 * <TD width="80%">A text message to be the body of the email. Can be omitted.</TD>
 * </TR>
 * <TR>
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
 * </TD>
 * </TR>
 * <TR>
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
 * </TD>
 * </TR>
 * <TR>
 * <TD width="20%">&lt;passThrough&gt;</TD>
 * <TD width="80%">true or false, if true the original message continues in the
 * mailet processor after this mailet is finished. False causes the original
 * to be stopped. The default is false.</TD>
 * </TR>
 * <TR>
 * <TD width="20%">&lt;fakeDomainCheck&gt;</TD>
 * <TD width="80%">TRUE or FALSE, if true will check if the sender domain is valid.
 * The default is true.</TD>
 * </TR>
 * <TR>
 * <TD width="20%">&lt;attachError&gt;</TD>
 * <TD width="80%">TRUE or FALSE, if true any error message available to the
 * mailet is appended to the message body (except in the case of inline ==
 * unaltered)</TD>
 * </TR>
 * <TR>
 * <TD width="20%">&lt;replyto&gt;</TD>
 * <TD width="80%">A single email address to appear in the Reply-To: header, can
 * also be &quot;sender&quot; or &quot;postmaster&quot;, this header is not
 * set if this is omitted.</TD>
 * </TR>
 * <TR>
 * <TD width="20%">&lt;returnPath&gt;</TD>
 * <TD width="80%">A single email address to appear in the Return-Path: header, can
 * also be &quot;sender&quot; or &quot;postmaster&quot; or &quot;null&quot;; this header is not
 * set if this parameter is omitted.</TD>
 * </TR>
 * <TR>
 * <TD width="20%">&lt;prefix&gt;</TD>
 * <TD width="80%">An optional subject prefix prepended to the original message
 * subject, for example:<BR>
 * Undeliverable mail: </TD>
 * </TR>
 * <TR>
 * <TD width="20%">&lt;isReply&gt;</TD>
 * <TD width="80%">TRUE or FALSE, if true the IN_REPLY_TO header will be set to the
 * id of the current message.</TD>
 * </TR>
 * <TR>
 * <TD width="20%">&lt;static&gt;</TD>
 * <TD width="80%">
 * <P>TRUE or FALSE.  If this is TRUE it tells the mailet that it can
 * reuse all the initial parameters (to, from, etc) without re-calculating
 * their values.  This will boost performance where a redirect task
 * doesn't contain any dynamic values.  If this is FALSE, it tells the
 * mailet to recalculate the values for each e-mail processed.</P>
 * <P>This defaults to false.<BR>
 * </TD>
 * </TR>
 * </TABLE>
 *
 * <P>Example:</P>
 * <P> &lt;mailet match=&quot;RecipientIs=test@localhost&quot; class=&quot;Redirect&quot;&gt;<BR>
 * &lt;recipients&gt;x@localhost, y@localhost, z@localhost&lt;/recipients&gt;<BR>
 * &lt;to&gt;list@localhost&lt;/to&gt;<BR>
 * &lt;sender&gt;owner@localhost&lt;/sender&gt;<BR>
 * &lt;message&gt;sent on from James&lt;/message&gt;<BR>
 * &lt;inline&gt;unaltered&lt;/inline&gt;<BR>
 * &lt;passThrough&gt;FALSE&lt;/passThrough&gt;<BR>
 * &lt;replyto&gt;postmaster&lt;/replyto&gt;<BR>
 * &lt;prefix xml:space="preserve"&gt;[test mailing] &lt;/prefix&gt;<BR>
 * &lt;!-- note the xml:space="preserve" to preserve whitespace --&gt;<BR>
 * &lt;static&gt;TRUE&lt;/static&gt;<BR>
 * &lt;/mailet&gt;<BR>
 * </P>
 * <P>and:</P>
 * <P> &lt;mailet match=&quot;All&quot; class=&quot;Redirect&quot;&gt;<BR>
 * &lt;recipients&gt;x@localhost&lt;/recipients&gt;<BR>
 * &lt;sender&gt;postmaster&lt;/sender&gt;<BR>
 * &lt;message xml:space="preserve"&gt;Message marked as spam:<BR>
 * &lt;/message&gt;<BR>
 * &lt;inline&gt;heads&lt;/inline&gt;<BR>
 * &lt;attachment&gt;message&lt;/attachment&gt;<BR>
 * &lt;passThrough&gt;FALSE&lt;/passThrough&gt;<BR>
 * &lt;attachError&gt;TRUE&lt;/attachError&gt;<BR>
 * &lt;replyto&gt;postmaster&lt;/replyto&gt;<BR>
 * &lt;prefix&gt;[spam notification]&lt;/prefix&gt;<BR>
 * &lt;static&gt;TRUE&lt;/static&gt;<BR>
 * &lt;/mailet&gt;</P>
 *
 * <P>CVS $Id: Redirect.java,v 1.29 2003/06/15 18:44:03 noel Exp $</P>
 * @version 2.2.0
 */

public class Redirect extends AbstractRedirect {

    /**
     * Returns a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Redirect Mailet";
    }

    /* ******************************************************************** */
    /* ****************** Begin of getX and setX methods ****************** */
    /* ******************************************************************** */

    /**
     * @return the <CODE>static</CODE> init parameter
    */
    protected boolean isStatic() {
        return isStatic;
    }

    /**
     * @return the <CODE>inline</CODE> init parameter
     */
    protected int getInLineType() throws MessagingException {
        if(getInitParameter("inline") == null) {
            return BODY;
        } else {
            return getTypeCode(getInitParameter("inline"));
        }
    }

    /**
     * @return the <CODE>attachment</CODE> init parameter
     */
    protected int getAttachmentType() throws MessagingException {
        if(getInitParameter("attachment") == null) {
            return NONE;
        } else {
            return getTypeCode(getInitParameter("attachment"));
        }
    }

    /**
     * @return the <CODE>message</CODE> init parameter or an empty string if missing
     */
    protected String getMessage() throws MessagingException {
        if(getInitParameter("message") == null) {
            return "";
        } else {
            return getInitParameter("message");
        }
    }

    /**
     * @return the <CODE>recipients</CODE> init parameter or <CODE>SpecialAddress.SENDER</CODE>
     * or <CODE>SpecialAddress.RETURN_PATH</CODE> or null if missing
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

        MailAddress specialAddress = getSpecialAddress(addressList,
                                        new String[] {"postmaster", "sender", "returnPath"});
        if (specialAddress != null) {
            newRecipients.add(specialAddress);
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
     * @return the <CODE>to</CODE> init parameter or <CODE>SpecialAddress.SENDER</CODE>
     * or S<CODE>pecialAddress.RETURN_PATH</CODE> or <CODE>SpecialAddress.UNALTERED</CODE> or null if missing
     */
    protected InternetAddress[] getTo() throws MessagingException {
        String addressList = (getInitParameter("to") == null)
                                 ? getInitParameter("recipients")
                                 : getInitParameter("to");
        // if nothing was specified, return null meaning no change
        if (addressList == null) {
            return null;
        }

        MailAddress specialAddress = getSpecialAddress(addressList,
                                        new String[] {"postmaster", "sender", "returnPath", "unaltered"});
        if (specialAddress != null) {
            InternetAddress[] iaarray = new InternetAddress[1];
            iaarray[0] = specialAddress.toInternetAddress();
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
     * @return the <CODE>replyto</CODE> init parameter or null if missing or == "sender"
     */
    protected MailAddress getReplyTo() throws MessagingException {
        String addressString = getInitParameter("replyto");
        if(addressString != null) {
            MailAddress specialAddress = getSpecialAddress(addressString,
                                            new String[] {"postmaster", "sender"});
            if (specialAddress != null) {
                if (specialAddress == SpecialAddress.SENDER) {
                    // means no change
                    return null;
                }
                return specialAddress;
            }

            try {
                return new MailAddress(addressString);
            } catch(Exception e) {
                log("Parse error in getReplyTo: " + addressString);
            }
        }

        return null;
    }

    /**
     * @return the <CODE>returnPath</CODE> init parameter or <CODE>SpecialAddress.NULL</CODE>
     * or <CODE>SpecialAddress.SENDER</CODE> or null if missing
     */
    protected MailAddress getReturnPath() throws MessagingException {
        String addressString = getInitParameter("returnPath");
        if(addressString != null) {
            MailAddress specialAddress = getSpecialAddress(addressString,
                                            new String[] {"postmaster", "sender", "null"});
            if (specialAddress != null) {
                return specialAddress;
            }

            try {
                return new MailAddress(addressString);
            } catch(Exception e) {
                log("Parse error in getReturnPath: " + addressString);
            }
        }

        return null;
    }

    /**
     * @return the <CODE>sender</CODE> init parameter or null if missing or == "sender"
     */
    protected MailAddress getSender() throws MessagingException {
        String addressString = getInitParameter("sender");
        if(addressString != null) {
            MailAddress specialAddress = getSpecialAddress(addressString,
                                            new String[] {"postmaster", "sender"});
            if (specialAddress != null) {
                if (specialAddress == SpecialAddress.SENDER) {
                    // means no change: use FROM header; kept as is for compatibility
                    return null;
                }
                return specialAddress;
            }

            try {
                return new MailAddress(addressString);
            } catch(Exception e) {
                log("Parse error in getSender: " + addressString);
            }
        }

        return null;
    }

    /**
     * @return the <CODE>prefix</CODE> init parameter or an empty string if missing
     */
    protected String getSubjectPrefix() throws MessagingException {
        if(getInitParameter("prefix") == null) {
            return "";
        } else {
            return getInitParameter("prefix");
        }
    }

    /**
     * @return the <CODE>attachError</CODE> init parameter; false if missing
     */
    protected boolean attachError() throws MessagingException {
        if(getInitParameter("attachError") == null) {
            return false;
        } else {
            return new Boolean(getInitParameter("attachError")).booleanValue();
        }
    }

    /**
     * @return the <CODE>isReply</CODE> init parameter; false if missing
     */
    protected boolean isReply() throws MessagingException {
        if(getInitParameter("isReply") == null) {
            return false;
        }
        return new Boolean(getInitParameter("isReply")).booleanValue();
    }

    /* ******************************************************************** */
    /* ******************* End of getX and setX methods ******************* */
    /* ******************************************************************** */

}
