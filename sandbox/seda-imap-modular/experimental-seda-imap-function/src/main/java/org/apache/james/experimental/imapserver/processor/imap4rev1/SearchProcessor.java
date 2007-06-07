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

import java.util.List;

import javax.mail.search.SearchTerm;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ImapProcessor;
import org.apache.james.api.imap.ImapSession;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.ImapResponseMessage;
import org.apache.james.experimental.imapserver.AuthorizationException;
import org.apache.james.experimental.imapserver.processor.base.AbstractImapRequestProcessor;
import org.apache.james.experimental.imapserver.processor.base.ImapSessionUtils;
import org.apache.james.imap.message.request.imap4rev1.SearchRequest;
import org.apache.james.imap.message.response.imap4rev1.legacy.SearchResponse;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;


public class SearchProcessor extends AbstractImapRequestProcessor {
	
	public SearchProcessor(final ImapProcessor next) {
        super(next);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof SearchRequest);
    }
    
    protected ImapResponseMessage doProcess(ImapRequest message, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        final SearchRequest request = (SearchRequest) message;
        final ImapResponseMessage result = doProcess(request, session, tag, command);
		return result;
	}

	private ImapResponseMessage doProcess(SearchRequest request, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
		final SearchTerm searchTerm = request.getSearchTerm();
		final boolean useUids = request.isUseUids();
		final ImapResponseMessage result = doProcess(searchTerm, useUids, session, tag, command);
		return result;
	}
	
	private ImapResponseMessage doProcess(final SearchTerm searchTerm, final boolean useUids, 
			ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        ImapMailboxSession mailbox = ImapSessionUtils.getMailbox(session);
        final int resultCode;
        if (useUids) {
            resultCode= MessageResult.UID;
        } else {
            resultCode= MessageResult.MSN;
        }
        MessageResult[] messageResults;
        try {
            messageResults = mailbox.search(GeneralMessageSetImpl.all(),searchTerm, resultCode);
        } catch (MailboxManagerException e) {
          throw new MailboxException(e);
        }
        // TODO: probably more efficient to stream data
        // TODO: directly to response
        StringBuffer idList = new StringBuffer();
        for (int i = 0; i < messageResults.length; i++) {
            if ( i > 0 ) {
                idList.append( ImapConstants.SP );
            }
            if ( useUids ) {
                idList.append( messageResults[i].getUid());
            } else {
                idList.append( messageResults[i].getMsn());
            }
        }
        final SearchResponse result = 
            new SearchResponse(command, idList.toString(), tag);
        boolean omitExpunged = (!useUids);
        List unsolicitedResponses = session.unsolicitedResponses( omitExpunged, useUids );
        result.addUnsolicitedResponses(unsolicitedResponses);
        return result;
	}
}
