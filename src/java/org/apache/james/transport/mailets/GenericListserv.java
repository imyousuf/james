/*
 * Copyright (C) The Apache Software Foundation. All rights reserved.
 *
 * This software is published under the terms of the Apache Software License
 * version 1.1, a copy of which has been included with this distribution in
 * the LICENSE file.
 */
package org.apache.james.transport.mailets;

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
     * An optional subject prefix which will be surrounded by [].
     */
    public abstract String getSubjectPrefix() throws MessagingException;

    /**
     * Processes the message.  Assumes it is the only recipient of this forked message.
     */
    public final void service(Mail mail) throws MessagingException {
        try {
            Collection members = new Vector();
            members.addAll(getMembers());

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
            message.removeHeader("Return-Path");

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
            if (getSubjectPrefix() != null) {
                StringBuffer prefixBuffer =
                    new StringBuffer(64)
                        .append("[")
                        .append(getSubjectPrefix())
                        .append("]");
                String prefix = prefixBuffer.toString();
                String subj = message.getSubject();
                if (subj == null) {
                    subj = "";
                }
                //replace Re: with RE:
                String re ="Re:";
                int index = subj.indexOf(re);
                while(index > -1){
                    subj = subj.substring(0, index) + "RE:" + subj.substring(index + re.length() + 1);
                    index = subj.indexOf(re);
                }
                //reduce them to one at the beginning
                re ="RE:";
                index = subj.indexOf(re,re.length());
                System.err.println("3i-"+index);
                while(index > 0){
                    subj = subj.substring(0, index) + subj.substring(index + re.length() + 1);
                    index = subj.indexOf(re,1);
                }
                //If the "prefix" is in the subject line, remove it and everything before it
                index = subj.indexOf(prefix);
                if (index > -1) {
                    if (index == 0) {
                        subj = prefix + ' ' + subj.substring(index + prefix.length() + 1);
                    } else {
                        subj = prefix + ' ' + subj.substring(0, index) + subj.substring(index + prefix.length() + 1);
                    }
                } else {
                    subj = prefix + ' ' + subj;
                }
                message.setSubject(subj);
            }

            //If replies should go to this list, we need to set the header
            if (isReplyToList()) {
                message.setHeader("Reply-To", listservAddr.toString());
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
