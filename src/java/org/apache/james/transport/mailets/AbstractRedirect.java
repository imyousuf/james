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
 * <P>Abstract mailet providing configurable redirection services.<BR>
 * This mailet can be subclassed to make authoring redirection mailets simple.<BR>
 * By extending it and overriding one or more of these methods new behaviour can
 * be quickly created without the author having to address any other issue than
 * the relevant one:</P>
 * <UL>
 * <LI>attachError() , should error messages be appended to the message</LI>
 * <LI>getAttachmentType(), what should be attached to the message</LI>
 * <LI>getInLineType(), what should be included in the message</LI>
 * <LI>getMessage(), The text of the message itself</LI>
 * <LI>getRecipients(), the recipients the mail is sent to</LI>
 * <LI>getReplyTo(), where replies to this message will be sent</LI>
 * <LI>getReturnPath(), what to set the Return-Path to</LI>
 * <LI>getSender(), who the mail is from</LI>
 * <LI>getSubjectPrefix(), a prefix to be added to the message subject</LI>
 * <LI>getTo(), a list of people to whom the mail is *apparently* sent</LI>
 * <LI>isReply(), should this mailet set the IN_REPLY_TO header to the id of the current message</LI>
 * <LI>getPassThrough(), should this mailet allow the original message to continue processing or GHOST it.</LI>
 * <LI>getFakeDomainCheck(), should this mailet check if the sender domain address is valid.</LI>
 * <LI>isStatic(), should this mailet run the get methods for every mail, or just once.</LI>
 * </UL>
 * <P>For each of the methods above (generically called "getX()" methods in this class
 * and its subclasses), there is an associated "getX(Mail)" method and most times
 * a "setX(Mail, Tx, Mail)" method.<BR>
 * The roles are the following:</P>
 * <UL>
 * <LI>a "getX()" method returns the correspondent "X" value that can be evaluated "statically"
 * once at init time and then stored in a variable and made available for later use by a
 * "getX(Mail)" method;</LI>
 * <LI>a "getX(Mail)" method is the one called to return the correspondent "X" value
 * that can be evaluated "dynamically", tipically based on the currently serviced mail;
 * the default behaviour is to return the value of getX();</LI>
 * <LI>a "setX(Mail, Tx, Mail)" method is called to change the correspondent "X" value
 * of the redirected Mail object, using the value returned by "gexX(Mail)";
 * if such value is null, it does nothing.</LI>
 * </UL>
 * <P>Here follows the typical pattern of those methods:</P>
 * <PRE><CODE>
 *    ...
 *    Tx x;
 *    ...
 *    protected boolean getX(Mail originalMail) throws MessagingException {
 *        boolean x = (isStatic()) ? this.x : getX();
 *        ...
 *        return x;
 *    }
 *    ...
 *    public void init() throws MessagingException {
 *        ...
 *        isStatic = (getInitParameter("static") == null) ? false : new Boolean(getInitParameter("static")).booleanValue();
 *        if(isStatic()) {
 *            ...
 *            X  = getX();
 *            ...
 *        }
 *    ...
 *    public void service(Mail originalMail) throws MessagingException {
 *    ...
 *    setX(newMail, getX(originalMail), originalMail);
 *    ...
 *    }
 *    ...
 * </CODE></PRE>
 * <P>The <I>isStatic</I> variable and method is used to allow for the situations
 * (deprecated since version 2.2, but possibly used by previoulsy written extensions
 * to {@link Redirect}) in which the getX() methods are non static: in this case
 * {@link #isStatic()} must return false.<BR>
 * Finally, a "getX()" method may return a "special address" (see {@link SpecialAddress}),
 * that later will be resolved ("late bound") by a "getX(Mail)" or "setX(Mail, Tx, Mail)":
 * it is a dynamic value that does not require <CODE>isStatic</CODE> to be false.</P>
 *
 * <P>Supports by default the <CODE>passThrough</CODE> init parameter (false if missing).
 * Subclasses can override this behaviour overriding {@link #getPassThrough()}.</P>
 *
 * @version CVS $Revision: 1.1.2.12 $ $Date: 2003/06/25 22:02:31 $
 * @since 2.2.0
 */

public abstract class AbstractRedirect extends GenericMailet {

    /**
     * Controls certain log messages.
     */
    protected boolean isDebug = false;

    /**
     * Holds the value of the <CODE>static</CODE> init parameter.
     */
    protected boolean isStatic = false;

    private static class AddressMarker {
        public static MailAddress SENDER;
        public static MailAddress RETURN_PATH;
        public static MailAddress TO;
        public static MailAddress RECIPIENTS;
        public static MailAddress DELETE;
        public static MailAddress UNALTERED;
        public static MailAddress NULL;

        static {
            try {
                SENDER      = new MailAddress("sender","address.marker");
                RETURN_PATH = new MailAddress("return.path","address.marker");
                TO          = new MailAddress("to","address.marker");
                RECIPIENTS  = new MailAddress("recipients","address.marker");
                DELETE      = new MailAddress("delete","address.marker");
                UNALTERED   = new MailAddress("unaltered","address.marker");
                NULL        = new MailAddress("null","address.marker");

            } catch (Exception _) {}
        }
    }

    /**
     * Class containing "special addresses" constants.
     * Such addresses mean dynamic values that later will be resolved ("late bound")
     * by a "getX(Mail)" or "setX(Mail, Tx, Mail)".
     */
    protected static class SpecialAddress {
        public static final MailAddress SENDER      = AddressMarker.SENDER;
        public static final MailAddress RETURN_PATH = AddressMarker.RETURN_PATH;
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

    private boolean passThrough = false;
    private boolean fakeDomainCheck = true;
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

    /* ******************************************************************** */
    /* ****************** Begin of getX and setX methods ****************** */
    /* ******************************************************************** */

    /**
     * <P>Gets the <CODE>static</CODE> property.</P>
     * <P>Return true to reduce calls to getTo, getSender, getRecipients, getReplyTo, getReturnPath amd getMessage
     * where these values don't change (eg hard coded, or got at startup from the mailet config);
     * return false where any of these methods generate their results dynamically eg in response to the message being processed,
     * or by reference to a repository of users.</P>
     * <P>It is now (from version 2.2) somehow obsolete, as should be always true because the "good practice"
     * is to use "getX()" methods statically, and use instead "getX(Mail)" methods for dynamic situations.
     * A false value is now meaningful only for subclasses of {@link Redirect} older than version 2.2
     * that were relying on this.</P>
     *
     * <P>Is a "getX()" method.</P>
     *
     * @return true, as normally "getX()" methods shouls be static
     */
    protected boolean isStatic() {
        return true;
    }

    /**
     * Gets the <CODE>passThrough</CODE> property.
     * Return true to allow the original message to continue through the processor, false to GHOST it.
     * Is a "getX()" method.
     *
     * @return the <CODE>passThrough</CODE> init parameter, or false if missing
     */
    protected boolean getPassThrough() throws MessagingException {
        if(getInitParameter("passThrough") == null) {
            return false;
        } else {
            return new Boolean(getInitParameter("passThrough")).booleanValue();
        }
    }

    /**
     * Gets the <CODE>passThrough</CODE> property,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getPassThrough()}
     */
    protected boolean getPassThrough(Mail originalMail) throws MessagingException {
        boolean passThrough = (isStatic()) ? this.passThrough : getPassThrough();
        return passThrough;
    }

    /**
     * Gets the <CODE>fakeDomainCheck</CODE> property.
     * Return true to check if the sender domain is valid.
     * Is a "getX()" method.
     *
     * @return the <CODE>fakeDomainCheck</CODE> init parameter, or true if missing
     */
    protected boolean getFakeDomainCheck() throws MessagingException {
        if(getInitParameter("fakeDomainCheck") == null) {
            return true;
        } else {
            return new Boolean(getInitParameter("fakeDomainCheck")).booleanValue();
        }
    }

    /**
     * Gets the <CODE>fakeDomainCheck</CODE> property,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getFakeDomainCheck()}
     */
    protected boolean getFakeDomainCheck(Mail originalMail) throws MessagingException {
        boolean fakeDomainCheck = (isStatic()) ? this.fakeDomainCheck : getFakeDomainCheck();
        return fakeDomainCheck;
    }

    /**
     * Gets the <CODE>inline</CODE> property.
     * May return one of the following values to indicate how to append the original message
     * to build the new message:
     * <ul>
     *    <li><CODE>UNALTERED</CODE> : original message is the new message body</li>
     *    <li><CODE>BODY</CODE> : original message body is appended to the new message</li>
     *    <li><CODE>HEADS</CODE> : original message headers are appended to the new message</li>
     *    <li><CODE>ALL</CODE> : original is appended with all headers</li>
     *    <li><CODE>NONE</CODE> : original is not appended</li>
     * </ul>
     * Is a "getX()" method.
     *
     * @return UNALTERED
     */
    protected int getInLineType() throws MessagingException {
        return UNALTERED;
    }

    /**
     * Gets the <CODE>inline</CODE> property,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getInLineType()}
     */
    protected int getInLineType(Mail originalMail) throws MessagingException {
        int inLineType = (isStatic()) ? this.inLineType : getInLineType();
        return inLineType;
    }

    /** Gets the <CODE>attachment</CODE> property.
     * May return one of the following values to indicate how to attach the original message
     * to the new message:
     * <ul>
     *    <li><CODE>BODY</CODE> : original message body is attached as plain text to the new message</li>
     *    <li><CODE>HEADS</CODE> : original message headers are attached as plain text to the new message</li>
     *    <li><CODE>ALL</CODE> : original is attached as plain text with all headers</li>
     *    <li><CODE>MESSAGE</CODE> : original message is attached as type message/rfc822, a complete mail message.</li>
     *    <li><CODE>NONE</CODE> : original is not attached</li>
     * </ul>
     * Is a "getX()" method.
     *
     * @return NONE
     */
    protected int getAttachmentType() throws MessagingException {
        return NONE;
    }

    /**
     * Gets the <CODE>attachment</CODE> property,
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
     * Gets the <CODE>message</CODE> property.
     * Returns a message to which the original message can be attached/appended
     * to build the new message.
     * Is a "getX()" method.
     *
     * @return ""
     */
    protected String getMessage() throws MessagingException {
        return "";
    }

    /**
     * Gets the <CODE>message</CODE> property,
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
     * Gets the <CODE>recipients</CODE> property.
     * Returns the collection of recipients of the new message,
     * or null if no change is requested.
     * Is a "getX()" method.
     *
     * @return null
     */
    protected Collection getRecipients() throws MessagingException {
        return null;
    }

    /**
     * Gets the <CODE>recipients</CODE> property,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getRecipients()},
     * replacing <CODE>SpecialAddress.SENDER</CODE> if applicable with the original sender
     * and <CODE>SpecialAddress.UNALTERED</CODE> if applicable with null
     */
    protected Collection getRecipients(Mail originalMail) throws MessagingException {
        Collection recipients = (isStatic()) ? this.recipients : getRecipients();
        if (recipients != null && recipients.size() == 1) {
            if (recipients.contains(SpecialAddress.UNALTERED)) {
                recipients = null;
            } else if (recipients.contains(SpecialAddress.SENDER)) {
                recipients = new ArrayList();
                recipients.add(originalMail.getSender());
            } else if (recipients.contains(SpecialAddress.RETURN_PATH)) {
                recipients = new ArrayList();
                MailAddress mailAddress = getExistingReturnPath(originalMail);
                if (mailAddress == SpecialAddress.NULL) {
                    // should never get here
                    throw new MessagingException("NULL return path found getting recipients");
                }
                if (mailAddress == null) {
                    recipients.add(originalMail.getSender());
                } else {
                    recipients.add(mailAddress);
                }
            }
        }
        return recipients;
    }

    /**
     * Sets the recipients of <I>newMail</I> to <I>recipients</I>.
     * If the requested value is null does nothing.
     * Is a "setX(Mail, Tx, Mail)" method.
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
     * Gets the <CODE>to</CODE> property.
     * Returns the "To:" recipients of the new message.
     * or null if no change is requested.
     * Is a "getX()" method.
     *
     * @return null
     */
    protected InternetAddress[] getTo() throws MessagingException {
        return null;
    }

    /**
     * Gets the <CODE>to</CODE> property,
     * built dynamically using the original Mail object.
     * Its outcome will be the the value the <I>TO:</I> header will be set to,
     * that could be different from the real recipient (see {@link #getRecipients}).
     * Is a "getX(Mail)" method.
     * <P>The logic is the following, based on {@link #getTo()}:</P>
     * <UL>
     *      <LI>
     *          If <CODE>getTo()</CODE> returns null it returns null, meaning no change.
     *      </LI>
     *      <LI>
     *          If <CODE>getTo()</CODE> returns <CODE>SpecialAddress.SENDER</CODE>
     *          it returns the <I>sender</I>;
     *          if the <I>sender</I> is null it returns the <I>Return-Path:</I> header;
     *          if the <I>Return-Path:</I> header does not exist or == <CODE>SpecialAddress.NULL</CODE> ("<>") it returns "<>".
     *      </LI>
     *      <LI>
     *          If <CODE>getTo()</CODE> returns <CODE>SpecialAddress.UNALTERED</CODE>
     *          it returns the original <I>TO:</I>.
     *      </LI>
     *      <LI>
     *          If <CODE>getTo()</CODE> returns <CODE>SpecialAddress.RETURN_PATH</CODE>
     *          it returns the contents of the <I>Return-Path:</I> header;
     *          if the <I>Return-Path:</I> header is <CODE>SpecialAddress.NULL</CODE> it throws a <CODE>MessagingException</CODE>;
     *          if the <I>Return-Path:</I> header does not exist it returns the <I>sender</I>;
     *          if the <I>sender</I> is null it returns "<>".
     *      </LI>
     * </UL>
     *
     * @return {@link #getTo()}, replacing <CODE>SpecialAddress.SENDER</CODE>,
     * <CODE>SpecialAddress.SENDER</CODE> and <CODE>SpecialAddress.UNALTERED</CODE> if applicable
     */
    protected InternetAddress[] getTo(Mail originalMail) throws MessagingException {
        InternetAddress[] apparentlyTo = (isStatic()) ? this.apparentlyTo : getTo();
        if (apparentlyTo != null && apparentlyTo.length == 1) {
            if (apparentlyTo[0].equals(SpecialAddress.SENDER.toInternetAddress())) {
                MailAddress mailAddress = originalMail.getSender();
                if (mailAddress == null) {
                    mailAddress = getExistingReturnPath(originalMail);
                    if (mailAddress == SpecialAddress.NULL) {
                        mailAddress = null;
                    }
                }
                if (mailAddress == null) {
                    /* IMPORTANT: setTo() treats null differently from
                     * an zero length array, so do not just use null
                     */
                    // set to <>
                    apparentlyTo = new InternetAddress[0];
                } else {
                    apparentlyTo = new InternetAddress[1];
                    apparentlyTo[0] = mailAddress.toInternetAddress();
                }
            } else if (apparentlyTo[0].equals(SpecialAddress.UNALTERED.toInternetAddress())) {
                apparentlyTo = (InternetAddress[]) originalMail.getMessage().getRecipients(Message.RecipientType.TO);
            } else if (apparentlyTo[0].equals(SpecialAddress.RETURN_PATH.toInternetAddress())) {
                MailAddress mailAddress = getExistingReturnPath(originalMail);
                if (mailAddress == SpecialAddress.NULL) {
                    // should never get here
                    throw new MessagingException("NULL return path found getting recipients");
                }
                if (mailAddress == null) {
                    mailAddress = originalMail.getSender();
                }
                if (mailAddress == null) {
                    /* IMPORTANT: setTo() treats null differently from
                     * an zero length array, so do not just use null
                     */
                    // set to <>
                    apparentlyTo = new InternetAddress[0];
                } else {
                    apparentlyTo = new InternetAddress[1];
                    apparentlyTo[0] = mailAddress.toInternetAddress();
                }
            }
        }
        return apparentlyTo;
    }

    /**
     * Sets the "To:" header of <I>newMail</I> to <I>to</I>.
     * If the requested value is null does nothing.
     * Is a "setX(Mail, Tx, Mail)" method.
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
     * Gets the <CODE>replyto</CODE> property.
     * Returns the Reply-To address of the new message,
     * or null if no change is requested.
     * Is a "getX()" method.
     *
     * @return null
     */
    protected MailAddress getReplyTo() throws MessagingException {
        return null;
    }

    /**
     * Gets the <CODE>replyTo</CODE> property,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getReplyTo()}
     * replacing <CODE>SpecialAddress.UNALTERED</CODE> if applicable with null
     */
    protected MailAddress getReplyTo(Mail originalMail) throws MessagingException {
        MailAddress replyTo = (isStatic()) ? this.replyTo : getReplyTo();
        if (replyTo != null) {
            if (replyTo == SpecialAddress.UNALTERED) {
                replyTo = null;
            }
        }
        return replyTo;
    }

    /**
     * <P>Sets the "Reply-To:" header of <I>newMail</I> to <I>replyTo</I>.</P>
     * <P>If the requested value is <CODE>SpecialAddress.SENDER</CODE> will use the original "From:" header;
     * if this header is empty will use the original "Sender:" header;
     * if this header is empty will use the original sender.
     * If the requested value is null does nothing.</P>
     * Is a "setX(Mail, Tx, Mail)" method.
     */
    protected void setReplyTo(Mail newMail, MailAddress replyTo, Mail originalMail) throws MessagingException {
        if(replyTo != null) {
            if (replyTo == SpecialAddress.SENDER) {
                replyTo = getSafeApparentSender(originalMail);

                // if still null give up.
                if (replyTo == null) {
                    return;
                }
            }
            
            // do the job
            InternetAddress[] iart = new InternetAddress[1];
            iart[0] = replyTo.toInternetAddress();
            newMail.getMessage().setReplyTo(iart);
            
            if (isDebug) {
                log("replyTo set to: " + replyTo);
            }
        }
    }

    /**
     * Gets the <CODE>returnPath</CODE> property.
     * Returns the Return-Path of the new message,
     * or null if no change is requested.
     * Is a "getX()" method.
     *
     * @return null
     */
    protected MailAddress getReturnPath() throws MessagingException {
        return null;
    }

    /**
     * Gets the <CODE>returnPath</CODE> property,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getReturnPath()},
     * replacing <CODE>SpecialAddress.SENDER</CODE> if applicable with the original sender,
     * replacing <CODE>SpecialAddress.UNALTERED</CODE> if applicable with null,
     * but not replacing <CODE>SpecialAddress.NULL</CODE>
     */
    protected MailAddress getReturnPath(Mail originalMail) throws MessagingException {
        MailAddress returnPath = (isStatic()) ? this.returnPath : getReturnPath();
        if (returnPath != null) {
            if (returnPath == SpecialAddress.UNALTERED) {
                returnPath = null;
            }
            else if (returnPath == SpecialAddress.SENDER) {
                returnPath = originalMail.getSender();
            }
        }
        return returnPath;
    }

    /**
     * Sets the "Return-Path:" header of <I>newMail</I> to <I>returnPath</I>.
     * If the requested value is <CODE>SpecialAddress.NULL</CODE> sets it to "<>".
     * If the requested value is null does nothing.
     * Is a "setX(Mail, Tx, Mail)" method.
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
     * Gets the <CODE>sender</CODE> property.
     * Returns the new sender as a MailAddress,
     * or null if no change is requested.
     * Is a "getX()" method.
     *
     * @return null
     */
    protected MailAddress getSender() throws MessagingException {
        return null;
    }

    /**
     * Gets the <CODE>sender</CODE> property,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getSender()}
     * replacing <CODE>SpecialAddress.UNALTERED</CODE> if applicable with null
     */
    protected MailAddress getSender(Mail originalMail) throws MessagingException {
        MailAddress sender = (isStatic()) ? this.sender : getSender();
        if (sender != null) {
            if (sender == SpecialAddress.UNALTERED) {
                sender = null;
            }
        }
        return sender;
    }

    /**
     * <P>Sets the sender and the "From:" header of <I>newMail</I> to <I>sender</I>.</P>
     * <P>If the requested value is <CODE>SpecialAddress.SENDER</CODE> will use the original "From:" header;
     * if this header is empty will use the original "Sender:" header;
     * if this header is empty will use the original sender.
     * If the requested value is null does nothing.</P>
     * Is a "setX(Mail, Tx, Mail)" method.
     */
    protected void setSender(Mail newMail, MailAddress sender, Mail originalMail) throws MessagingException {
        if (sender != null) {
            if (sender == SpecialAddress.SENDER) {
                MailAddress newSender = getSafeApparentSender(originalMail);
                String newFromHeader = getSafeFromHeader(originalMail);
                
                if (!newFromHeader.trim().equals("")) {
                    newMail.getMessage().setHeader(RFC2822Headers.FROM, newFromHeader);
                }
                
                if (newSender != null) {
                    ((MailImpl) newMail).setSender(newSender);
                }
                
                // The new code should be compatible with and extend the previous code:
//                MailAddress originalSender = new MailAddress(((InternetAddress) originalMail.getMessage().getFrom()[0]).getAddress());
//                newMail.getMessage().setHeader(RFC2822Headers.FROM, originalMail.getMessage().getHeader(RFC2822Headers.FROM, ","));
//                ((MailImpl) newMail).setSender(originalSender);
            } else {
                newMail.getMessage().setFrom(sender.toInternetAddress());
                ((MailImpl) newMail).setSender(sender);
            }
            
            if (isDebug) {
                log("sender set to: " + sender);
            }
        }
    }
    
    /**
     * <P>Gets a safe "apparent" sender using the original "From:" header if possible.</P>
     * <P>Will use the original "From:" header;
     * if this header is empty will use the original "Sender:" header;
     * if this header is empty will use the original sender.</P>
     */
    private MailAddress getSafeApparentSender(Mail originalMail) throws MessagingException {
        MailAddress sender = null;
        InternetAddress from = (InternetAddress) originalMail.getMessage().getFrom()[0];
        
        if (from == null) {
            // perhaps this is redundant, but just in case ...
            from = originalMail.getSender().toInternetAddress();
        }
        if (from != null) {
            sender = new MailAddress(from.getAddress());
        }
        
        return sender;
    }

    /**
     * <P>Gets a safe "From:" header using the original if possible.</P>
     * <P>Will use the original "From:" header;
     * if this header is empty will use the original "Sender:" header;
     * if this header is empty will use the original sender.</P>
     */
    private String getSafeFromHeader(Mail originalMail) throws MessagingException {
        MailAddress sender = getSafeApparentSender(originalMail);
        String fromHeader = originalMail.getMessage().getHeader(RFC2822Headers.FROM, ",");
        
        if (fromHeader.trim().equals("") && sender != null) {
            fromHeader = sender.toInternetAddress().toString();
        }

        return fromHeader;
    }

    /**
     * Gets the <CODE>prefix</CODE> property.
     * Returns a prefix for the new message subject.
     * Is a "getX()" method.
     *
     * @return ""
     */
    protected String getSubjectPrefix() throws MessagingException {
        return "";
    }

    /**
     * Gets the <CODE>subjectPrefix</CODE> property,
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
     * Gets the <CODE>attachError</CODE> property.
     * Returns a boolean indicating whether to append a description of any error to the main body part
     * of the new message, if getInlineType does not return "UNALTERED".
     * Is a "getX()" method.
     *
     * @return false
     */
    protected boolean attachError() throws MessagingException {
        return false;
    }

    /**
     * Gets the <CODE>attachError</CODE> property,
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
     * Gets the <CODE>isReply</CODE> property.
     * Returns a boolean indicating whether the new message must be considered
     * a reply to the original message, setting the IN_REPLY_TO header of the new
     * message to the id of the original message.
     * Is a "getX()" method.
     *
     * @return false
     */
    protected boolean isReply() throws MessagingException {
        return false;
    }

    /**
     * Gets the <CODE>isReply</CODE> property,
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

    /* ******************************************************************** */
    /* ******************* End of getX and setX methods ******************* */
    /* ******************************************************************** */

    /**
     * Mailet initialization routine.
     * Will setup static values for each "x" initialization parameter in config.xml,
     * using getX(), if {@link #isStatic()} returns true.
     */
    public void init() throws MessagingException {
        if (isDebug) {
            log("Redirect init");
        }
        isDebug = (getInitParameter("debug") == null) ? false : new Boolean(getInitParameter("debug")).booleanValue();

        isStatic = (getInitParameter("static") == null) ? false : new Boolean(getInitParameter("static")).booleanValue();

        if(isStatic()) {
            passThrough     = getPassThrough();
            fakeDomainCheck = getFakeDomainCheck();
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
                            .append("static")
                            .append(", passThrough=").append(passThrough)
                            .append(", fakeDomainCheck=").append(fakeDomainCheck)
                            .append(", sender=").append(sender)
                            .append(", replyTo=").append(replyTo)
                            .append(", returnPath=").append(returnPath)
                            .append(", message=").append(messageText)
                            .append(", recipients=").append(arrayToString(recipients == null ? null : recipients.toArray()))
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
        Mail newMail = ((MailImpl) originalMail).duplicate(newName((MailImpl) originalMail));
        // We don't need to use the original Remote Address and Host,
        // and doing so would likely cause a loop with spam detecting
        // matchers.
        try {
            ((MailImpl) newMail).setRemoteAddr(java.net.InetAddress.getLocalHost().getHostAddress());
            ((MailImpl) newMail).setRemoteHost(java.net.InetAddress.getLocalHost().getHostName());
        } catch (java.net.UnknownHostException _) {
            ((MailImpl) newMail).setRemoteAddr("127.0.0.1");
            ((MailImpl) newMail).setRemoteHost("localhost");
        }

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
                log("Alter message");
            }
            newMail.setMessage(new MimeMessage(Session.getDefaultInstance(System.getProperties(),
                                                               null)));

            // handle the new message if altered
            buildAlteredMessage(newMail, originalMail);

            setTo(newMail, getTo(originalMail), originalMail);

        } else {
            // if we need the original, create a copy of this message to redirect
            if (getPassThrough(originalMail)) {
                newMail.setMessage(new MimeMessage(originalMail.getMessage()) {
                    protected void updateHeaders() throws MessagingException {
                        if (getMessageID() == null) super.updateHeaders();
                        else {
                            modified = false;
                        }
                    }
                });
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

        if (senderDomainIsValid(newMail)) {
            //Send it off...
            getMailetContext().sendMail(newMail);
        } else {
            StringBuffer logBuffer = new StringBuffer(256)
                                    .append(getMailetName())
                                    .append(" mailet cannot forward ")
                                    .append(((MailImpl) originalMail).getName())
                                    .append(". Invalid sender domain for ")
                                    .append(newMail.getSender())
                                    .append(". Consider using the Redirect mailet ")
                                    .append("using a different sender.");
            log(logBuffer.toString());
        }

        if(!getPassThrough(originalMail)) {
            originalMail.setState(Mail.GHOST);
        }
    }

    private static final java.util.Random random = new java.util.Random();  // Used to generate new mail names

    /**
     * Create a unique new primary key name.
     *
     * @param mail the mail to use as the basis for the new mail name
     * @return a new name
     */
    private String newName(MailImpl mail) throws MessagingException {
        String oldName = mail.getName();
        
        // Checking if the original mail name is too long, perhaps because of a
        // loop caused by a configuration error.
        // it could cause a "null pointer exception" in AvalonMailRepository much
        // harder to understand.
        if (oldName.length() > 76) {
            int count = 0;
            int index = 0;
            while ((index = oldName.indexOf('!', index)) >= 0) {
                count++;
            }
            // It looks like a configuration loop. It's better to stop.
            if (count > 7) {
                throw new MessagingException("Unable to create a new message name: too long."
                                             + " Possible loop in config.xml.");
            }
            else {
                oldName = oldName.substring(0, 76);
            }
        }
        
        StringBuffer nameBuffer =
                                 new StringBuffer(64)
                                 .append(oldName)
                                 .append("-!")
                                 .append(random.nextInt(1048576));
        return nameBuffer.toString();
    }

    /**
     * A private method to convert types from string to int.
     *
     * @param param the string type
     * @return the corresponding int enumeration
     */
    protected int getTypeCode(String param) {
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
     * Gets the MailAddress corresponding to the existing "Return-Path" header of
     * <I>mail</I>.
     * If empty returns <CODE>SpecialAddress.NULL</CODE>,
     * if missing return <CODE>SpecialAddress.SENDER</CODE>.
     */
    protected MailAddress getExistingReturnPath(Mail mail) throws MessagingException {
        MailAddress mailAddress = null;
        String[] returnPathHeaders = mail.getMessage().getHeader(RFC2822Headers.RETURN_PATH);
        String returnPathHeader = null;
        if (returnPathHeaders != null) {
            returnPathHeader = returnPathHeaders[0];
            if (returnPathHeader != null) {
                returnPathHeader = returnPathHeader.trim();
                if (returnPathHeader.equals("<>")) {
                    mailAddress = SpecialAddress.NULL;
                } else {
                    mailAddress = new MailAddress(new InternetAddress(returnPathHeader));
                }
            }
        }
        return mailAddress;
    }

    /**
     * Utility method for obtaining a string representation of an array of Objects.
     */
    private String arrayToString(Object[] array) {
        if (array == null) {
            return "null";
        }
        StringBuffer sb = new StringBuffer(1024);
        sb.append("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(array[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Utility method for obtaining a string representation of a
     * Message's headers
     */
    private String getMessageHeaders(MimeMessage message) throws MessagingException {
        Enumeration heads = message.getAllHeaderLines();
        StringBuffer headBuffer = new StringBuffer(1024);
        while(heads.hasMoreElements()) {
            headBuffer.append(heads.nextElement().toString()).append("\r\n");
        }
        return headBuffer.toString();
    }

    /**
     * Utility method for obtaining a string representation of a
     * Message's body
     */
    private String getMessageBody(MimeMessage message) throws Exception {
        java.io.InputStream bis = null;
        java.io.OutputStream bos = null;
        java.io.ByteArrayOutputStream bodyOs = new java.io.ByteArrayOutputStream();

        try {
            // Get the message as a stream.  This will encode
            // objects as necessary, and we have some overhead from
            // decoding and re-encoding the stream.  I'd prefer the
            // raw stream, but see the WARNING below.
            bos = javax.mail.internet.MimeUtility.encode(bodyOs, message.getEncoding());
            bis = message.getInputStream();
        } catch(javax.activation.UnsupportedDataTypeException udte) {
            /* If we get an UnsupportedDataTypeException try using
             * the raw input stream as a "best attempt" at rendering
             * a message.
             *
             * WARNING: JavaMail v1.3 getRawInputStream() returns
             * INVALID (unchanged) content for a changed message.
             * getInputStream() works properly, but in this case
             * has failed due to a missing DataHandler.
             *
             * MimeMessage.getRawInputStream() may throw a "no
             * content" MessagingException.  In JavaMail v1.3, when
             * you initially create a message using MimeMessage
             * APIs, there is no raw content available.
             * getInputStream() works, but getRawInputStream()
             * throws an exception.  If we catch that exception,
             * throw the UDTE.  It should mean that someone has
             * locally constructed a message part for which JavaMail
             * doesn't have a DataHandler.
             */

            try {
                bis = message.getRawInputStream();
                bos = bodyOs;
            } catch(javax.mail.MessagingException _) {
                throw udte;
            }
        }
        catch(javax.mail.MessagingException me) {
            /* This could be another kind of MessagingException
             * thrown by MimeMessage.getInputStream(), such as a
             * javax.mail.internet.ParseException.
             *
             * The ParseException is precisely one of the reasons
             * why the getRawInputStream() method exists, so that we
             * can continue to stream the content, even if we cannot
             * handle it.  Again, if we get an exception, we throw
             * the one that caused us to call getRawInputStream().
             */
            try {
                bis = message.getRawInputStream();
                bos = bodyOs;
            } catch(javax.mail.MessagingException _) {
                throw me;
            }
        }

        try {
            byte[] block = new byte[1024];
            int read = 0;
            while ((read = bis.read(block)) > -1) {
                bos.write(block, 0, read);
            }
            bos.flush();
            return bodyOs.toString();
        }
        finally {
            bis.close();
        }
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
        String head = getMessageHeaders(message);
        boolean all = false;

        String messageText = getMessage(originalMail);
        if(messageText != null) {
            out.println(messageText);
        }

        if (isDebug) {
            log("inline:" + getInLineType(originalMail));
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
                    out.println(getMessageBody(message));
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
            if (isDebug) {
                log("attachmentType:" + getAttachmentType(originalMail));
            }
            if(getAttachmentType(originalMail) != NONE) {
                part = new MimeBodyPart();
                switch(getAttachmentType(originalMail)) {
                    case HEADS: //HEADS:
                        part.setText(head);
                        break;
                    case BODY: //BODY:
                        try {
                            part.setText(getMessageBody(message));
                        } catch(Exception e) {
                            part.setText("body unavailable");
                        }
                        break;
                    case ALL: //ALL:
                        StringBuffer textBuffer =
                            new StringBuffer(1024)
                                .append(head)
                                .append("\r\nMessage:\r\n")
                                .append(getMessageBody(message));
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
            //if set, attach the original mail's error message
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

    /**
     * Returns the {@link SpecialAddress} that corresponds to an init parameter value.
     * The init parameter value is checked against a String[] of allowed values.
     * The checks are case insensitive.
     *
     * @param addressString the string to check if is a special address
     * @param allowedSpecials a String[] with the allowed special addresses
     * @return a SpecialAddress if found, null if not found or addressString is null
     * @throws MessagingException if is a special address not in the allowedSpecials array
     */
    protected final MailAddress getSpecialAddress(String addressString, String[] allowedSpecials) throws MessagingException {
        if (addressString == null) {
            return null;
        }

        addressString = addressString.toLowerCase(Locale.US);
        addressString = addressString.trim();

        MailAddress specialAddress = null;

        if(addressString.compareTo("postmaster") == 0) {
            specialAddress = getMailetContext().getPostmaster();
        }
        if(addressString.compareTo("sender") == 0) {
            specialAddress = SpecialAddress.SENDER;
        }
        if(addressString.compareTo("returnpath") == 0) {
            specialAddress = SpecialAddress.RETURN_PATH;
        }
        if(addressString.compareTo("to") == 0) {
            specialAddress = SpecialAddress.TO;
        }
        if(addressString.compareTo("recipients") == 0) {
            specialAddress = SpecialAddress.RECIPIENTS;
        }
        if(addressString.compareTo("delete") == 0) {
            specialAddress = SpecialAddress.DELETE;
        }
        if(addressString.compareTo("unaltered") == 0) {
            specialAddress = SpecialAddress.UNALTERED;
        }
        if(addressString.compareTo("null") == 0) {
            specialAddress = SpecialAddress.NULL;
        }

        // if is a special address, must be in the allowedSpecials array
        if (specialAddress != null) {
            // check if is an allowed special
            boolean allowed = false;
            for (int i = 0; i < allowedSpecials.length; i++) {
                String allowedSpecial = allowedSpecials[i];
                allowedSpecial = allowedSpecial.toLowerCase(Locale.US);
                allowedSpecial = allowedSpecial.trim();
                if(addressString.compareTo(allowedSpecial) == 0) {
                    allowed = true;
                    break;
                }
            }
            if (!allowed) {
                throw new MessagingException("Special (\"magic\") address found not allowed: " + addressString +
                                             ", allowed values are \"" + arrayToString(allowedSpecials) + "\"");
            }
        }

        return specialAddress;
    }

    /**
     * <P>Checks if a sender domain of <I>mail</I> is valid.
     * It is valid if the sender is null or
     * {@link org.apache.mailet.MailetContext#getMailServers} returns true for
     * the sender host part.</P>
     * <P>If we do not do this check, and someone uses a redirection mailet in a
     * processor initiated by SenderInFakeDomain, then a fake
     * sender domain will cause an infinite loop (the forwarded
     * e-mail still appears to come from a fake domain).<BR>
     * Although this can be viewed as a configuration error, the
     * consequences of such a mis-configuration are severe enough
     * to warrant protecting against the infinite loop.</P>
     * <P>This check can be skipped if {@link #getFakeDomainCheck(Mail)} returns true.</P> 
     */
    protected final boolean senderDomainIsValid(Mail mail) throws MessagingException {
        if (getFakeDomainCheck(mail)) {
            return mail.getSender() == null || getMailetContext().getMailServers(mail.getSender().getHost()).size() != 0;
        } else return true;
    }
}
