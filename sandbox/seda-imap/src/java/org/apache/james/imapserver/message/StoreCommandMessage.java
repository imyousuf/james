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
package org.apache.james.imapserver.message;

import javax.mail.Flags;

import org.apache.james.imapserver.AuthorizationException;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.commands.ImapCommand;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.GeneralMessageSet;
import org.apache.james.mailboxmanager.MailboxListener;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.impl.GeneralMessageSetImpl;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;

class StoreCommandMessage extends AbstractImapCommandMessage {
    private final IdRange[] idSet;
    private final StoreDirective directive;
    private final Flags flags;
    private final boolean useUids;
    
    public StoreCommandMessage(final ImapCommand command, final IdRange[] idSet, final StoreDirective directive, final Flags flags, 
            final boolean useUids, final String tag) {
        super(tag, command);
        this.idSet = idSet;
        this.directive = directive;
        this.flags = flags;
        this.useUids = useUids;
    }
    
    protected ImapResponseMessage doProcess(ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {

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
        
        final StoreResponseMessage result = 
            new StoreResponseMessage(command, useUids, tag);
        return result;
    }
}
