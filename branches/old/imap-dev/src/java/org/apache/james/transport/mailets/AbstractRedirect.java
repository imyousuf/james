/***********************************************************************
 * Copyright (c) 2000-2005 The Apache Software Foundation.             *
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

import java.io.PrintWriter;
import java.io.StringWriter;

import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.ArrayList;


import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.ParseException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.internet.AddressException;

import org.apache.mailet.RFC2822Headers;
import org.apache.mailet.dates.RFC822DateFormat;
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
 * <LI>getReversePath(), what to set the reverse-path to</LI>
 * <LI>getSender(), who the mail is from</LI>
 * <LI>getSubject(), a string to replace the message subject</LI>
 * <LI>getSubjectPrefix(), a prefix to be added to the message subject, possibly already replaced by a new subject</LI>
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
 * @version CVS $Revision$ $Date$
 * @since 2.2.0
 */

public abstract class AbstractRedirect extends GenericMailet {
    
    /**
     * Gets the expected init parameters.
     *
     * @return null meaning no check
     */
    protected  String[] getAllowedInitParameters() {
        return null;
    }
    
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
        public static MailAddress REVERSE_PATH;
        public static MailAddress FROM;
        public static MailAddress REPLY_TO;
        public static MailAddress TO;
        public static MailAddress RECIPIENTS;
        public static MailAddress DELETE;
        public static MailAddress UNALTERED;
        public static MailAddress NULL;

