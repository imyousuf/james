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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.james.services.JamesUser;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetConfig;
import org.apache.mailet.UsersRepository;

/**
 * Receives a Mail from JamesSpoolManager and takes care of delivery of the
 * message to local inboxes.
 */
public class LocalDelivery extends GenericMailet {
    private boolean enableAliases;
    private boolean enableForwarding;
    private boolean ignoreCase;
    private String inboxURI;
    private UsersRepository localusers;
    private String users;

    /**
     * Return a string describing this mailet.
     *
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Local Delivery Mailet";
    }

    /**
     * @see org.apache.mailet.Mailet#init(org.apache.mailet.MailetConfig)
     */
    public void init(MailetConfig newConfig) throws MessagingException {
        super.init(newConfig);
        if (newConfig.getInitParameter("inboxURI") != null) {
            inboxURI = newConfig.getInitParameter("inboxURI");
        } else {
            log("No inboxURI defined for LocalDelivery");
        }
        if (newConfig.getInitParameter("users") != null) {
            users = newConfig.getInitParameter("users");
            localusers = getMailetContext().getUserRepository(users);
        } else {
            log("No users repository defined for LocalDelivery");
        }
        if (newConfig.getInitParameter("ignoreCase") != null) {
            ignoreCase = Boolean.valueOf(newConfig.getInitParameter("ignoreCase")).booleanValue();
        } else {
            ignoreCase = false;
        }
        if (newConfig.getInitParameter("enableAliases") != null) {
            enableAliases = Boolean.valueOf(newConfig.getInitParameter("enableAliases")).booleanValue();
        } else {
            enableAliases = false;
        }
        if (newConfig.getInitParameter("enableForwarding") != null) {
            enableForwarding = Boolean.valueOf(newConfig.getInitParameter("enableForwarding")).booleanValue();
        } else {
            enableForwarding = false;
        }
    }

    /* MimeMessage that does NOT change the headers when we save it */
    class LocalMimeMessage extends MimeMessage {

        public LocalMimeMessage(MimeMessage source) throws MessagingException {
            super(source);
        }

        protected void updateHeaders() throws MessagingException {
            if (getMessageID() == null) super.updateHeaders();
        }
    }

    /**
     * Delivers a mail to a local mailbox.
     *
     * @param mail the mail being processed
     *
     * @throws MessagingException if an error occurs while storing the mail
     */
    public void service(Mail mail) throws MessagingException {
        Collection recipients = mail.getRecipients();
        Collection errors = new Vector();
        if (mail == null) {
            throw new IllegalArgumentException("Mail message to be stored cannot be null.");
        }
        MimeMessage message = mail.getMessage();
        for (Iterator i = recipients.iterator(); i.hasNext();) {
            MailAddress recipient = (MailAddress)i.next();
            String username = null;
            if (recipient == null) {
                throw new IllegalArgumentException("Recipient for mail to be stored cannot be null.");
            }
            if (ignoreCase) {
                username = localusers.getRealName(recipient.getUser());
            } else if (localusers.contains(recipient.getUser())) {
                username = recipient.getUser();
            }
            if (username == null) {
                StringBuffer errorBuffer =
                    new StringBuffer(128).append("The inbox for user ").append(recipient.getUser()).append(
                        " was not found on this server.");
                throw new MessagingException(errorBuffer.toString());
            }
            if ((JamesUser)localusers.getUserByName(username) instanceof JamesUser) {
                JamesUser user = (JamesUser)localusers.getUserByName(username);
                if (enableAliases || enableForwarding) {
                    if (enableAliases && user.getAliasing()) {
                        username = user.getAlias();
                    }
                    // Forwarding takes precedence over local aliases
                    if (enableForwarding && user.getForwarding()) {
                        MailAddress forwardTo = user.getForwardingDestination();
                        if (forwardTo == null) {
                            StringBuffer errorBuffer =
                                new StringBuffer(128).append("Forwarding was enabled for ").append(
                                    username).append(
                                    " but no forwarding address was set for this account.");
                            throw new MessagingException(errorBuffer.toString());
                        }
                        recipients = new HashSet();
                        recipients.add(forwardTo);
                        try {
                            // Add qmail's de facto standard Delivered-To header
                            MimeMessage localMessage = new LocalMimeMessage(message);
                            localMessage.addHeader("Delivered-To", recipient.toString());
                            localMessage.saveChanges();

                            getMailetContext().sendMail(mail.getSender(), recipients, localMessage);
                            StringBuffer logBuffer =
                                new StringBuffer(128).append("Mail for ").append(username).append(
                                    " forwarded to ").append(
                                    forwardTo.toString());
                            log(logBuffer.toString());
                            return;
                        } catch (MessagingException me) {
                            StringBuffer logBuffer =
                                new StringBuffer(128).append("Error forwarding mail to ").append(
                                    forwardTo.toString()).append(
                                    "attempting local delivery");
                            log(logBuffer.toString());
                            throw me;
                        }
                    }
                }
            }
            try {
                getMailetContext().getMailRepository(inboxURI + recipient.getUser() + "/").store(mail);
            } catch (Exception ex) {
                getMailetContext().log("Error while storing mail.", ex);
                errors.add(recipient);
            }
        }
        if (!errors.isEmpty()) {
            // If there were errors, we redirect the email to the ERROR processor.
            // In order for this server to meet the requirements of the SMTP specification,
            // mails on the ERROR processor must be returned to the sender.  Note that this
            // email doesn't include any details regarding the details of the failure(s).
            // In the future we may wish to address this.
            getMailetContext().sendMail(mail.getSender(), errors, message, Mail.ERROR);
        }
        //We always consume this message
        mail.setState(Mail.GHOST);
    }
}
