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
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;

class CopyCommandMessage extends AbstractImapCommandMessage {

    private final IdRange[] idSet;
    private final String mailboxName;
    private final boolean useUids;

    public CopyCommandMessage(final ImapCommand command, final IdRange[] idSet, final String mailboxName, 
            final boolean useUids, final String tag) {
        super(tag, command);
        this.idSet = idSet;
        this.mailboxName = mailboxName;
        this.useUids = useUids;
    }

    protected ImapResponseMessage doProcess(ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        ImapMailboxSession currentMailbox = session.getSelected().getMailbox();
        try {
            String fullMailboxName = session.buildFullName(this.mailboxName);
            if (!session.getMailboxManager().existsMailbox(fullMailboxName)) {
                MailboxException e=new MailboxException("Mailbox does not exists");
                e.setResponseCode( "TRYCREATE" );
                throw e;
            }
            for (int i = 0; i < idSet.length; i++) {
                GeneralMessageSet messageSet=GeneralMessageSetImpl.range(idSet[i].getLowVal(),idSet[i].getHighVal(),useUids);
                session.getMailboxManager().copyMessages(currentMailbox,messageSet,fullMailboxName);
            }
        } catch (MailboxManagerException e) {
            throw new MailboxException(e);
        } 
        final CommandCompleteResponseMessage result = 
            new CommandCompleteResponseMessage(useUids, command, tag);
        return result;
    }
}
