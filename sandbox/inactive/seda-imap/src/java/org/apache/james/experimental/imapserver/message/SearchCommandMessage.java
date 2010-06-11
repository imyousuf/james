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
package org.apache.james.experimental.imapserver.message;

import javax.mail.search.SearchTerm;

import org.apache.james.experimental.imapserver.AuthorizationException;
import org.apache.james.experimental.imapserver.ImapConstants;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.ProtocolException;
import org.apache.james.experimental.imapserver.commands.ImapCommand;
import org.apache.james.experimental.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.MessageResult;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;

class SearchCommandMessage extends AbstractImapCommandMessage {
    private final SearchTerm searchTerm;
    private final boolean useUids;

    public SearchCommandMessage(final ImapCommand command, final SearchTerm searchTerm, final boolean useUids,
            final String tag) {
        super(tag, command);
        this.searchTerm = searchTerm;
        this.useUids = useUids;
    }

    protected ImapResponseMessage doProcess(ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        ImapMailboxSession mailbox = session.getSelected().getMailbox();
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
        final SearchResponseMessage result = 
            new SearchResponseMessage(command, idList.toString(), 
                    useUids, tag);
        return result;
    }
}
