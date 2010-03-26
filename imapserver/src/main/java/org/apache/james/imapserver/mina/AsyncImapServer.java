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

package org.apache.james.imapserver.mina;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.annotation.Resource;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.HierarchicalConfiguration;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.decode.ImapDecoder;
import org.apache.james.imap.encode.ImapEncoder;
import org.apache.james.imap.mailbox.Mailbox;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.main.ImapRequestHandler;
import org.apache.james.socket.mina.AbstractAsyncServer;
import org.apache.jsieve.mailet.Poster;
import org.apache.mina.core.filterchain.DefaultIoFilterChainBuilder;
import org.apache.mina.core.service.IoHandler;

/**
 * Async ImapServer which use MINA for socket handling
 *
 */
public class AsyncImapServer extends AbstractAsyncServer implements ImapConstants, Poster{

    private static final String softwaretype = "JAMES "+VERSION+" Server "; //+ Constants.SOFTWARE_VERSION;

    private String hello;
    private ImapProcessor processor;
    private ImapEncoder encoder;

    private ImapDecoder decoder;

    private MailboxManager mailboxManager;

    @Resource(name="imapDecoder")
    public void setImapDecoder(ImapDecoder decoder) {
        this.decoder = decoder;
    }
    
    @Resource(name="imapEncoder")
    public void setImapEncoder(ImapEncoder encoder) {
        this.encoder = encoder;
    }
    
    @Resource(name="imapProcessor")
    public void setImapProcessor(ImapProcessor processor) {
        this.processor = processor;
    }
   
    @Resource(name="mailboxmanager")
    public void setMailboxManager(MailboxManager mailboxManager) {
        this.mailboxManager = mailboxManager;
    }
    
    @Override
    public void doConfigure( final HierarchicalConfiguration configuration ) throws ConfigurationException {
        super.doConfigure(configuration);
        hello  = softwaretype + " Server " + getHelloName() + " is ready.";
    }
    
    
    /*
     * (non-Javadoc)
     * @see org.apache.james.socket.mina.AbstractAsyncServer#getDefaultPort()
     */
    public int getDefaultPort() {
        return 143;
    }

 
    /*
     * (non-Javadoc)
     * @see org.apache.james.socket.mina.AbstractAsyncServer#getServiceType()
     */
    public String getServiceType() {
        return "IMAP Service";
    }
    
    @Override
    protected IoHandler createIoHandler() {
        final ImapRequestHandler handler = new ImapRequestHandler(decoder, processor, encoder);
        return new ImapIoHandler(hello, handler, getLogger());
    }

    @Override
    protected DefaultIoFilterChainBuilder createIoFilterChainBuilder() {
        
        // just return an empty filterchain because we need no special protocol filter etc
        return new DefaultIoFilterChainBuilder();
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
                    if (getMailServer().isLocalServer(host) == false) {
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
                        if (getMailServer().supportVirtualHosting()) {
                            user = user + "@" + host;
                        } 
                        
                        final MailboxSession session = mailboxManager.createSystemSession(user, getLogger());
                        
                        // start processing request
                        mailboxManager.startProcessingRequest(session);

                        // This allows Sieve scripts to use a standard delimiter regardless of mailbox implementation
                        String destination = urlPath.replace('/', session.getPersonalSpace().getDeliminator());
                        
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

                            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            mail.writeTo(baos);
                            mailbox.appendMessage(baos.toByteArray() , new Date(), session, true, null);
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
}
