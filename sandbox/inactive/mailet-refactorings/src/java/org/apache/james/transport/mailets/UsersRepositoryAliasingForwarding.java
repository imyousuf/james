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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Vector;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import org.apache.james.services.VirtualUserTable;
import org.apache.james.vut.ErrorMappingException;
import org.apache.mailet.GenericMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.RFC2822Headers;
import org.apache.mailet.UsersRepository;

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
                Collection usernames = processMail(mail.getSender(), recipient,
                        message);

                // if the username is null or changed we remove it from the
                // remaining recipients
                if (usernames == null) {
                    i.remove();
                } else {
                    i.remove();
                    // if the username has been changed we add a new recipient
                    // with the new name.
                    newRecipients.addAll(usernames);
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
     * @param sender
     * @param recipient
     * @param message
     * @return
     * @throws MessagingException
     */
    public Collection processMail(MailAddress sender, MailAddress recipient,
            MimeMessage message) throws MessagingException {
        if (recipient == null) {
            throw new IllegalArgumentException(
                    "Recipient for mail to be spooled cannot be null.");
        }
        if (message == null) {
            throw new IllegalArgumentException(
                    "Mail message to be spooled cannot be null.");
        }

        if (usersRepository instanceof VirtualUserTable) {
            Collection mappings;
            try {
                mappings = ((VirtualUserTable) usersRepository).getMappings(recipient.getLocalPart(), recipient.getHost());
            } catch (ErrorMappingException e) {
                StringBuffer errorBuffer = new StringBuffer(128)
                    .append("A problem as occoured trying to alias and forward user ")
                    .append(recipient)
                    .append(": ")
                    .append(e.getMessage());
                    throw new MessagingException(errorBuffer.toString());
            }
            
            // TODO: what to do when mappings return null?
            if (mappings != null) {
                Iterator i = mappings.iterator();
                Collection remoteRecipients = new ArrayList();
                Collection localRecipients = new ArrayList();
                while (i.hasNext()) {
                    MailAddress nextMap = new MailAddress((String) i.next());
                    if (getMailetContext().isLocalServer(nextMap.getHost())) {
                        System.err.println("L: "+nextMap);
                        localRecipients.add(nextMap);
                    } else {
                        System.err.println("R: "+nextMap);
                        remoteRecipients.add(nextMap);
                    }
                }
                
                if (remoteRecipients.size() > 0) {
                    try {
                        getMailetContext().sendMail(sender, remoteRecipients, message);
                        StringBuffer logBuffer = new StringBuffer(128).append(
                                "Mail for ").append(recipient).append(
                                " forwarded to ");
                        for (Iterator j = remoteRecipients.iterator(); j.hasNext(); ) {
                            logBuffer.append(j.next());
                            if (j.hasNext()) logBuffer.append(", ");
                        }
                        getMailetContext().log(logBuffer.toString());
                        return null;
                    } catch (MessagingException me) {
                        StringBuffer logBuffer = new StringBuffer(128).append(
                                "Error forwarding mail to ");
                        for (Iterator j = remoteRecipients.iterator(); j.hasNext(); ) {
                            logBuffer.append(j.next());
                            if (j.hasNext()) logBuffer.append(", ");
                        }
                        logBuffer.append("attempting local delivery");
                        
                        getMailetContext().log(logBuffer.toString());
                        throw me;
                    }
                }
                
                if (localRecipients.size() > 0) {
                    return localRecipients;
                } else {
                    return null;
                }
            }
        } else {
            StringBuffer errorBuffer = new StringBuffer(128)
                .append("Warning: the repository ")
                .append(usersRepository.getClass().getName())
                .append(" does not implement VirtualUserTable interface).");
            getMailetContext().log(errorBuffer.toString());
        }
        String realName = usersRepository.getRealName(recipient.getLocalPart());
        if (realName != null) {
            ArrayList ret = new ArrayList();
            ret.add(new MailAddress(realName, recipient.getHost()));
            return ret;
        } 
            ArrayList ret = new ArrayList();
            ret.add(recipient);
            return ret;
        
    }

    /**
     * @see org.apache.mailet.GenericMailet#init()
     */
    public void init() throws MessagingException {
        super.init();
        
        
            String userRep = getInitParameter("usersRepository");
            
                usersRepository = getMailetContext().getUsersRepository(userRep);
            

    }

}
