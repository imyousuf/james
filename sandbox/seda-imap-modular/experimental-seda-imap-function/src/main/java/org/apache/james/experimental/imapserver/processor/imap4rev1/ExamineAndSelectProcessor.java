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

import javax.mail.Flags;

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.experimental.imapserver.AuthorizationException;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.ProtocolException;
import org.apache.james.experimental.imapserver.commands.ImapCommand;
import org.apache.james.experimental.imapserver.message.ImapResponseMessage;
import org.apache.james.experimental.imapserver.message.request.AbstractImapRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.ExamineRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.SelectRequest;
import org.apache.james.experimental.imapserver.message.response.imap4rev1.BadResponse;
import org.apache.james.experimental.imapserver.message.response.imap4rev1.ExamineAndSelectResponse;
import org.apache.james.experimental.imapserver.processor.AbstractImapRequestProcessor;
import org.apache.james.experimental.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;


public class ExamineAndSelectProcessor extends AbstractImapRequestProcessor {
	
	protected ImapResponseMessage doProcess(AbstractImapRequest message, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
		final ImapResponseMessage result;
		if (message instanceof SelectRequest) {
			final SelectRequest request = (SelectRequest) message;
			result = doProcess(request, session, tag, command);
			
		} else if (message instanceof ExamineRequest) {
			final ExamineRequest request = (ExamineRequest) message;
			result = doProcess(request, session, tag, command);
			
		} else {
			final Logger logger = getLogger();
			if (logger != null)
			{
				logger.debug("Expected SelectRequest or ExamineRequest, was " + message);
			}
			result = new BadResponse("Command unknown by Examine and Select processor.");
		}
		
		return result;
	}
	
	private ImapResponseMessage doProcess(ExamineRequest request, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
		final String mailboxName = request.getMailboxName();
		final ImapResponseMessage result = doProcess(mailboxName, false, session, tag, command);
		return result;
	}

	private ImapResponseMessage doProcess(SelectRequest request, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
		final String mailboxName = request.getMailboxName();
		final ImapResponseMessage result = doProcess(mailboxName, false, session, tag, command);
		return result;
	}
	
	private ImapResponseMessage doProcess(String mailboxName, boolean isExamine,
			ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        ImapResponseMessage result;
        session.deselect();
        try {
            String fullMailboxName=session.buildFullName(mailboxName);
            selectMailbox(fullMailboxName, session, isExamine);
            ImapMailboxSession mailbox = session.getSelected().getMailbox();
            final Flags permanentFlags = mailbox.getPermanentFlags();
            final boolean writeable = mailbox.isWriteable();
            final boolean resetRecent = !isExamine;
            final int recentCount = mailbox.getRecentCount(resetRecent);
            final long uidValidity = mailbox.getUidValidity();
            final MessageResult firstUnseen = mailbox.getFirstUnseen(MessageResult.MSN);
            final int messageCount = mailbox.getMessageCount();
            result = new ExamineAndSelectResponse(command, permanentFlags, 
                    writeable, recentCount, uidValidity, firstUnseen, messageCount,
                    tag);
        } catch (MailboxManagerException e) {
            throw new MailboxException(e);
        }
        return result;
	}
    
    private boolean selectMailbox(String mailboxName, ImapSession session, boolean readOnly) throws MailboxException, MailboxManagerException {
        ImapMailboxSession mailbox = session.getMailboxManager().getImapMailboxSession(mailboxName);

        if ( !mailbox.isSelectable() ) {
            throw new MailboxException( "Nonselectable mailbox." );
        }

        session.setSelected( mailbox, readOnly );
        return readOnly;
    }
}
