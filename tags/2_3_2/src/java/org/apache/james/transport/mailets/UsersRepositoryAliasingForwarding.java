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

import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.james.Constants;
import org.apache.james.services.JamesUser;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;
import org.apache.james.services.UsersStore;
import org.apache.mailet.RFC2822Headers;

import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;

/**
 * Receives a Mail from JamesSpoolManager and takes care of delivery of the
 * message to local inboxes.
 * 
 * Available configurations are:
 * 
 * <enableAliases>true</enableAliases>: specify wether the user aliases should
 * be looked up or not. Default is false.
 * 
 * <enableForwarding>true</enableForwarding>: enable the forwarding. Default to
 * false.
 * 
 * <usersRepository>LocalAdmins</usersRepository>: specific users repository
 * name. Default to empty. If empty does lookup the default userRepository.
 */
public class UsersRepositoryAliasingForwarding extends GenericMailet {

    /**
     * The user repository for this mail server. Contains all the users with
     * inboxes on this server.
     */
    private UsersRepository usersRepository;

    /**
     * Whether to enable aliasing for users on this server
     */
    private boolean enableAliases;

    /**
     * Whether to enable forwarding for users on this server
     */
    private boolean enableForwarding;

    /**
     * Whether to ignore case when looking up user names on this server
     */
    private boolean ignoreCase;

    /**
     * Delivers a mail to a local mailbox.
     * 
     * @param mail
     *            the mail being processed
     * 
     * @throws MessagingException
     *             if an error occurs while storing the mail
     */
    public void service(Mail mail) throws MessagingException {
        Collection recipients = mail.getRecipients();
        Collection errors = new Vector();

        MimeMessage message = mail.getMessage();

        // Set Return-Path and remove all other Return-Path headers from the
        // message
        // This only works because there is a placeholder inserted by
        // MimeMessageWrapper
        message
                .setHeader(RFC2822Headers.RETURN_PATH,
                        (mail.getSender() == null ? "<>" : "<"
                                + mail.getSender() + ">"));

        Collection newRecipients = new LinkedList();
        for (Iterator i = recipients.iterator(); i.hasNext();) {
            MailAddress recipient = (MailAddress) i.next();
            try {
                String username = processMail(mail.getSender(), recipient,
                        message);

                // if the username is null or changed we remove it from the
                // remaining recipients
                if (username == null) {
                    i.remove();
                } else if (!username.equals(recipient.getUser())) {
                    i.remove();
                    // if the username has been changed we add a new recipient
                    // with the new name.
                    newRecipients.add(new MailAddress(username, recipient
                            .getHost()));
                }

            } catch (Exception ex) {
                getMailetContext().log("Error while storing mail.", ex);
                errors.add(recipient);
            }
        }

        if (newRecipients.size() > 0) {
            recipients.addAll(newRecipients);
        }

        if (!errors.isEmpty()) {
            // If there were errors, we redirect the email to the ERROR
            // processor.
            // In order for this server to meet the requirements of the SMTP
            // specification, mails on the ERROR processor must be returned to
            // the sender. Note that this email doesn't include any details
            // regarding the details of the failure(s).
            // In the future we may wish to address this.
            getMailetContext().sendMail(mail.getSender(), errors, message,
                    Mail.ERROR);
        }

        if (recipients.size() == 0) {
            // We always consume this message
            mail.setState(Mail.GHOST);
        }
    }

    /**
     * Return a string describing this mailet.
     * 
     * @return a string describing this mailet
     */
    public String getMailetInfo() {
        return "Local User Aliasing and Forwarding Mailet";
    }

    /**
     * Return null when the mail should be GHOSTed, the username string when it
     * should be changed due to the ignoreUser configuration.
     * 
     * @param sender
     * @param recipient
     * @param message
     * @throws MessagingException
     */
    public String processMail(MailAddress sender, MailAddress recipient,
            MimeMessage message) throws MessagingException {
        String username;
        if (recipient == null) {
            throw new IllegalArgumentException(
                    "Recipient for mail to be spooled cannot be null.");
        }
        if (message == null) {
            throw new IllegalArgumentException(
                    "Mail message to be spooled cannot be null.");
        }
        if (ignoreCase) {
            String originalUsername = recipient.getUser();
            username = usersRepository.getRealName(originalUsername);
            if (username == null) {
                StringBuffer errorBuffer = new StringBuffer(128).append(
                        "The inbox for user ").append(originalUsername).append(
                        " was not found on this server.");
                throw new MessagingException(errorBuffer.toString());
            }
        } else {
            username = recipient.getUser();
        }
        User user;
        if (enableAliases || enableForwarding) {
            user = usersRepository.getUserByName(username);
            if (user instanceof JamesUser) {
                if (enableAliases && ((JamesUser) user).getAliasing()) {
                    username = ((JamesUser) user).getAlias();
                }
                // Forwarding takes precedence over local aliases
                if (enableForwarding && ((JamesUser) user).getForwarding()) {
                    MailAddress forwardTo = ((JamesUser) user).getForwardingDestination();
                    if (forwardTo == null) {
                        StringBuffer errorBuffer = new StringBuffer(128)
                                .append("Forwarding was enabled for ")
                                .append(username)
                                .append(
                                        " but no forwarding address was set for this account.");
                        throw new MessagingException(errorBuffer.toString());
                    }
                    Collection recipients = new HashSet();
                    recipients.add(forwardTo);
                    try {
                        getMailetContext().sendMail(sender, recipients, message);
                        StringBuffer logBuffer = new StringBuffer(128).append(
                                "Mail for ").append(username).append(
                                " forwarded to ").append(forwardTo.toString());
                        getMailetContext().log(logBuffer.toString());
                        return null;
                    } catch (MessagingException me) {
                        StringBuffer logBuffer = new StringBuffer(128).append(
                                "Error forwarding mail to ").append(
                                forwardTo.toString()).append(
                                "attempting local delivery");
                        getMailetContext().log(logBuffer.toString());
                        throw me;
                    }
                }
            }
        }
        return username;
    }

    /**
     * @see org.apache.mailet.GenericMailet#init()
     */
    public void init() throws MessagingException {
        super.init();
        ServiceManager compMgr = (ServiceManager) getMailetContext()
                .getAttribute(Constants.AVALON_COMPONENT_MANAGER);

        UsersStore usersStore;
        try {
            usersStore = (UsersStore) compMgr.lookup(UsersStore.ROLE);


            enableAliases = new Boolean(getInitParameter("enableAliases",
                    getMailetContext().getAttribute(Constants.DEFAULT_ENABLE_ALIASES).toString()
                    )).booleanValue();
            enableForwarding = new Boolean(getInitParameter("enableForwarding",
                    getMailetContext().getAttribute(Constants.DEFAULT_ENABLE_FORWARDING).toString()
                    )).booleanValue();
            ignoreCase = new Boolean(getInitParameter("ignoreCase",
                    getMailetContext().getAttribute(Constants.DEFAULT_IGNORE_USERNAME_CASE).toString()
                    )).booleanValue();
            
            String userRep = getInitParameter("usersRepository");
            if (userRep == null || userRep.length() == 0) {
                try {
                    usersRepository = (UsersRepository) compMgr
                            .lookup(UsersRepository.ROLE);
                } catch (ServiceException e) {
                    log("Failed to retrieve UsersRepository component:"
                            + e.getMessage());
                }
            } else {
                usersRepository = usersStore.getRepository(userRep);
            }

        } catch (ServiceException cnfe) {
            log("Failed to retrieve UsersStore component:" + cnfe.getMessage());
        }

    }

}
