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

package org.apache.james.imapserver.commands;

import org.apache.james.imapserver.AuthorizationException;
import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;

/**
 * Handles processeing for the SUBSCRIBE imap command.
 *
 * @version $Revision: 109034 $
 */
class SubscribeCommand extends AuthenticatedStateCommand {
    public static final String NAME = "SUBSCRIBE";
    public static final String ARGS = "<mailbox>";

    /** @see ImapCommand#getName */
    public String getName() {
        return NAME;
    }

    /** @see CommandTemplate#getArgSyntax */
    public String getArgSyntax() {
        return ARGS;
    }

    protected AbstractImapCommandMessage decode(ImapRequestLineReader request) throws ProtocolException {
        final String mailboxName = parser.mailbox( request );
        parser.endLine( request );
        
        final SubscribeCommandMessage result = new SubscribeCommandMessage(mailboxName);
        return result;
    }
    
    private class SubscribeCommandMessage extends AbstractImapCommandMessage {
        private final String mailboxName;
        
        public SubscribeCommandMessage(final String mailboxName) {
            super();
            this.mailboxName = mailboxName;
        }

        protected ImapResponseMessage doProcess(ImapSession session) throws MailboxException, AuthorizationException, ProtocolException {
            try {
                final String fullMailboxName = session.buildFullName(this.mailboxName);
                session.getMailboxManager().setSubscription(fullMailboxName,true);
            } catch (MailboxManagerException e) {
               throw new MailboxException(e);
            }
            return new CommandCompleteResponseMessage(false, SubscribeCommand.this);
        }
        
    }
}
