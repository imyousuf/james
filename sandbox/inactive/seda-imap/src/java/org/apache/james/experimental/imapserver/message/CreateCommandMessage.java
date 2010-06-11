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

import org.apache.james.experimental.imapserver.AuthorizationException;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.ProtocolException;
import org.apache.james.experimental.imapserver.commands.ImapCommand;
import org.apache.james.experimental.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;

class CreateCommandMessage extends AbstractImapCommandMessage {
    private final String mailboxName;
    public CreateCommandMessage(final ImapCommand command, final String mailboxName, final String tag) {
        super(tag, command);
        this.mailboxName = mailboxName;
    }
    
    protected ImapResponseMessage doProcess(ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        try {

            final String fullMailboxName=session.buildFullName(this.mailboxName);
            session.getMailboxManager().createMailbox(fullMailboxName );
        } catch (MailboxManagerException e) {
           throw new MailboxException(e);
        }
        return new CommandCompleteResponseMessage(false, command, tag);
    }
}
