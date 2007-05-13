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
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.ProtocolException;
import org.apache.james.experimental.imapserver.commands.ImapCommand;
import org.apache.james.experimental.imapserver.message.BadResponseMessage;
import org.apache.james.experimental.imapserver.message.CommandCompleteResponseMessage;
import org.apache.james.experimental.imapserver.message.ImapResponseMessage;
import org.apache.james.experimental.imapserver.message.request.AbstractImapRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.RenameRequest;
import org.apache.james.experimental.imapserver.processor.AbstractImapRequestProcessor;
import org.apache.james.experimental.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;


public class RenameProcessor extends AbstractImapRequestProcessor {
	
	protected ImapResponseMessage doProcess(AbstractImapRequest message, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
		final ImapResponseMessage result;
		if (message instanceof RenameRequest) {
			final RenameRequest request = (RenameRequest) message;
			result = doProcess(request, session, tag, command);
		} else {
			final Logger logger = getLogger();
			if (logger != null)
			{
				logger.debug("Expected RenameRequest, was " + message);
			}
			result = new BadResponseMessage("Command unknown by Rename processor.");
		}
		
		return result;
	}

	private ImapResponseMessage doProcess(RenameRequest request, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
		final String existingName = request.getExistingName();
		final String newName = request.getNewName();
		final ImapResponseMessage result = doProcess(existingName, newName, session, tag, command);
		return result;
	}
	
	private ImapResponseMessage doProcess(final String existingName, final String newName, 
			ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        try {
            final String fullExistingName=session.buildFullName(existingName);
            final String fullNewName=session.buildFullName(newName);
            session.getMailboxManager().renameMailbox( fullExistingName, fullNewName );
        } catch (MailboxManagerException e) {
           throw new MailboxException(e);
        }

        return new CommandCompleteResponseMessage(false, command, tag);
	}
}
