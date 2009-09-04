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

package org.apache.james.imapserver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.framework.configuration.Configuration;
import org.apache.avalon.framework.configuration.ConfigurationException;
import org.apache.avalon.framework.service.ServiceException;
import org.apache.avalon.framework.service.ServiceManager;
import org.apache.commons.logging.impl.AvalonLogger;
import org.apache.james.Constants;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.imap.api.ImapConstants;
import org.apache.james.imap.mailbox.Mailbox;
import org.apache.james.imap.mailbox.MailboxManager;
import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.main.ImapRequestHandler;
import org.apache.james.services.FileSystem;
import org.apache.james.socket.AbstractJamesService;
import org.apache.james.socket.ProtocolHandler;
import org.apache.jsieve.mailet.Poster;

/**
 * TODO: this is a quick cut-and-paste hack from POP3Server. Should probably be
 * rewritten from scratch, together with ImapHandler.
 *
 * <p>Accepts IMAP connections on a server socket and dispatches them to IMAPHandlers.</p>
 *
 * <p>Also responsible for loading and parsing IMAP specific configuration.</p>
 */
public class ImapServer extends AbstractJamesService implements ImapConstants, Poster
{
    private static final String softwaretype = "JAMES "+VERSION+" Server " + Constants.SOFTWARE_VERSION;
     
    private ImapFactory factory;
    
    private String hello = softwaretype;
    
    
    public void service(ServiceManager comp) throws ServiceException {
        super.service(comp);
        factory = new DefaultImapFactory((FileSystem) comp.lookup(FileSystem.ROLE), 
                (UsersRepository) comp.lookup(UsersRepository.ROLE), getLogger());
    }


    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    protected void doInit() throws Exception {
        getLogger().debug("Initialising...");
        factory.initialize();
    }



    /**
     * @see org.apache.avalon.framework.configuration.Configurable#configure(Configuration)
     */
    public void configure( final Configuration configuration ) throws ConfigurationException {
        super.configure( configuration );
        factory.configure(configuration);
        hello  = softwaretype + " Server " + helloName + " is ready.";
    }
    
    /**
     * @see AbstractJamesService#getDefaultPort()
     */
    protected int getDefaultPort() {
        return 143;
    }

    /**
     * @see AbstractJamesService#getServiceType()
     */
    public String getServiceType() {
        return "IMAP Service";
    }

    /**
     * Producing handlers.
     * @see org.apache.avalon.excalibur.pool.ObjectFactory#newInstance()
     */
    public ProtocolHandler newProtocolHandlerInstance()
    {  
        final ImapRequestHandler handler = factory.createHandler();
        final ImapHandler imapHandler = new ImapHandler(handler, hello); 
        getLogger().debug("Create handler instance");
        return imapHandler;
    }

    // TODO: 
    protected Object getConfigurationData() {
        return null;
    }
    
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
                        if (endOfHost + 1 == length) {
                            urlPath = "INBOX";
                        } else {
                            urlPath = url.substring(endOfHost, length);
                        }
                        final MailboxManager mailboxManager = factory.getMailbox();
                        final MailboxSession session = mailboxManager.createSystemSession(user, new AvalonLogger(getLogger()));
                        // This allows Sieve scripts to use a standard delimiter regardless of mailbox implementation
                        final String mailbox = urlPath.replace('/', session.getPersonalSpace().getDeliminator());
                        postToMailbox(user, mail, mailbox, session, mailboxManager);
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
    
    public void postToMailbox(String username, MimeMessage mail, String destination, final MailboxSession session, final MailboxManager mailboxManager) throws MessagingException {
        if (destination == null || "".equals(destination)) {
            destination = "INBOX";
        }
        final String name = mailboxManager.resolve(username, destination);
        try
        {
            if ("INBOX".equalsIgnoreCase(destination) && !(mailboxManager.mailboxExists(name, session))) {
                mailboxManager.createMailbox(name, session);
            }
            final Mailbox mailbox = mailboxManager.getMailbox(name, session);
            
            if (mailbox == null) {
                final String error = "Mailbox for user " + username
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
        }
    }
}
