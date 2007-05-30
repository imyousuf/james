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

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.experimental.imapserver.AuthorizationException;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.message.request.ImapRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.LsubRequest;
import org.apache.james.experimental.imapserver.message.response.ImapResponseMessage;
import org.apache.james.experimental.imapserver.processor.ImapProcessor;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.ListResult;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;


public class LsubProcessor extends AbstractListingProcessor {
	
	public LsubProcessor(final ImapProcessor next, 
            final MailboxManagerProvider mailboxManagerProvider) {
        super(next, mailboxManagerProvider);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof LsubRequest);
    }

    
    protected ImapResponseMessage doProcess(ImapRequest message, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        final LsubRequest request = (LsubRequest) message;
        final ImapResponseMessage result = doProcess(request, session, tag, command);
		return result;
	}

	private ImapResponseMessage doProcess(LsubRequest request, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
		final String baseReferenceName = request.getBaseReferenceName();
		final String mailboxPatternString = request.getMailboxPattern();
		final ImapResponseMessage result = doProcess(baseReferenceName, mailboxPatternString, session, tag, command);
		return result;
	}
    
    protected ListResult[] doList( ImapSession session, String base, String pattern ) throws MailboxException {
        return doList(  session,  base,  pattern, true);
    }
}
