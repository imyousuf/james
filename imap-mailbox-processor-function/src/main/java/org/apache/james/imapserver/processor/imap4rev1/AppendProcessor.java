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

package org.apache.james.imapserver.processor.imap4rev1;

import java.util.Date;

import javax.mail.MessagingException;
import javax.mail.Flags.Flag;
import javax.mail.internet.MimeMessage;

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.imap.message.request.imap4rev1.AppendRequest;
import org.apache.james.imapserver.processor.base.AbstractMailboxAwareProcessor;
import org.apache.james.imapserver.processor.base.AuthorizationException;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.ImapMailbox;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;

public class AppendProcessor extends AbstractMailboxAwareProcessor {

    final StatusResponseFactory statusResponseFactory;
    
    public AppendProcessor(final ImapProcessor next,
            final MailboxManagerProvider mailboxManagerProvider, final StatusResponseFactory statusResponseFactory) {
        super(next, mailboxManagerProvider, statusResponseFactory);
        this.statusResponseFactory = statusResponseFactory;
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof AppendRequest);
    }

    protected void doProcess(ImapRequest message,
            ImapSession session, String tag, ImapCommand command, Responder responder)
            throws MailboxException, AuthorizationException, ProtocolException {
        final AppendRequest request = (AppendRequest) message;
        final String mailboxName = request.getMailboxName();
        final MimeMessage mimeMessage = request.getMessage();
        final Date datetime = request.getDatetime();
        doProcess(mailboxName, mimeMessage,
                datetime, session, tag, command, responder);
    }



    private void doProcess(String mailboxName,
            MimeMessage message, Date datetime, ImapSession session,
            String tag, ImapCommand command, Responder responder) throws MailboxException,
            AuthorizationException, ProtocolException {
        
        // TODO: Flags are ignore: check whether the specification says that
        // they should be processed
        try {
            
            mailboxName = buildFullName(session, mailboxName);
            final MailboxManager mailboxManager = getMailboxManager(session);
            final ImapMailbox mailbox = mailboxManager.getImapMailbox(mailboxName);
            appendToMailbox(message, datetime, session, tag, command, mailbox, responder);
            
        } catch (MailboxManagerException mme) {
            // Mailbox API does not provide facilities for diagnosing the problem
            // assume that 
            // TODO: improved API should communicate when this operation
            // TODO: fails whether the mailbox exists
            Logger logger = getLogger();
            if (logger.isInfoEnabled()) {
                logger.info(mme.getMessage());
            }
            if (logger.isDebugEnabled()) {
                logger.debug("Cannot open mailbox: ", mme);
            }
            no(command, tag, responder, HumanReadableTextKey.FAILURE_NO_SUCH_MAILBOX, 
                    StatusResponse.ResponseCode.TRYCREATE);
        }

    }

    private void appendToMailbox(MimeMessage message, Date datetime, 
            ImapSession session, String tag, ImapCommand command, ImapMailbox mailbox,
            Responder responder) throws MailboxException {
        try {
            message.setFlag(Flag.RECENT, true);
            mailbox.appendMessage(message, datetime, 0);
        } catch (MailboxManagerException e) {
            // TODO why not TRYCREATE?
            throw new MailboxException(e);
        } catch (MessagingException e) {
            throw new MailboxException(e);
        }
        
        unsolicitedResponses(session, responder, false);
        okComplete(command, tag, responder);
        
    }
}
