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
import org.apache.james.experimental.imapserver.message.CommandFailedResponseMessage;
import org.apache.james.experimental.imapserver.message.ImapResponseMessage;
import org.apache.james.experimental.imapserver.message.request.AbstractImapRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.AuthenticateRequest;
import org.apache.james.experimental.imapserver.processor.AbstractImapRequestProcessor;
import org.apache.james.experimental.imapserver.store.MailboxException;


public class AuthenticateProcessor extends AbstractImapRequestProcessor {
	
	protected ImapResponseMessage doProcess(AbstractImapRequest message, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
		final ImapResponseMessage result;
		if (message instanceof AuthenticateRequest) {
			final AuthenticateRequest request = (AuthenticateRequest) message;
			result = doProcess(request, session, tag, command);
		} else {
			final Logger logger = getLogger();
			if (logger != null)
			{
				logger.debug("Expected AuthenticateRequest, was " + message);
			}
			result = new BadResponseMessage("Command unknown by Authenticate processor.");
		}
		
		return result;
	}

	private ImapResponseMessage doProcess(AuthenticateRequest request, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
		final String authType = request.getAuthType();
		final ImapResponseMessage result = doProcess(authType, session, tag, command);
		return result;
	}
	
	private ImapResponseMessage doProcess(String authType, 
			ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        final CommandFailedResponseMessage result = new CommandFailedResponseMessage(command, 
                "Unsupported authentication mechanism '" + authType + "'", tag);
        return result;
	}
}
