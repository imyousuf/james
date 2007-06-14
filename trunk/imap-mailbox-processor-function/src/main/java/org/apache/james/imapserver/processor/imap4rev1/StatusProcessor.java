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

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.message.StatusDataItems;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.ImapResponseMessage;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.imap.message.request.imap4rev1.StatusRequest;
import org.apache.james.imap.message.response.imap4rev1.legacy.StatusResponse;
import org.apache.james.imapserver.processor.base.AbstractMailboxAwareProcessor;
import org.apache.james.imapserver.processor.base.AuthorizationException;
import org.apache.james.imapserver.processor.base.ImapSessionUtils;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;


public class StatusProcessor extends AbstractMailboxAwareProcessor {
	
	public StatusProcessor(final ImapProcessor next, 
            final MailboxManagerProvider mailboxManagerProvider) {
        super(next, mailboxManagerProvider);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof StatusRequest);
    }
    
    protected ImapResponseMessage doProcess(ImapRequest message, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        final StatusRequest request = (StatusRequest) message;
        final ImapResponseMessage result = doProcess(request, session, tag, command);
		return result;
	}

	private ImapResponseMessage doProcess(StatusRequest request, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
	    final String mailboxName = request.getMailboxName();
	    final StatusDataItems statusDataItems = request.getStatusDataItems();
		final ImapResponseMessage result = doProcess(mailboxName, statusDataItems, session, tag, command);
		return result;
	}
	
	private ImapResponseMessage doProcess(final String mailboxName, final StatusDataItems statusDataItems,
			ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        final Logger logger = getLogger(); 

        // TODO: response should not be prepared in process
        // TODO: return a transfer object
        StringBuffer buffer = new StringBuffer( mailboxName );
        buffer.append( ImapConstants.SP );
        buffer.append( "(" );
        try {
            String fullMailboxName= buildFullName(session, mailboxName);
            
            if (logger != null && logger.isDebugEnabled()) { 
                logger.debug("Status called on mailbox named " + mailboxName + " (" + fullMailboxName + ")"); 
            }
            
            final MailboxManager mailboxManager = getMailboxManager(session);
            final ImapMailboxSession mailbox = mailboxManager.getImapMailboxSession(fullMailboxName);
            
            if (statusDataItems.isMessages()) {
                buffer.append(ImapConstants.STATUS_MESSAGES);
                buffer.append(ImapConstants.SP);

                buffer.append(mailbox.getMessageCount());

                buffer.append(ImapConstants.SP);
            }

            if (statusDataItems.isRecent()) {
                buffer.append(ImapConstants.STATUS_RECENT);
                buffer.append(ImapConstants.SP);
                buffer.append(mailbox.getRecentCount(false));
                buffer.append(ImapConstants.SP);
            }

            if (statusDataItems.isUidNext()) {
                buffer.append(ImapConstants.STATUS_UIDNEXT);
                buffer.append(ImapConstants.SP);
                buffer.append(mailbox.getUidNext());
                buffer.append(ImapConstants.SP);
            }

            if (statusDataItems.isUidValidity()) {
                buffer.append(ImapConstants.STATUS_UIDVALIDITY);
                buffer.append(ImapConstants.SP);
                buffer.append(mailbox.getUidValidity());
                buffer.append(ImapConstants.SP);
            }

            if (statusDataItems.isUnseen()) {
                buffer.append(ImapConstants.STATUS_UNSEEN);
                buffer.append(ImapConstants.SP);
                buffer.append(mailbox.getUnseenCount());
                buffer.append(ImapConstants.SP);
            }
        } catch (MailboxManagerException e) {
            if (logger != null && logger.isDebugEnabled()) { 
                logger.debug("STATUS command failed: ", e); 
            }
            throw new MailboxException(e);
        }
        if ( buffer.charAt( buffer.length() - 1 ) == ' ' ) {
            buffer.setLength( buffer.length() - 1 );
        }
        buffer.append(')');
        final StatusResponse result = 
            new StatusResponse(command, buffer.toString(), tag);
        ImapSessionUtils.addUnsolicitedResponses( result, session, false );
        return result;
	}
}
