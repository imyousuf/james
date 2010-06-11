/****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one   *
 * or more contributor license agreements.  See the NOTICE file *
 * distributed with this work for additional information        *
 * regarding copyright ownership.  The ASF licenses this file   *
 * to you under the Apache License, Version 2.0 (the            *
 * "License"); you may not use this file except in compliance   *
 * with the License.  You may obtain a copy of the License at   *
 *                                                              *
 *   http://www.apache.org/licenses/LICENSE-2.0                 *
 *                                                              *
 * Unless required by applicable law or agreed to in writing,   *
 * software distributed under the License is distributed on an  *
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY       *
 * KIND, either express or implied.  See the License for the    *
 * specific language governing permissions and limitations      *
 * under the License.                                           *
 ****************************************************************/



package org.apache.james.transport.mailets;

import org.apache.mailet.base.RFC2822Headers;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.PrintWriter;
import java.io.StringWriter;
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
 * <LI>Supports by default the <CODE>passThrough</CODE> init parameter (true if missing).</LI>
 * </UL>
 *
 * <P>Sample configuration common to all notification mailet subclasses:</P>
 * <PRE><CODE>
 * &lt;mailet match="All" class="<I>a notification mailet</I>">
 *   &lt;sender&gt;<I>an address or postmaster or sender or unaltered, default=postmaster</I>&lt;/sender&gt;
 *   &lt;attachError&gt;<I>true or false, default=false</I>&lt;/attachError&gt;
 *   &lt;message&gt;<I>notice attached to the original message text (optional)</I>&lt;/message&gt;
 *   &lt;prefix&gt;<I>optional subject prefix prepended to the original message</I>&lt;/prefix&gt;
 *   &lt;inline&gt;<I>see {@link Redirect}, default=none</I>&lt;/inline&gt;
 *   &lt;attachment&gt;<I>see {@link Redirect}, default=message</I>&lt;/attachment&gt;
 *   &lt;passThrough&gt;<I>true or false, default=true</I>&lt;/passThrough&gt;
 *   &lt;fakeDomainCheck&gt;<I>true or false, default=true</I>&lt;/fakeDomainCheck&gt;
 *   &lt;debug&gt;<I>true or false, default=false</I>&lt;/debug&gt;
 * &lt;/mailet&gt;
 * </CODE></PRE>
 * <P><I>notice</I> and <I>senderAddress</I> can be used instead of
 * <I>message</I> and <I>sender</I>; such names are kept for backward compatibility.</P>
 *
 * @version CVS $Revision$ $Date$
 * @since 2.2.0
 */
public abstract class AbstractNotify extends AbstractRedirect {

    /* ******************************************************************** */
    /* ****************** Begin of getX and setX methods ****************** */
    /* ******************************************************************** */

    /**
     * @return the <CODE>passThrough</CODE> init parameter, or true if missing
     */
    protected boolean getPassThrough() throws MessagingException {
        return new Boolean(getInitParameter("passThrough","true")).booleanValue();
    }

    /**
     * @return the <CODE>inline</CODE> init parameter, or <CODE>NONE</CODE> if missing
     */
    protected int getInLineType() throws MessagingException {
        return getTypeCode(getInitParameter("inline","none"));
    }

    /**
     * @return the <CODE>attachment</CODE> init parameter, or <CODE>MESSAGE</CODE> if missing
     */
    protected int getAttachmentType() throws MessagingException {
        return getTypeCode(getInitParameter("attachment","message"));
    }

    /**
     * @return the <CODE>notice</CODE> init parameter,
     * or the <CODE>message</CODE> init parameter if missing,
     * or a default string if both are missing
     */
    protected String getMessage() {
        return getInitParameter("notice",
                getInitParameter("message",
                "We were unable to deliver the attached message because of an error in the mail server."));
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

    // All subclasses of AbstractNotify are expected to establish their own recipients
    abstract protected Collection getRecipients() throws MessagingException;

    /**
     * @return null
     */
    protected InternetAddress[] getTo() throws MessagingException {
        return null;
    }

    /**
     * @return <CODE>SpecialAddress.NULL</CODE>, that will remove the "ReplyTo:" header
     */
    protected MailAddress getReplyTo() throws MessagingException {
        return SpecialAddress.NULL;
    }

    /**
     * @return {@link AbstractRedirect#getSender(Mail)}, meaning the new requested sender if any
     */
    protected MailAddress getReversePath(Mail originalMail) throws MessagingException {
        return getSender(originalMail);
    }

    /**
     * @return the <CODE>sendingAddress</CODE> init parameter
     * or the <CODE>sender</CODE> init parameter
     * or the postmaster address if both are missing;
     * possible special addresses returned are
     * <CODE>SpecialAddress.SENDER</CODE>
     * and <CODE>SpecialAddress.UNALTERED</CODE>
     */
    protected MailAddress getSender() throws MessagingException {
        String addressString = getInitParameter("sendingAddress",getInitParameter("sender"));
        
        if (addressString == null) {
            return getMailetContext().getPostmaster();
        }
        
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

    /**
     * @return null
     */
    protected String getSubject() throws MessagingException {
        return null;
    }

    /**
     * @return the <CODE>prefix</CODE> init parameter or "Re:" if missing
     */
    protected String getSubjectPrefix() {
        return getInitParameter("prefix","Re:");
    }

    /**
     * Builds the subject of <I>newMail</I> appending the subject
     * of <I>originalMail</I> to <I>subjectPrefix</I>, but avoiding a duplicate.
     */
    protected void setSubjectPrefix(Mail newMail, String subjectPrefix, Mail originalMail) throws MessagingException {
        String subject = originalMail.getMessage().getSubject();
        if (subject == null) {
            subject = "";
        }
        if (subjectPrefix==null || subject.indexOf(subjectPrefix) == 0) {
            newMail.getMessage().setSubject(subject);
        } else {
            newMail.getMessage().setSubject(subjectPrefix + subject);
        }
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
