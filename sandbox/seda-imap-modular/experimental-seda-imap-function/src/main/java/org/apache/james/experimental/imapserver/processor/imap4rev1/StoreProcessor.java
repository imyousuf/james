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

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.api.imap.message.StoreDirective;
import org.apache.james.experimental.imapserver.AuthorizationException;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.message.request.ImapRequest;
import org.apache.james.experimental.imapserver.message.request.imap4rev1.StoreRequest;
import org.apache.james.experimental.imapserver.message.response.ImapResponseMessage;
import org.apache.james.experimental.imapserver.message.response.imap4rev1.legacy.StoreResponse;
import org.apache.james.experimental.imapserver.processor.ImapProcessor;
import org.apache.james.experimental.imapserver.processor.base.AbstractImapRequestProcessor;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;


public class StoreProcessor extends AbstractImapRequestProcessor {
	
	public StoreProcessor(final ImapProcessor next) {
        super(next);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof StoreRequest);
    }
    
    protected ImapResponseMessage doProcess(ImapRequest message, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
		final StoreRequest request = (StoreRequest) message;
        final ImapResponseMessage result = doProcess(request, session, tag, command);
		return result;
	}

	private ImapResponseMessage doProcess(StoreRequest request, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
		final IdRange[] idSet = request.getIdSet();
	    final StoreDirective directive = request.getDirective();
	    final Flags flags = request.getFlags();
	    final boolean useUids = request.isUseUids();
		final ImapResponseMessage result = doProcess(idSet, directive, flags, useUids, session, tag, command);
		return result;
	}
	
	private ImapResponseMessage doProcess(final IdRange[] idSet, final StoreDirective directive,
			final Flags flags, final boolean useUids, 
			ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {

        ImapMailboxSession mailbox = session.getSelected().getMailbox();
        MailboxListener silentListener = null;

        final boolean replace;
        final boolean value;
        if (directive.getSign() < 0) {
            value=false;
            replace=false;
        }
        else if (directive.getSign() > 0) {
            value=true;
            replace=false;
        }
        else {
            replace=true;
            value=true;
        }
        try {
            if (directive.isSilent()) {
                silentListener = session.getSelected().getMailbox();
            }
            for (int i = 0; i < idSet.length; i++) {
                final GeneralMessageSet messageSet = GeneralMessageSetImpl
                        .range(idSet[i].getLowVal(), idSet[i].getHighVal(),
                                useUids);

                mailbox.setFlags(flags, value, replace, messageSet,
                        silentListener);
            }
        } catch (MailboxManagerException e) {
            throw new MailboxException(e);
        }
        
        final StoreResponse result = 
            new StoreResponse(command, useUids, tag);
        return result;
	}
}
