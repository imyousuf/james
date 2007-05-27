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

import org.apache.james.api.imap.ImapMessage;
import org.apache.james.experimental.imapserver.AuthorizationException;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.ProtocolException;
import org.apache.james.experimental.imapserver.commands.ImapCommand;
import org.apache.james.experimental.imapserver.message.ImapResponseMessage;
import org.apache.james.experimental.imapserver.message.request.ImapRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.CloseRequest;
import org.apache.james.experimental.imapserver.message.response.imap4rev1.CloseResponse;
import org.apache.james.experimental.imapserver.processor.ImapProcessor;
import org.apache.james.experimental.imapserver.processor.base.AbstractImapRequestProcessor;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;


public class CloseProcessor extends AbstractImapRequestProcessor {
	
	public CloseProcessor(final ImapProcessor next) {
        super(next);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof CloseRequest);
    }

    protected ImapResponseMessage doProcess(ImapRequest message, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        final CloseRequest request = (CloseRequest) message;
        final ImapResponseMessage result = doProcess(request, session, tag, command);
		return result;
	}

	private ImapResponseMessage doProcess(CloseRequest request, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
		final ImapResponseMessage result = doProcess(session, tag, command);
		return result;
	}
	
	private ImapResponseMessage doProcess(ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        ImapMailboxSession mailbox = session.getSelected().getMailbox();
        if ( session.getSelected().getMailbox().isWriteable() ) {
            try {
                mailbox.expunge(GeneralMessageSetImpl.all(),MessageResult.NOTHING);
            } catch (MailboxManagerException e) {
               throw new MailboxException(e);
            }
        }
        session.deselect();
        final CloseResponse result = new CloseResponse(command, tag);
        return result;
	}
}
