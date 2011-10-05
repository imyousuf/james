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

import java.util.Date;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.james.core.MimeMessageInputStream;
import org.apache.james.mailbox.BadCredentialsException;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.user.api.UsersRepository;
import org.apache.james.user.api.UsersRepositoryException;
import org.apache.james.transport.util.MailetContextLog;
import org.apache.jsieve.mailet.Poster;
import org.apache.jsieve.mailet.SieveMailboxMailet;
import org.apache.mailet.Mail;
import org.apache.mailet.MailAddress;
import org.apache.mailet.MailetConfig;

/**
 * Contains resource bindings.
 */
public class SieveMailet extends SieveMailboxMailet implements Poster {

    private UsersRepository usersRepos;
    private MailboxManager mailboxManager;

    @Resource(name = "usersrepository")
    public void setUsersRepository(UsersRepository usersRepos) {
        this.usersRepos = usersRepos;
    }

    @Resource(name = "mailboxmanager")
    public void setMailboxManager(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }

    @Override
    public void init(MailetConfig config) throws MessagingException {
        // ATM Fixed implementation
        try {
            setLocator(new ResourceLocatorImpl(usersRepos.supportVirtualHosting()));
        } catch (UsersRepositoryException e) {
            throw new MessagingException("Unable to access UsersRepository", e);
        }
        setPoster(this);
        super.init(config);
    }

    public SieveMailet() {
        super();

    }

    /**
     * Return the username to use for sieve processing for the given
     * MailAddress. If virtualhosting is supported use the full emailaddrees as
     * username
     * 
     * @param m
     * @return username
     */
    protected String getUsername(MailAddress m) {
        try {
            if (usersRepos.supportVirtualHosting()) {
                return m.toString();
            } else {
                return super.getUsername(m);
            }
        } catch (UsersRepositoryException e) {
            log("Unable to access UsersRepository", e);
            return super.getUsername(m);

        }
    }

    @Override
    public void storeMail(MailAddress sender, MailAddress recipient, Mail mail) throws MessagingException {
        super.storeMail(sender, recipient, mail);

        String s;
        if (sender != null) {
            s = sender.toString();
        } else {
            s = "<>";
        }
        // if no exception was thrown the message was successfully stored in the
        // mailbox
        log("Local delivered mail " + mail.getName() + " sucessfully from " + s + " to " + recipient.toString());
    }

    /**
     * @see org.apache.jsieve.mailet.Poster#post(java.lang.String,
     *      javax.mail.internet.MimeMessage)
     */
    public void post(String url, MimeMessage mail) throws MessagingException {
        final int endOfScheme = url.indexOf(':');
        if (endOfScheme < 0) {
            throw new MessagingException("Malformed URI");
        } else {
            final String scheme = url.substring(0, endOfScheme);
            if ("mailbox".equals(scheme)) {
                final int startOfUser = endOfScheme + 3;
                final int endOfUser = url.indexOf('@', startOfUser);
                if (endOfUser < 0) {
                    // TODO: when user missing, append to a default location
                    throw new MessagingException("Shared mailbox is not supported");
                } else {
                    String user = url.substring(startOfUser, endOfUser);
                    final int startOfHost = endOfUser + 1;
                    final int endOfHost = url.indexOf('/', startOfHost);
                    final String host = url.substring(startOfHost, endOfHost);
                    final String urlPath;
                    final int length = url.length();
                    if (endOfHost + 1 == length) {
                        urlPath = "INBOX";
                    } else {
                        urlPath = url.substring(endOfHost, length);
                    }

                    // check if we should use the full emailaddress as
                    // username
                    try {
                        if (usersRepos.supportVirtualHosting()) {
                            user = user + "@" + host;
                        }
                    } catch (UsersRepositoryException e) {
                        throw new MessagingException("Unable to accessUsersRepository", e);
                    }

                    MailboxSession session;
                    try {
                        session = mailboxManager.createSystemSession(user, new MailetContextLog(getMailetContext()));
                    } catch (BadCredentialsException e) {
                        throw new MessagingException("Unable to authenticate to mailbox", e);
                    } catch (MailboxException e) {
                        throw new MessagingException("Can not access mailbox", e);
                    }

                    // start processing request
                    mailboxManager.startProcessingRequest(session);

                    // This allows Sieve scripts to use a standard delimiter
                    // regardless of mailbox implementation
                    String destination = urlPath.replace('/', session.getPathDelimiter());

                    if (destination == null || "".equals(destination)) {
                        destination = "INBOX";
                    }
                    if (destination.startsWith(session.getPathDelimiter() + ""))
                        destination = destination.substring(1);
                    
                    // Use the MailboxSession to construct the MailboxPath.
                    // See JAMES-1326
                    final MailboxPath path = MailboxPath.inbox(session);
                    try {
                        if ("INBOX".equalsIgnoreCase(destination) && !(mailboxManager.mailboxExists(path, session))) {
                            mailboxManager.createMailbox(path, session);
                        }
                        final MessageManager mailbox = mailboxManager.getMailbox(path, session);
                        if (mailbox == null) {
                            final String error = "Mailbox for user " + user + " was not found on this server.";
                            throw new MessagingException(error);
                        }

                        mailbox.appendMessage(new MimeMessageInputStream(mail), new Date(), session, true, null);
                    } catch (MailboxException e) {
                        throw new MessagingException("Unable to access mailbox.", e);
                    } finally {
                        session.close();
                        try {
                            mailboxManager.logout(session, true);
                        } catch (MailboxException e) {
                            throw new MessagingException("Can logout from mailbox", e);
                        }

                        // stop processing request
                        mailboxManager.endProcessingRequest(session);

                    }
                }
            } else {
                // TODO: add support for more protocols
                // TODO: for example mailto: for forwarding over SMTP
                // TODO: for example xmpp: for forwarding over Jabber
                throw new MessagingException("Unsupported protocol");
            }
        }
    }
}