        static {
            try {
                SENDER          = new MailAddress("sender","address.marker");
                REVERSE_PATH    = new MailAddress("reverse.path","address.marker");
                FROM            = new MailAddress("from","address.marker");
                REPLY_TO        = new MailAddress("reply.to","address.marker");
                TO              = new MailAddress("to","address.marker");
                RECIPIENTS      = new MailAddress("recipients","address.marker");
                DELETE          = new MailAddress("delete","address.marker");
                UNALTERED       = new MailAddress("unaltered","address.marker");
                NULL            = new MailAddress("null","address.marker");

            } catch (Exception _) {}
        }
    }

    /**
     * Class containing "special addresses" constants.
     * Such addresses mean dynamic values that later will be resolved ("late bound")
     * by a "getX(Mail)" or "setX(Mail, Tx, Mail)".
     */
    protected static class SpecialAddress {
        public static final MailAddress SENDER          = AddressMarker.SENDER;
        public static final MailAddress REVERSE_PATH    = AddressMarker.REVERSE_PATH;
        public static final MailAddress FROM            = AddressMarker.FROM;
        public static final MailAddress REPLY_TO        = AddressMarker.REPLY_TO;
        public static final MailAddress TO              = AddressMarker.TO;
        public static final MailAddress RECIPIENTS      = AddressMarker.RECIPIENTS;
        public static final MailAddress DELETE          = AddressMarker.DELETE;
        public static final MailAddress UNALTERED       = AddressMarker.UNALTERED;
        public static final MailAddress NULL            = AddressMarker.NULL;
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
    private MailAddress reversePath;
    private MailAddress sender;
    private String subject;
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
     * <P>Return true to reduce calls to getTo, getSender, getRecipients, getReplyTo, getReversePath amd getMessage
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
        return new Boolean(getInitParameter("passThrough")).booleanValue();
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
        return new Boolean(getInitParameter("fakeDomainCheck")).booleanValue();
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
     * @return the <CODE>inline</CODE> init parameter, or <CODE>UNALTERED</CODE> if missing
     */
    protected int getInLineType() throws MessagingException {
        if(getInitParameter("inline") == null) {
            return UNALTERED;
        } else {
            return getTypeCode(getInitParameter("inline"));
        }
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
     * @return the <CODE>attachment</CODE> init parameter, or <CODE>NONE</CODE> if missing
     */
    protected int getAttachmentType() throws MessagingException {
        if(getInitParameter("attachment") == null) {
            return NONE;
        } else {
            return getTypeCode(getInitParameter("attachment"));
        }
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
     * @return the <CODE>recipients</CODE> init parameter
     * or the postmaster address
     * or <CODE>SpecialAddress.SENDER</CODE>
     * or <CODE>SpecialAddress.FROM</CODE>
     * or <CODE>SpecialAddress.REPLY_TO</CODE>
     * or <CODE>SpecialAddress.REVERSE_PATH</CODE>
     * or <CODE>SpecialAddress.UNALTERED</CODE>
     * or <CODE>SpecialAddress.RECIPIENTS</CODE>
     * or <CODE>null</CODE> if missing
     */
    protected Collection getRecipients() throws MessagingException {
        Collection newRecipients = new HashSet();
        String addressList = getInitParameter("recipients");
        
        // if nothing was specified, return <CODE>null</CODE> meaning no change
        if (addressList == null) {
            return null;
        }

        try {
            InternetAddress[] iaarray = InternetAddress.parse(addressList, false);
            for (int i = 0; i < iaarray.length; i++) {
                String addressString = iaarray[i].getAddress();
                MailAddress specialAddress = getSpecialAddress(addressString,
                new String[] {"postmaster", "sender", "from", "replyTo", "reversePath", "unaltered", "recipients", "to", "null"});
                if (specialAddress != null) {
                    newRecipients.add(specialAddress);
                } else {
                    newRecipients.add(new MailAddress(iaarray[i]));
                }
            }
        } catch (Exception e) {
            throw new MessagingException("Exception thrown in getRecipients() parsing: " + addressList, e);
        }
        if (newRecipients.size() == 0) {
            throw new MessagingException("Failed to initialize \"recipients\" list; empty <recipients> init parameter found.");
        }

        return newRecipients;
    }

    /**
     * Gets the <CODE>recipients</CODE> property,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #replaceMailAddresses} on {@link #getRecipients()},
     */
    protected Collection getRecipients(Mail originalMail) throws MessagingException {
        Collection recipients = (isStatic()) ? this.recipients : getRecipients();
        if (recipients != null) {
            if (recipients.size() == 1 && (recipients.contains(SpecialAddress.UNALTERED) || recipients.contains(SpecialAddress.RECIPIENTS))) {
                recipients = null;
            } else {
                recipients = replaceMailAddresses(originalMail, recipients);
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
     * @return the <CODE>to</CODE> init parameter
     * or the postmaster address
     * or <CODE>SpecialAddress.SENDER</CODE>
     * or <CODE>SpecialAddress.REVERSE_PATH</CODE>
     * or <CODE>SpecialAddress.FROM</CODE>
     * or <CODE>SpecialAddress.REPLY_TO</CODE>
     * or <CODE>SpecialAddress.UNALTERED</CODE>
     * or <CODE>SpecialAddress.TO</CODE>
     * or <CODE>null</CODE> if missing
     */
    protected InternetAddress[] getTo() throws MessagingException {
        InternetAddress[] iaarray = null;
        String addressList = getInitParameter("to");
        
        // if nothing was specified, return null meaning no change
        if (addressList == null) {
            return null;
        }

        try {
            iaarray = InternetAddress.parse(addressList, false);
            for(int i = 0; i < iaarray.length; ++i) {
                String addressString = iaarray[i].getAddress();
                MailAddress specialAddress = getSpecialAddress(addressString,
                                                new String[] {"postmaster", "sender", "from", "replyTo", "reversePath", "unaltered", "recipients", "to", "null"});
                if (specialAddress != null) {
                    iaarray[i] = specialAddress.toInternetAddress();
                }
            }
        } catch (Exception e) {
            throw new MessagingException("Exception thrown in getTo() parsing: " + addressList, e);
        }
        if (iaarray.length == 0) {
            throw new MessagingException("Failed to initialize \"to\" list; empty <to> init parameter found.");
        }

        return iaarray;
    }

    /**
     * Gets the <CODE>to</CODE> property,
     * built dynamically using the original Mail object.
     * Its outcome will be the the value the <I>TO:</I> header will be set to,
     * that could be different from the real recipient (see {@link #getRecipients}).
     * Is a "getX(Mail)" method.
     *
     * @return {@link #replaceInternetAddresses} on {@link #getRecipients()},
     */
    protected InternetAddress[] getTo(Mail originalMail) throws MessagingException {
        InternetAddress[] apparentlyTo = (isStatic()) ? this.apparentlyTo : getTo();
        if (apparentlyTo != null) {
            if (   apparentlyTo.length == 1
                && (   apparentlyTo[0].equals(SpecialAddress.UNALTERED.toInternetAddress())
                    || apparentlyTo[0].equals(SpecialAddress.TO.toInternetAddress())
                    )) {
                apparentlyTo = null;
            } else {
                Collection toList = new ArrayList(apparentlyTo.length);
                for (int i = 0; i < apparentlyTo.length; i++) {
                    toList.add(apparentlyTo[i]);
                }
                /* IMPORTANT: setTo() treats null differently from a zero length array,
                  so it's ok to get a zero length array from replaceSpecialAddresses
                 */
                apparentlyTo = (InternetAddress[]) replaceInternetAddresses(originalMail, toList).toArray(new InternetAddress[0]);
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
     * @return the <CODE>replyto</CODE> init parameter
     * or the postmaster address
     * or <CODE>SpecialAddress.SENDER</CODE>
     * or <CODE>SpecialAddress.UNALTERED</CODE>
     * or <CODE>SpecialAddress.NULL</CODE>
     * or <CODE>null</CODE> if missing
     */
    protected MailAddress getReplyTo() throws MessagingException {
        String addressString = getInitParameter("replyTo");
        if (addressString == null) {
            addressString = getInitParameter("replyto");
        }
        if(addressString != null) {
            MailAddress specialAddress = getSpecialAddress(addressString,
                                            new String[] {"postmaster", "sender", "null", "unaltered"});
            if (specialAddress != null) {
                return specialAddress;
            }

            try {
                return new MailAddress(addressString);
            } catch(Exception e) {
                throw new MessagingException("Exception thrown in getReplyTo() parsing: " + addressString, e);
            }
        }

        return null;
    }

    /**
     * Gets the <CODE>replyTo</CODE> property,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getReplyTo()}
     * replacing <CODE>SpecialAddress.UNALTERED</CODE> if applicable with null
     * and <CODE>SpecialAddress.SENDER</CODE> with the original mail sender
     */
    protected MailAddress getReplyTo(Mail originalMail) throws MessagingException {
        MailAddress replyTo = (isStatic()) ? this.replyTo : getReplyTo();
        if (replyTo != null) {
            if (replyTo == SpecialAddress.UNALTERED) {
                replyTo = null;
            } else if (replyTo == SpecialAddress.SENDER) {
                replyTo = originalMail.getSender();
            }
        }
        return replyTo;
    }

    /**
     * <P>Sets the "Reply-To:" header of <I>newMail</I> to <I>replyTo</I>.</P>
     * If the requested value is <CODE>SpecialAddress.NULL</CODE> will remove the "Reply-To:" header.
     * If the requested value is null does nothing.</P>
     * Is a "setX(Mail, Tx, Mail)" method.
     */
    protected void setReplyTo(Mail newMail, MailAddress replyTo, Mail originalMail) throws MessagingException {
        if(replyTo != null) {
            InternetAddress[] iart = null;
            if (replyTo != SpecialAddress.NULL) {
                iart = new InternetAddress[1];
                iart[0] = replyTo.toInternetAddress();
            }
            
            // Note: if iart is null will remove the header
            newMail.getMessage().setReplyTo(iart);
            
            if (isDebug) {
                log("replyTo set to: " + replyTo);
            }
        }
    }

    /**
     * Gets the <CODE>reversePath</CODE> property.
     * Returns the reverse-path of the new message,
     * or null if no change is requested.
     * Is a "getX()" method.
     *
     * @return the <CODE>reversePath</CODE> init parameter 
     * or the postmaster address
     * or <CODE>SpecialAddress.SENDER</CODE>
     * or <CODE>SpecialAddress.NULL</CODE>
     * or <CODE>SpecialAddress.UNALTERED</CODE>
     * or <CODE>null</CODE> if missing
     */
    protected MailAddress getReversePath() throws MessagingException {
        String addressString = getInitParameter("reversePath");
        if(addressString != null) {
            MailAddress specialAddress = getSpecialAddress(addressString,
                                            new String[] {"postmaster", "sender", "null", "unaltered"});
            if (specialAddress != null) {
                return specialAddress;
            }

            try {
                return new MailAddress(addressString);
            } catch(Exception e) {
                throw new MessagingException("Exception thrown in getReversePath() parsing: " + addressString, e);
            }
        }

        return null;
    }

    /**
     * Gets the <CODE>reversePath</CODE> property,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getReversePath()},
     * replacing <CODE>SpecialAddress.SENDER</CODE> if applicable with null,
     * replacing <CODE>SpecialAddress.UNALTERED</CODE>
     * and <CODE>SpecialAddress.REVERSE_PATH</CODE> if applicable with null,
     * but not replacing <CODE>SpecialAddress.NULL</CODE>
     * that will be handled by {@link #setReversePath}
     */
    protected MailAddress getReversePath(Mail originalMail) throws MessagingException {
        MailAddress reversePath = (isStatic()) ? this.reversePath : getReversePath();
        if (reversePath != null) {
            if (reversePath == SpecialAddress.UNALTERED || reversePath == SpecialAddress.REVERSE_PATH) {
                reversePath = null;
            }
            else if (reversePath == SpecialAddress.SENDER) {
                reversePath = null;
            }
        }
        return reversePath;
    }

    /**
     * Sets the "reverse-path" of <I>newMail</I> to <I>reversePath</I>.
     * If the requested value is <CODE>SpecialAddress.NULL</CODE> sets it to "<>".
     * If the requested value is null does nothing.
     * Is a "setX(Mail, Tx, Mail)" method.
     */
    protected void setReversePath(Mail newMail, MailAddress reversePath, Mail originalMail) throws MessagingException {
        if(reversePath != null) {
            if (reversePath == SpecialAddress.NULL) {
                reversePath = null;
            }
            ((MailImpl) newMail).setSender(reversePath);
            if (isDebug) {
                log("reversePath set to: " + reversePath);
            }
        }
    }

    /**
     * Gets the <CODE>sender</CODE> property.
     * Returns the new sender as a MailAddress,
     * or null if no change is requested.
     * Is a "getX()" method.
     *
     * @return the <CODE>sender</CODE> init parameter
     * or the postmaster address
     * or <CODE>SpecialAddress.SENDER</CODE>
     * or <CODE>SpecialAddress.UNALTERED</CODE>
     * or <CODE>null</CODE> if missing
     */
    protected MailAddress getSender() throws MessagingException {
        String addressString = getInitParameter("sender");
        if(addressString != null) {
            MailAddress specialAddress = getSpecialAddress(addressString,
                                            new String[] {"postmaster", "sender", "unaltered"});
            if (specialAddress != null) {
                return specialAddress;
            }

            try {
                return new MailAddress(addressString);
            } catch(Exception e) {
                throw new MessagingException("Exception thrown in getSender() parsing: " + addressString, e);
            }
        }

        return null;
    }

    /**
     * Gets the <CODE>sender</CODE> property,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getSender()}
     * replacing <CODE>SpecialAddress.UNALTERED</CODE>
     * and <CODE>SpecialAddress.SENDER</CODE> if applicable with null
     */
    protected MailAddress getSender(Mail originalMail) throws MessagingException {
        MailAddress sender = (isStatic()) ? this.sender : getSender();
        if (sender != null) {
            if (sender == SpecialAddress.UNALTERED || sender == SpecialAddress.SENDER) {
                sender = null;
            }
        }
        return sender;
    }

    /**
     * Sets the "From:" header of <I>newMail</I> to <I>sender</I>.
     * If the requested value is null does nothing.
     * Is a "setX(Mail, Tx, Mail)" method.
     */
    protected void setSender(Mail newMail, MailAddress sender, Mail originalMail) throws MessagingException {
        if (sender != null) {
            newMail.getMessage().setFrom(sender.toInternetAddress());
            
            if (isDebug) {
                log("sender set to: " + sender);
            }
        }
    }
    
    /**
     * Gets the <CODE>subject</CODE> property.
     * Returns a string for the new message subject.
     * Is a "getX()" method.
     *
     * @return the <CODE>subject</CODE> init parameter or null if missing
     */
    protected String getSubject() throws MessagingException {
        if(getInitParameter("subject") == null) {
            return null;
        } else {
            return getInitParameter("subject");
        }
    }

    /**
     * Gets the <CODE>subject</CODE> property,
     * built dynamically using the original Mail object.
     * Is a "getX(Mail)" method.
     *
     * @return {@link #getSubject()}
     */
    protected String getSubject(Mail originalMail) throws MessagingException {
        String subject = (isStatic()) ? this.subject : getSubject();
        return subject;
    }

    /**
     * Gets the <CODE>prefix</CODE> property.
     * Returns a prefix for the new message subject.
     * Is a "getX()" method.
     *
     * @return the <CODE>prefix</CODE> init parameter or an empty string if missing
     */
    protected String getSubjectPrefix() throws MessagingException {
        if(getInitParameter("prefix") == null) {
            return null;
        } else {
            return getInitParameter("prefix");
        }
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
     * Is a "setX(Mail, Tx, Mail)" method.
     */
    protected void setSubjectPrefix(Mail newMail, String subjectPrefix, Mail originalMail) throws MessagingException {
        String subject = getSubject(originalMail);
        if ((subjectPrefix != null && subjectPrefix.length() > 0) || subject != null) {
            if (subject == null) {
                subject = originalMail.getMessage().getSubject();
            } else {
                // replacing the subject
                if (isDebug) {
                    log("subject set to: " + subject);
                }
            }
            // Was null in original?
            if (subject == null) {
                subject = "";
            }
            
            if (subjectPrefix != null) {
                subject = subjectPrefix + subject;
                // adding a prefix
                if (isDebug) {
                    log("subjectPrefix set to: " + subjectPrefix);
                }
            }
//            newMail.getMessage().setSubject(subject);
            changeSubject(newMail.getMessage(), subject);
        }
    }

    /**
     * Gets the <CODE>attachError</CODE> property.
     * Returns a boolean indicating whether to append a description of any error to the main body part
     * of the new message, if getInlineType does not return "UNALTERED".
     * Is a "getX()" method.
     *
     * @return the <CODE>attachError</CODE> init parameter; false if missing
     */
    protected boolean attachError() throws MessagingException {
        return new Boolean(getInitParameter("attachError")).booleanValue();
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
     * @return the <CODE>isReply</CODE> init parameter; false if missing
     */
    protected boolean isReply() throws MessagingException {
        return new Boolean(getInitParameter("isReply")).booleanValue();
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
        isDebug = (getInitParameter("debug") == null) ? false : new Boolean(getInitParameter("debug")).booleanValue();

        isStatic = (getInitParameter("static") == null) ? false : new Boolean(getInitParameter("static")).booleanValue();

        if (isDebug) {
            log("Initializing");
        }
        
        // check that all init parameters have been declared in allowedInitParameters
        checkInitParameters(getAllowedInitParameters());
        
        if(isStatic()) {
            passThrough         = getPassThrough();
            fakeDomainCheck     = getFakeDomainCheck();
            attachmentType      = getAttachmentType();
            inLineType          = getInLineType();
            messageText         = getMessage();
            recipients          = getRecipients();
            replyTo             = getReplyTo();
            reversePath         = getReversePath();
            sender              = getSender();
            subject             = getSubject();
            subjectPrefix       = getSubjectPrefix();
            apparentlyTo        = getTo();
            attachError         = attachError();
            isReply             = isReply();
            if (isDebug) {
                StringBuffer logBuffer =
                    new StringBuffer(1024)
                            .append("static")
                            .append(", passThrough=").append(passThrough)
                            .append(", fakeDomainCheck=").append(fakeDomainCheck)
                            .append(", sender=").append(sender)
                            .append(", replyTo=").append(replyTo)
                            .append(", reversePath=").append(reversePath)
                            .append(", message=").append(messageText)
                            .append(", recipients=").append(arrayToString(recipients == null ? null : recipients.toArray()))
                            .append(", subject=").append(subject)
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

        setTo(newMail, getTo(originalMail), originalMail);

        setSubjectPrefix(newMail, getSubjectPrefix(originalMail), originalMail);

        if(newMail.getMessage().getHeader(RFC2822Headers.DATE) == null) {
            newMail.getMessage().setHeader(RFC2822Headers.DATE, rfc822DateFormat.format(new Date()));
        }

        setReplyTo(newMail, getReplyTo(originalMail), originalMail);

        setReversePath(newMail, getReversePath(originalMail), originalMail);

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
                                    .append(". Consider using the Resend mailet ")
                                    .append("using a different sender.");
            throw new MessagingException(logBuffer.toString());
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
            while ((index = oldName.indexOf('!', index + 1)) >= 0) {
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
     * Gets the MailAddress corresponding to the existing "Return-Path" of
     * <I>mail</I>.
     * If empty returns <CODE>SpecialAddress.NULL</CODE>,
     * if missing return <CODE>null</CODE>.
     * @deprecated The Return-Path header is no longer available until local delivery.
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

        MimeMessage originalMessage = originalMail.getMessage();
        MimeMessage newMessage = newMail.getMessage();

        // Copy the relevant headers
        String[] relevantHeaderNames =
            {RFC2822Headers.DATE,
             RFC2822Headers.FROM,
             RFC2822Headers.REPLY_TO,
             RFC2822Headers.TO,
             RFC2822Headers.SUBJECT,
             RFC2822Headers.RETURN_PATH};
        Enumeration headerEnum = originalMessage.getMatchingHeaderLines(relevantHeaderNames);
        while (headerEnum.hasMoreElements()) {
            newMessage.addHeaderLine((String) headerEnum.nextElement());
        }

        StringWriter sout = new StringWriter();
        PrintWriter out   = new PrintWriter(sout, true);
        String head = getMessageHeaders(originalMessage);
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
                    out.println(getMessageBody(originalMessage));
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
                            part.setText(getMessageBody(originalMessage));
                        } catch(Exception e) {
                            part.setText("body unavailable");
                        }
                        break;
                    case ALL: //ALL:
                        StringBuffer textBuffer =
                            new StringBuffer(1024)
                                .append(head)
                                .append("\r\nMessage:\r\n")
                                .append(getMessageBody(originalMessage));
                        part.setText(textBuffer.toString());
                        break;
                    case MESSAGE: //MESSAGE:
                        part.setContent(originalMessage, "message/rfc822");
                        break;
                }
                if ((originalMessage.getSubject() != null) && (originalMessage.getSubject().trim().length() > 0)) {
                    part.setFileName(originalMessage.getSubject().trim());
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
        if(addressString.compareTo("reversepath") == 0) {
            specialAddress = SpecialAddress.REVERSE_PATH;
        }
        if(addressString.compareTo("from") == 0) {
            specialAddress = SpecialAddress.FROM;
        }
        if(addressString.compareTo("replyto") == 0) {
            specialAddress = SpecialAddress.REPLY_TO;
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
     * <P>Checks if a sender domain of <I>mail</I> is valid.</P>
     * <P>If we do not do this check, and someone uses a redirection mailet in a
     * processor initiated by SenderInFakeDomain, then a fake
     * sender domain will cause an infinite loop (the forwarded
     * e-mail still appears to come from a fake domain).<BR>
     * Although this can be viewed as a configuration error, the
     * consequences of such a mis-configuration are severe enough
     * to warrant protecting against the infinite loop.</P>
     * <P>This check can be skipped if {@link #getFakeDomainCheck(Mail)} returns true.</P> 
     *
     * @param mail the mail object to check
     * @return true if the if the sender is null or
     * {@link org.apache.mailet.MailetContext#getMailServers} returns true for
     * the sender host part
     */
    protected final boolean senderDomainIsValid(Mail mail) throws MessagingException {
        if (getFakeDomainCheck(mail)) {
            return mail.getSender() == null || getMailetContext().getMailServers(mail.getSender().getHost()).size() != 0;
        } else return true;
    }
    
    /**
     * Checks if there are unallowed init parameters specified in the configuration file
     * against the String[] allowedInitParameters.
     */
    private void checkInitParameters(String[] allowedArray) throws MessagingException {
        // if null then no check is requested
        if (allowedArray == null) {
            return;
        }
        
        Collection allowed = new HashSet();
        Collection bad = new ArrayList();
        
        for (int i = 0; i < allowedArray.length; i++) {
            allowed.add(allowedArray[i]);
        }
        
        Iterator iterator = getInitParameterNames();
        while (iterator.hasNext()) {
            String parameter = (String) iterator.next();
            if (!allowed.contains(parameter)) {
                bad.add(parameter);
            }
        }
        
        if (bad.size() > 0) {
            throw new MessagingException("Unexpected init parameters found: "
                                         + arrayToString(bad.toArray()));
        }
    }

    /**
     * It changes the subject of the supplied message to to supplied value 
     * but it also tries to preserve the original charset information.<BR>
     * 
     * This method was needed to avoid sending the subject using a charset
     * (usually the default charset on the server) which doesn't contain
     * the characters in the subject, resulting in the loss of these characters. 
     * The most simple method would be to either send it in ASCII unencoded 
     * or in UTF-8 if non-ASCII characters are present but unfortunately UTF-8 
     * is not yet a MIME standard and not all email clients 
     * are supporting it. The optimal method would be to determine the best 
     * charset by analyzing the actual characters. That would require much 
     * more work (exept if an open source library already exists for this). 
     * However there is nothing to stop somebody to add a detection algorithm
     * for a specific charset. <BR>
     * 
     * The current algorithm works correctly if only ASCII characters are 
     * added to an existing subject.<BR>
     * 
     * If the new value is ASCII only, then it doesn't apply any encoding to
     * the subject header. (This is provided by MimeMessage.setSubject()).<BR>
     * 
     * Possible enhancement:  under java 1.4 java.nio the system can determine if the
     * suggested charset fits or not (if there is untranslatable
     * characters). If the charset doesn't fit the new value, it
     * can fall back to UTF-8.<BR>
     * 
     * @param message the message of which subject is changed 
     * @param newValue the new (unencoded) value of the subject. It must
     *   not be null.
     * @throws MessagingException - according to the JavaMail doc most likely
     *    this is never thrown
     */
    public static void changeSubject(MimeMessage message, String newValue)
            throws MessagingException
    {
        String rawSubject = message.getHeader(RFC2822Headers.SUBJECT, null);
        String mimeCharset = determineMailHeaderEncodingCharset(rawSubject);
        if (mimeCharset == null) { // most likely ASCII
            // it uses the system charset or the value of the
            // mail.mime.charset property if set  
            message.setSubject(newValue);
            return;
        } else { // original charset determined 
            String javaCharset = javax.mail.internet.MimeUtility.javaCharset(mimeCharset);
            try {
                message.setSubject(newValue, javaCharset);
            } catch (MessagingException e) {
                // known, but unsupported encoding
                // this should be logged, the admin may setup a more i18n
                // capable JRE, but the log API cannot be accessed from here  
                //if (charset != null) log(charset + 
                //      " charset unsupported by the JRE, email subject may be damaged");
                message.setSubject(newValue); // recover
            }
        }
    }
     
    /**
     * It attempts to determine the charset used to encode an "unstructured" 
     * RFC 822 header (like Subject). The encoding is specified in RFC 2047.
     * If it cannot determine or the the text is not encoded then it returns null.
     *
     * Here is an example raw text: 
     * Subject: =?iso-8859-2?Q?leg=FAjabb_pr=F3ba_l=F5elemmel?=
     *
     * @param rawText the raw (not decoded) value of the header. Null means
     *   that the header was not present (in this case it always return null).
     * @return the MIME charset name or null if no encoding applied
     */
    static private String determineMailHeaderEncodingCharset(String rawText)
    {
        if (rawText == null) return null;
        int iEncodingPrefix = rawText.indexOf("=?");
        if (iEncodingPrefix == -1) return null;
        int iCharsetBegin = iEncodingPrefix + 2; 
        int iSecondQuestionMark = rawText.indexOf('?', iCharsetBegin);
        if (iSecondQuestionMark == -1) return null;
        // safety checks
        if (iSecondQuestionMark == iCharsetBegin) return null; // empty charset? impossible
        int iThirdQuestionMark = rawText.indexOf('?', iSecondQuestionMark + 1);
        if (iThirdQuestionMark == -1) return null; // there must be one after encoding
        if (-1 == rawText.indexOf("?=", iThirdQuestionMark + 1)) return null; // closing tag
        String mimeCharset = rawText.substring(iCharsetBegin, iSecondQuestionMark);
        return mimeCharset;
    }
    
    /**
     * Returns a new Collection built over <I>list</I> replacing special addresses
     * with real <CODE>MailAddress</CODE>-es.<BR>
     * Manages <CODE>SpecialAddress.SENDER</CODE>, <CODE>SpecialAddress.REVERSE_PATH</CODE>,
     * <CODE>SpecialAddress.FROM</CODE>, <CODE>SpecialAddress.REPLY_TO</CODE>, 
     * <CODE>SpecialAddress.RECIPIENTS</CODE>, <CODE>SpecialAddress.TO</CODE>, 
     * <CODE>SpecialAddress.NULL</CODE> and <CODE>SpecialAddress.UNALTERED</CODE>.<BR>
     * <CODE>SpecialAddress.FROM</CODE> is made equivalent to <CODE>SpecialAddress.SENDER</CODE>;
     * <CODE>SpecialAddress.TO</CODE> is made equivalent to <CODE>SpecialAddress.RECIPIENTS</CODE>.<BR>
     * <CODE>SpecialAddress.REPLY_TO</CODE> uses the ReplyTo header if available, otherwise the
     * From header if available, otherwise the Sender header if available, otherwise the return-path.<BR>
     * <CODE>SpecialAddress.NULL</CODE> and <CODE>SpecialAddress.UNALTERED</CODE> are ignored.<BR>
     * Any other address is not replaced.
     */
    protected Collection replaceMailAddresses(Mail mail, Collection list) {
        Collection newList = new HashSet(list.size());
        Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            MailAddress mailAddress = (MailAddress) iterator.next();
            if (!mailAddress.getHost().equalsIgnoreCase("address.marker")) {
                newList.add(mailAddress);
            } else if (mailAddress == SpecialAddress.SENDER || mailAddress == SpecialAddress.FROM) {
                MailAddress sender = mail.getSender();
                if (sender != null) {
                    newList.add(sender);
                }
            } else if (mailAddress == SpecialAddress.REPLY_TO) {
                int parsedAddressCount = 0;
                try {
                    InternetAddress[] replyToArray = (InternetAddress[]) mail.getMessage().getReplyTo();
                    if (replyToArray != null) {
                        for (int i = 0; i < replyToArray.length; i++) {
                            try {
                                newList.add(new MailAddress(replyToArray[i]));
                                parsedAddressCount++;
                            } catch (ParseException pe) {
                                log("Unable to parse a \"REPLY_TO\" header address in the original message: " + replyToArray[i] + "; ignoring.");
                            }
                        }
                    }
                } catch (MessagingException ae) {
                    log("Unable to parse the \"REPLY_TO\" header in the original message; ignoring.");
                }
                // no address was parsed?
                if (parsedAddressCount == 0) {
                    MailAddress sender = mail.getSender();
                    if (sender != null) {
                        newList.add(sender);
                    }
                }
            } else if (mailAddress == SpecialAddress.REVERSE_PATH) {
                MailAddress reversePath = mail.getSender();
                if (reversePath != null) {
                    newList.add(reversePath);
                }
            } else if (mailAddress == SpecialAddress.RECIPIENTS || mailAddress == SpecialAddress.TO) {
                newList.addAll(mail.getRecipients());
            } else if (mailAddress == SpecialAddress.UNALTERED) {
                continue;
            } else if (mailAddress == SpecialAddress.NULL) {
                continue;
            } else {
                newList.add(mailAddress);
            }
        }
        return newList;
    }

    /**
     * Returns a new Collection built over <I>list</I> replacing special addresses
     * with real <CODE>InternetAddress</CODE>-es.<BR>
     * Manages <CODE>SpecialAddress.SENDER</CODE>, <CODE>SpecialAddress.REVERSE_PATH</CODE>,
     * <CODE>SpecialAddress.FROM</CODE>, <CODE>SpecialAddress.REPLY_TO</CODE>,
     * <CODE>SpecialAddress.RECIPIENTS</CODE>, <CODE>SpecialAddress.TO</CODE>, 
     * <CODE>SpecialAddress.NULL</CODE> and <CODE>SpecialAddress.UNALTERED</CODE>.<BR>
     * <CODE>SpecialAddress.RECIPIENTS</CODE> is made equivalent to <CODE>SpecialAddress.TO</CODE>.<BR>
     * <CODE>SpecialAddress.FROM</CODE> uses the From header if available, otherwise the Sender header if available,
     * otherwise the return-path.<BR>
     * <CODE>SpecialAddress.REPLY_TO</CODE> uses the ReplyTo header if available, otherwise the
     * From header if available, otherwise the Sender header if available, otherwise the return-path.<BR>
     * <CODE>SpecialAddress.UNALTERED</CODE> is ignored.<BR>
     * Any other address is not replaced.<BR>
     */
    protected Collection replaceInternetAddresses(Mail mail, Collection list) throws MessagingException {
        Collection newList = new HashSet(list.size());
        Iterator iterator = list.iterator();
        while (iterator.hasNext()) {
            InternetAddress internetAddress = (InternetAddress) iterator.next();
            MailAddress mailAddress = new MailAddress(internetAddress);
            if (!mailAddress.getHost().equalsIgnoreCase("address.marker")) {
                newList.add(internetAddress);
            } else if (internetAddress.equals(SpecialAddress.SENDER.toInternetAddress())) {
                MailAddress sender = mail.getSender();
                if (sender != null) {
                    newList.add(sender.toInternetAddress());
                }
            } else if (internetAddress.equals(SpecialAddress.REVERSE_PATH.toInternetAddress())) {
                MailAddress reversePath = mail.getSender();
                if (reversePath != null) {
                    newList.add(reversePath.toInternetAddress());
                }
            } else if (internetAddress.equals(SpecialAddress.FROM.toInternetAddress())) {
                try {
                    InternetAddress[] fromArray = (InternetAddress[]) mail.getMessage().getFrom();
                    if (fromArray != null) {
                        for (int i = 0; i < fromArray.length; i++) {
                            newList.add(fromArray[i]);
                        }
                    } else {
                        MailAddress reversePath = mail.getSender();
                        if (reversePath != null) {
                            newList.add(reversePath.toInternetAddress());
                        }
                    }
                } catch (MessagingException me) {
                    log("Unable to parse the \"FROM\" header in the original message; ignoring.");
                }
            } else if (internetAddress.equals(SpecialAddress.REPLY_TO.toInternetAddress())) {
                try {
                    InternetAddress[] replyToArray = (InternetAddress[]) mail.getMessage().getReplyTo();
                    if (replyToArray != null) {
                        for (int i = 0; i < replyToArray.length; i++) {
                            newList.add(replyToArray[i]);
                        }
                    } else {
                        MailAddress reversePath = mail.getSender();
                        if (reversePath != null) {
                            newList.add(reversePath.toInternetAddress());
                        }
                    }
                } catch (MessagingException me) {
                    log("Unable to parse the \"REPLY_TO\" header in the original message; ignoring.");
                }
            } else if (internetAddress.equals(SpecialAddress.TO.toInternetAddress())
                       || internetAddress.equals(SpecialAddress.RECIPIENTS.toInternetAddress())) {
                try {
                    String[] toHeaders = mail.getMessage().getHeader(RFC2822Headers.TO);
                    if (toHeaders != null) {
                        for (int i = 0; i < toHeaders.length; i++) {
                            try {
                                InternetAddress[] originalToInternetAddresses = InternetAddress.parse(toHeaders[i], false);
                                for (int j = 0; j < originalToInternetAddresses.length; j++) {
                                    newList.add(originalToInternetAddresses[j]);
                                }
                            } catch (MessagingException ae) {
                                log("Unable to parse a \"TO\" header address in the original message: " + toHeaders[i] + "; ignoring.");
                            }
                        }
                    }
                } catch (MessagingException ae) {
                    log("Unable to parse the \"TO\" header  in the original message; ignoring.");
                }
            } else if (internetAddress.equals(SpecialAddress.UNALTERED.toInternetAddress())) {
                continue;
            } else if (internetAddress.equals(SpecialAddress.NULL.toInternetAddress())) {
                continue;
            } else {
                newList.add(internetAddress);
            }
        }
        return newList;
    }

}
