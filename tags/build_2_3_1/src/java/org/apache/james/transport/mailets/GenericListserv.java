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

import org.apache.mailet.RFC2822Headers;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetException;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

/**
 * An abstract implementation of a listserv.  The underlying implementation must define
 * various settings, and can vary in their individual configuration.  Supports restricting
 * to members only, allowing attachments or not, sending replies back to the list, and an
 * optional subject prefix.
 */
public abstract class GenericListserv extends GenericMailet {

    /**
     * Returns a Collection of MailAddress objects of members to receive this email
     */
    public abstract Collection getMembers() throws MessagingException;

    /**
     * Returns whether this list should restrict to senders only
     */
    public abstract boolean isMembersOnly() throws MessagingException;

    /**
     * Returns whether this listserv allow attachments
     */
    public abstract boolean isAttachmentsAllowed() throws MessagingException;

    /**
     * Returns whether listserv should add reply-to header
     */
    public abstract boolean isReplyToList() throws MessagingException;

    /**
     * The email address that this listserv processes on.  If returns null, will use the
     * recipient of the message, which hopefully will be the correct email address assuming
     * the matcher was properly specified.
     */
    public MailAddress getListservAddress() throws MessagingException {
        return null;
    }

    /**
     * An optional subject prefix.
     */
    public abstract String getSubjectPrefix() throws MessagingException;

    /**
     * Should the subject prefix be automatically surrounded by [].
     *
     * @return whether the subject prefix will be surrounded by []
     *
     * @throws MessagingException never, for this implementation
     */
    public boolean isPrefixAutoBracketed() throws MessagingException {
        return true; // preserve old behavior unless subclass overrides.
    }

    /**
     * <p>This takes the subject string and reduces (normailzes) it.
     * Multiple "Re:" entries are reduced to one, and capitalized.  The
     * prefix is always moved/placed at the beginning of the line, and
     * extra blanks are reduced, so that the output is always of the
     * form:</p>
     * <code>
     * &lt;prefix&gt; + &lt;one-optional-"Re:"*gt; + &lt;remaining subject&gt;
     * </code>
     * <p>I have done extensive testing of this routine with a standalone
     * driver, and am leaving the commented out debug messages so that
     * when someone decides to enhance this method, it can be yanked it
     * from this file, embedded it with a test driver, and the comments
     * enabled.</p>
     */
    static private String normalizeSubject(final String subj, final String prefix) {
        // JDK IMPLEMENTATION NOTE!  When we require JDK 1.4+, all
        // occurrences of subject.toString.().indexOf(...) can be
        // replaced by subject.indexOf(...).

        StringBuffer subject = new StringBuffer(subj);
        int prefixLength = prefix.length();

        // System.err.println("In:  " + subject);

        // If the "prefix" is not at the beginning the subject line, remove it
        int index = subject.toString().indexOf(prefix);
        if (index != 0) {
            // System.err.println("(p) index: " + index + ", subject: " + subject);
            if (index > 0) {
                subject.delete(index, index + prefixLength);
            }
            subject.insert(0, prefix); // insert prefix at the front
        }

        // Replace Re: with RE:
        String match = "Re:";
        index = subject.toString().indexOf(match, prefixLength);

        while(index > -1) {
            // System.err.println("(a) index: " + index + ", subject: " + subject);
            subject.replace(index, index + match.length(), "RE:");
            index = subject.toString().indexOf(match, prefixLength);
            // System.err.println("(b) index: " + index + ", subject: " + subject);
        }

        // Reduce them to one at the beginning
        match ="RE:";
        int indexRE = subject.toString().indexOf(match, prefixLength) + match.length();
        index = subject.toString().indexOf(match, indexRE);
        while(index > 0) {
            // System.err.println("(c) index: " + index + ", subject: " + subject);
            subject.delete(index, index + match.length());
            index = subject.toString().indexOf(match, indexRE);
            // System.err.println("(d) index: " + index + ", subject: " + subject);
        }

        // Reduce blanks
        match = "  ";
        index = subject.toString().indexOf(match, prefixLength);
        while(index > -1) {
            // System.err.println("(e) index: " + index + ", subject: " + subject);
            subject.replace(index, index + match.length(), " ");
            index = subject.toString().indexOf(match, prefixLength);
            // System.err.println("(f) index: " + index + ", subject: " + subject);
        }


        // System.err.println("Out: " + subject);

        return subject.toString();
    }
    
    /**
     * Processes the message.  Assumes it is the only recipient of this forked message.
     */
    public final void service(Mail mail) throws MessagingException {
        try {
            Collection members = getMembers();

            //Check for members only flag....
            if (isMembersOnly() && !members.contains(mail.getSender())) {
                //Need to bounce the message to say they can't send to this list
                getMailetContext().bounce(mail, "Only members of this listserv are allowed to send a message to this address.");
                mail.setState(Mail.GHOST);
                return;
            }

            //Check for no attachments
            if (!isAttachmentsAllowed() && mail.getMessage().getContent() instanceof MimeMultipart) {
                getMailetContext().bounce(mail, "You cannot send attachments to this listserv.");
                mail.setState(Mail.GHOST);
                return;
            }

            //Create a copy of this message to send out
            MimeMessage message = new MimeMessage(mail.getMessage());
            //We need to remove this header from the copy we're sending around
            message.removeHeader(RFC2822Headers.RETURN_PATH);

            //Figure out the listserv address.
            MailAddress listservAddr = getListservAddress();
            if (listservAddr == null) {
                //Use the recipient
                listservAddr = (MailAddress)mail.getRecipients().iterator().next();
            }

            //Check if the X-been-there header is set to the listserv's name
            //  (the address).  If it has, this means it's a message from this
            //  listserv that's getting bounced back, so we need to swallow it
            if (listservAddr.equals(message.getHeader("X-been-there"))) {
                mail.setState(Mail.GHOST);
                return;
            }

            //Set the subject if set
            String prefix = getSubjectPrefix();
            if (prefix != null) {
                if (isPrefixAutoBracketed()) {
                    StringBuffer prefixBuffer =
                        new StringBuffer(64)
                            .append("[")
                            .append(prefix)
                            .append("] ");
                    prefix = prefixBuffer.toString();
                }
                String subj = message.getSubject();
                if (subj == null) {
                    subj = "";
                }
                subj = normalizeSubject(subj, prefix);
                AbstractRedirect.changeSubject(message, subj);
            }

            //If replies should go to this list, we need to set the header
            if (isReplyToList()) {
                message.setHeader(RFC2822Headers.REPLY_TO, listservAddr.toString());
            }
            //We're going to set this special header to avoid bounces
            //  getting sent back out to the list
            message.setHeader("X-been-there", listservAddr.toString());

            //Send the message to the list members
            //We set the postmaster as the sender for now so bounces go to him/her
            getMailetContext().sendMail(getMailetContext().getPostmaster(), members, message);

            //Kill the old message
            mail.setState(Mail.GHOST);
        } catch (IOException ioe) {
            throw new MailetException("Error creating listserv message", ioe);
        }
    }
}
