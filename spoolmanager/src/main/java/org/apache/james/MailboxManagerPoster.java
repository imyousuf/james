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

package org.apache.james;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Enumeration;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.logging.Log;
import org.apache.james.imap.mailbox.Mailbox;
import org.apache.james.imap.mailbox.MailboxConstants;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.services.MailServer;
import org.apache.jsieve.mailet.Poster;

/**
 * Post to Mail to MailboxManager implementations
 * 
 *
 */
public class MailboxManagerPoster implements Poster, LogEnabled{

    private MailboxManager mailboxManager;
    private MailServer mailserver;
    private Log logger;


    @Resource(name="James")
    public void setMailServer(MailServer mailserver) {
        this.mailserver = mailserver;
    }
    
    @Resource(name="mailboxmanager")
    public void setMailboxManager(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }

    /**
     * @see org.apache.jsieve.mailet.Poster#post(java.lang.String, javax.mail.internet.MimeMessage)
     */
    public void post(String url, MimeMessage mail)throws MessagingException {
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
                    final int endOfHost  = url.indexOf('/', startOfHost);
                    final String host = url.substring(startOfHost, endOfHost);
                    //if (!"localhost".equals(host)) {
                    if (mailserver.isLocalServer(host) == false) {
                        //TODO: possible support for clustering?
                        throw new MessagingException("Only local mailboxes are supported");
                    } else {
                        final String urlPath;
                        final int length = url.length();
                        if (endOfHost + 1 == length) {
                            urlPath = "INBOX";
                        } else {
                            urlPath = url.substring(endOfHost, length);
                        }
                        
                        // check if we should use the full emailaddress as username
                        if (mailserver.supportVirtualHosting()) {
                            user = user + "@" + host;
                        } 
                        
                        final MailboxSession session = mailboxManager.createSystemSession(user, logger);
                        
                        // start processing request
                        mailboxManager.startProcessingRequest(session);

                        // This allows Sieve scripts to use a standard delimiter regardless of mailbox implementation
                        String destination = urlPath.replace('/', MailboxConstants.DEFAULT_DELIMITER);
                        
                        if (destination == null || "".equals(destination)) {
                            destination = "INBOX";
                        }
                        final String name = mailboxManager.resolve(user, destination);
                        try
                        {
                            if ("INBOX".equalsIgnoreCase(destination) && !(mailboxManager.mailboxExists(name, session))) {
                                mailboxManager.createMailbox(name, session);
                            }
                            final Mailbox mailbox = mailboxManager.getMailbox(name, session);
                            
                            if (mailbox == null) {
                                final String error = "Mailbox for user " + user
                                        + " was not found on this server.";
                                throw new MessagingException(error);
                            }


                            mailbox.appendMessage(new MimeMessageInputStream(mail) , new Date(), session, true, null);
                        }
                        catch (IOException e)
                        {
                            throw new MessagingException("Failed to write mail message", e);
                        }
                        finally 
                        {
                            session.close();   
                            mailboxManager.logout(session, true);
                            
                            // stop processing request
                            mailboxManager.endProcessingRequest(session);
                        }
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

    /*
     * (non-Javadoc)
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.logger = log;
    }

    /**
     * {@link InputStream} which contains the headers and the Body of the
     * wrapped {@link MimeMessage}
     * 
     */
    private final class MimeMessageInputStream extends InputStream {
        private final InputStream headersInputStream;
        private final InputStream bodyInputStream;
        private int cStream = 0;

        boolean nextCR = false;
        boolean nextLF = false;

        @SuppressWarnings("unchecked")
        public MimeMessageInputStream(MimeMessage message) throws IOException {
            try {
                ByteArrayOutputStream headersOut = new ByteArrayOutputStream();
                Enumeration headers = message.getAllHeaderLines();
                while (headers.hasMoreElements()) {
                    headersOut.write(headers.nextElement().toString().getBytes("US-ASCII"));
                    headersOut.write("\r\n".getBytes());
                }
                headersInputStream = new ByteArrayInputStream(headersOut.toByteArray());
                
                // use the raw InputStream because we want to have no conversion here and just obtain the original message body
                this.bodyInputStream = message.getRawInputStream();
            } catch (MessagingException e) {
                throw new IOException("Unable to read MimeMessage: " + e.getMessage());
            }
        }

        @Override
        public int read() throws IOException {
            if (nextCR) {
                nextCR = false;
                nextLF = true;
                return '\r';
            } else if (nextLF) {
                nextLF = false;
                return '\n';
            } else {
                int i = -1;
                if (cStream == 0) {
                    i = headersInputStream.read();
                } else {
                    i = bodyInputStream.read();
                }

                if (i == -1 && cStream == 0) {
                    cStream++;
                    nextCR = true;
                    return read();
                }
                return i;
            }

        }

        /** Closes all streams */
        public void close() throws IOException {
            headersInputStream.close();
            bodyInputStream.close();
        }

        /** Is there more data to read */
        public int available() throws IOException {
            if (cStream == 0) {
                return headersInputStream.available() + bodyInputStream.available() + 2;
            } else {
                return bodyInputStream.available();
            }
        }

    }
}
