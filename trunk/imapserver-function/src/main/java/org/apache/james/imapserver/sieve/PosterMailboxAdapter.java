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

package org.apache.james.imapserver.sieve;

import java.util.Date;

import javax.mail.MessagingException;

import org.apache.james.mailboxmanager.MailboxSession;
import org.apache.james.mailboxmanager.mailbox.Mailbox;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;
import org.apache.mailet.Mail;

/**
 * This is just an experimental example. 
 * This is probably not working ATM but it's often easier 
 * to illustrate using code.
 * 
 * Temporarily removed "Poster" to avoid function-to-function dependency.
 */
public class PosterMailboxAdapter { // implements Poster {

    private final MailboxManagerProvider mailboxManagerProvider;
    
    /**
     * TODO: switch to SDI to make extra avalon foo easier
     * @param mailboxManagerProvider not null
     */
    public PosterMailboxAdapter(MailboxManagerProvider mailboxManagerProvider) {
        this.mailboxManagerProvider = mailboxManagerProvider;
    }
    
    public void post(String url, Mail mail)throws MessagingException {
        final int endOfScheme = url.indexOf(':');
        if (endOfScheme < 0) {
            throw new MessagingException("Malformed URI");
        } else {
            final String scheme = url.substring(0, endOfScheme);
            if ("mailbox".equals(scheme)) {
                final int startOfUser = endOfScheme + 2;
                final int endOfUser = url.indexOf('@', startOfUser);
                if (endOfUser < 0) {
                    // TODO: when user missing, append to a default location
                    throw new MessagingException("Shared mailbox is not supported");
                } else {
                    final String user = url.substring(startOfUser, endOfUser);
                    final int startOfHost = endOfUser + 1;
                    final int endOfHost  = url.indexOf('/', startOfHost);
                    final String host = url.substring(startOfHost, endOfHost);
                    if (!"localhost".equals(host)) {
                        //TODO: possible support for clustering?
                        throw new MessagingException("Only local mailboxes are supported");
                    } else {
                        final String urlPath;
                        final int length = url.length();
                        if (endOfHost == length) {
                            urlPath = "INBOX";
                        } else {
                            urlPath = url.substring(endOfHost, length);
                        }
                        // This allows Sieve scripts to use a standard delimiter regardless of mailbox implementation
                        final String mailbox = urlPath.replace('/', MailboxManager.HIERARCHY_DELIMITER);
                        postToMailbox(user, mail, mailbox);
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
    
    
    public void postToMailbox(String username, Mail mail, String destination) throws MessagingException {
        final String name = mailboxManagerProvider.getMailboxManager().resolve(username, "INBOX");
        final MailboxManager mailboxManager = mailboxManagerProvider.getMailboxManager();
        final MailboxSession session = mailboxManager.createSession();
        try
        {
            final Mailbox mailbox = mailboxManager.getMailbox(name, true);
            
            if (mailbox == null) {
                final String error = "Mailbox for user " + username
                        + " was not found on this server.";
                throw new MessagingException(error);
            }
            mailbox.appendMessage(mail.getMessage(), new Date(), null, session);
        }
        finally 
        {
            session.close();   
        }
    }
}
