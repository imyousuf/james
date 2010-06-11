/***********************************************************************
 * Copyright (c) 2000-2004 The Apache Software Foundation.             *
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
 * 
 * IMPORTANT now requires a users repository to be explicitly set for 
 * 
 */
public class LocalDelivery extends GenericMailet {
    private boolean enableAliases;
    private boolean enableForwarding;
    private boolean ignoreCase;
    private String inboxURI;
    private UsersRepository localusers;
    private String users;
    private boolean vhost = true;
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
            log("No inboxURI defined for LocalDelivery, Will deliver to globally specified inbox");
            vhost = false;
        }
        if (newConfig.getInitParameter("users") != null) {
            users = newConfig.getInitParameter("users");
            localusers = getMailetContext().getUserRepository(users);
        } else {
            log("No users repository defined for LocalDelivery, Will deliver to globally specified inbox");
            vhost = false;
        }
        if (newConfig.getInitParameter("ignoreCase") != null) {
            ignoreCase =
                Boolean
                    .valueOf(newConfig.getInitParameter("ignoreCase"))
                    .booleanValue();
        } else {
            ignoreCase = false;
        }
        if (newConfig.getInitParameter("enableAliases") != null) {
            enableAliases =
                Boolean
                    .valueOf(newConfig.getInitParameter("enableAliases"))
                    .booleanValue();
        } else {
            enableAliases = false;
        }
        if (newConfig.getInitParameter("enableForwarding") != null) {
            enableForwarding =
                Boolean
                    .valueOf(newConfig.getInitParameter("enableForwarding"))
                    .booleanValue();
        } else {
            enableForwarding = false;
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
            MailAddress recipient = (MailAddress) i.next();
            String username = null;
            if (recipient == null) {
                throw new IllegalArgumentException("Recipient for mail to be stored cannot be null.");
            }
            // Add qmail's de facto standard Delivered-To header
            MimeMessage localMessage = new MimeMessage(message) {
                protected void updateHeaders() throws MessagingException {
                    if (getMessageID() == null)
                        super.updateHeaders();
                    else {
                        modified = false;
                    }
                }
            };
            localMessage.addHeader("Delivered-To", recipient.toString());
            localMessage.saveChanges();
            if (!vhost) {
                //use the basic local storage method of the API
                try {
                    getMailetContext().storeMail(
                        mail.getSender(),
                        recipient,
                        mail.getMessage());
                } catch (Exception ex) {
                    getMailetContext().log("Error while storing mail.", ex);
                    errors.add(recipient);
                }
            } else {
                if (ignoreCase) {
                    username = localusers.getRealName(recipient.getUser());
                } else if (localusers.contains(recipient.getUser())) {
                    username = recipient.getUser();
                }
                if (username == null) {
                    StringBuffer errorBuffer =
                        new StringBuffer(128)
                            .append("The inbox for user ")
                            .append(recipient.getUser())
                            .append(" was not found on this server.");
                    throw new MessagingException(errorBuffer.toString());
                }
                if ((JamesUser) localusers.getUserByName(username)
                    instanceof JamesUser) {
                    JamesUser user =
                        (JamesUser) localusers.getUserByName(username);
                    //            }else{
                    //                //JamesUser user =null;
                    if (enableAliases || enableForwarding) {
                        if (enableAliases && user.getAliasing()) {
                            username = user.getAlias();
                        }
                        // Forwarding takes precedence over local aliases
                        if (enableForwarding && user.getForwarding()) {
                            MailAddress forwardTo =
                                user.getForwardingDestination();
                            if (forwardTo == null) {
                                StringBuffer errorBuffer =
                                    new StringBuffer(128)
                                        .append("Forwarding was enabled for ")
                                        .append(username)
                                        .append(" but no forwarding address was set for this account.");
                                throw new MessagingException(
                                    errorBuffer.toString());
                            }
                            recipients = new HashSet();
                            recipients.add(forwardTo);
                            try {
                                getMailetContext().sendMail(
                                    mail.getSender(),
                                    recipients,
                                    message);
                                StringBuffer logBuffer =
                                    new StringBuffer(128)
                                        .append("Mail for ")
                                        .append(username)
                                        .append(" forwarded to ")
                                        .append(forwardTo.toString());
                                log(logBuffer.toString());
                                return;
                            } catch (MessagingException me) {
                                StringBuffer logBuffer =
                                    new StringBuffer(128)
                                        .append("Error forwarding mail to ")
                                        .append(forwardTo.toString())
                                        .append("attempting local delivery");
                                log(logBuffer.toString());
                                throw me;
                            }
                        }
                    }
                } else {
                    throw new MessagingException("user is not instance of JamesUser");
                }
                try {
                    //build a repository spec and use that for this host
                    getMailetContext()
                        .getMailRepository(inboxURI + recipient.getUser() + "/")
                        .store(mail);
                } catch (Exception ex) {
                    getMailetContext().log("Error while storing mail.", ex);
                    errors.add(recipient);
                }
            }
        }
        if (!errors.isEmpty()) {
            // If there were errors, we redirect the email to the ERROR processor.
            // In order for this server to meet the requirements of the SMTP specification,
            // mails on the ERROR processor must be returned to the sender.  Note that this
            // email doesn't include any details regarding the details of the failure(s).
            // In the future we may wish to address this.
            getMailetContext().sendMail(
                mail.getSender(),
                errors,
                message,
                Mail.ERROR);
        }
        //We always consume this message
        mail.setState(Mail.GHOST);
    }
}
