/*****************************************************************************
 * Copyright (C) The Apache Software Foundation. All rights reserved.        *
 * ------------------------------------------------------------------------- *
 * This software is published under the terms of the Apache Software License *
 * version 1.1, a copy of which has been included  with this distribution in *
 * the LICENSE file.                                                         *
 *****************************************************************************/

package org.apache.james.transport.servlet;

import java.util.*;
import javax.mail.*;
import javax.mail.internet.*;
import org.apache.java.lang.*;
import org.apache.james.*;
import org.apache.avalon.interfaces.*;
import org.apache.mail.*;
import org.apache.james.transport.*;

/**
 * Routes a given email address to a list of members, based on simple
 * listserv settings
 *
 * @author  Serge Knystautas <sergek@lokitech.com>
 */
public abstract class GenericListserv extends GenericMailServlet {
    private InternetAddress postmaster;

    protected abstract Vector getMembers();

    /**
     * Whether this list restricts posting to members only
     */
    protected boolean isMembersOnly() {
        return false;
    }

    /**
     * Whether this listserv allowed attachments
     */
    protected boolean isAttachmentsAllowed() {
        return true;
    }

    /**
     * Whether the list sets itself as the reply-to target
     */
    protected boolean isReplyToList() {
        return true;
    }

    /**
     * The optional subject prefix for "[subject] original subject"
     */
    protected String getSubject() {
        return null;
    }

    /**
     * The listserv address to put in the reply address (and for bounced emails)
     */
    protected String getListservAddress() {
        return null;
    }

    /**
     * Check for the appropriate restrictions to the list, and then send the
     * message to all members if valid.
     */
    public Mail service(Mail mail) {
        try {
            Vector members = getMembers ();
            MimeMessage message = mail.getMessage ();

            if (isMembersOnly()) {
                //Determine whether a member sent this message
                boolean fromMember = false;
                String sender = mail.getSender();
                for (int i = 0; i < members.size(); i++) {
                    if (members.elementAt(i).equals(sender)) {
                        fromMember = true;
                        break;
                    }
                }

                if (!fromMember) {
                    return bounce(mail, "Only members can send emails to this listserv.");
                }
            }
            if (!isAttachmentsAllowed()) {
                //Determine whether message has attachments
                boolean containsAttachment = message.getContentType().startsWith("multipart");
                if (containsAttachment) {
                    return bounce(mail, "You cannot send attachments to this listserv.");
                }
            }
            if (getSubject() != null) {
                //Rewrite the subject to contain the [Subject] prefix
                String subj = message.getSubject ();
                String wrappedSubject = '[' + getSubject() + ']';
                //If the "subject" is in the subject line, remove it and everything before it
                int index = subj.indexOf (wrappedSubject);
                if (index > -1) {
                    if (index == 0) {
                        subj = wrappedSubject + ' ' + subj.substring (index + wrappedSubject.length () + 1);
                    } else {
                        subj = wrappedSubject + ' ' + subj.substring (0, index) + subj.substring (index + wrappedSubject.length () + 1);
                    }
                } else {
                    subj = wrappedSubject + ' ' + subj;
                }
                message.setSubject(subj);
            }

            //Set the reply-to as this list
            if (isReplyToList() && getListservAddress() != null) {
                message.setHeader("Reply-To", getListservAddress());
            }

            //Forward message to the members
            log("Forwarding mail " + mail.getName() + " to " + members);
            mail.setRecipients(members);
        } catch (MessagingException me) {
            log("Exception while retrieving message " + mail.getName ()+ ": " + me.getMessage());
        }
        return mail;
    }

    public String getServletInfo() {
        return "GenericListserv";
    }

    protected Mail bounce(Mail mail, String errorMessage) throws MessagingException {
        //This sends a message to the james component that is a bounce of the sent message
        MimeMessage reply = (MimeMessage) (mail.getMessage()).reply(false);
        reply.setSubject("Message to " + getListservAddress () + " was bounced.");
        Vector recipients = new Vector ();
        recipients.addElement (mail.getSender ());
        InternetAddress addr[] = {new InternetAddress(mail.getSender())};
        reply.setRecipients(Message.RecipientType.TO, addr);
        reply.setFrom(new InternetAddress(mail.getRecipients().elementAt(0).toString()));

        mail.setSender (mail.getRecipients().elementAt(0).toString());
        mail.setMessage (reply);
        mail.setRecipients (recipients);
        return mail;
    }
}

