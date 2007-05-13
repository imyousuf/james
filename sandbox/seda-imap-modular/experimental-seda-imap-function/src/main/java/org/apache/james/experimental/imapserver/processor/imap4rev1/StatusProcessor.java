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

package org.apache.james.experimental.imapserver.processor.imap4rev1;

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.experimental.imapserver.AuthorizationException;
import org.apache.james.experimental.imapserver.ImapConstants;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.ProtocolException;
import org.apache.james.experimental.imapserver.commands.ImapCommand;
import org.apache.james.experimental.imapserver.message.BadResponseMessage;
import org.apache.james.experimental.imapserver.message.ImapResponseMessage;
import org.apache.james.experimental.imapserver.message.StatusDataItems;
import org.apache.james.experimental.imapserver.message.StatusResponseMessage;
import org.apache.james.experimental.imapserver.message.request.AbstractImapRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.StatusRequest;
import org.apache.james.experimental.imapserver.processor.AbstractImapRequestProcessor;
import org.apache.james.experimental.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;


public class StatusProcessor extends AbstractImapRequestProcessor {
	
	protected ImapResponseMessage doProcess(AbstractImapRequest message, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
		final ImapResponseMessage result;
		if (message instanceof StatusRequest) {
			final StatusRequest request = (StatusRequest) message;
			result = doProcess(request, session, tag, command);
		} else {
			final Logger logger = getLogger();
			if (logger != null)
			{
				logger.debug("Expected StatusRequest, was " + message);
			}
			result = new BadResponseMessage("Command unknown by Status processor.");
		}
		
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
            String fullMailboxName= session.buildFullName(mailboxName);
            
            if (logger != null && logger.isDebugEnabled()) { 
                logger.debug("Status called on mailbox named " + mailboxName + " (" + fullMailboxName + ")"); 
            }
            
            ImapMailboxSession mailbox = session.getMailboxManager().getImapMailboxSession(fullMailboxName);
            
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
        final StatusResponseMessage result = 
            new StatusResponseMessage(command, buffer.toString(), tag);
        return result;
	}
}
