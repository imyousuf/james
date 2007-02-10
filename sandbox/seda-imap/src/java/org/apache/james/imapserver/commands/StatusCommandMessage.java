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

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.imapserver.AuthorizationException;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;

class StatusCommandMessage extends AbstractImapCommandMessage {
    private final String mailboxName;
    private final StatusDataItems statusDataItems;
    
    public StatusCommandMessage(final ImapCommand command, final String mailboxName, final StatusDataItems statusDataItems, final String tag) {
        super(tag, command);
        this.mailboxName = mailboxName;
        this.statusDataItems = statusDataItems;
    }
    
    protected ImapResponseMessage doProcess(ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        final Logger logger = getLogger(); 

        StringBuffer buffer = new StringBuffer( mailboxName );
        buffer.append( StatusCommand.SP );
        buffer.append( "(" );
        try {
            String fullMailboxName= session.buildFullName(mailboxName);
            
            if (logger != null && logger.isDebugEnabled()) { 
                logger.debug("Status called on mailbox named " + mailboxName + " (" + fullMailboxName + ")"); 
            }
            
            ImapMailboxSession mailbox = session.getMailboxManager().getImapMailboxSession(fullMailboxName);
            
            if (statusDataItems.messages) {
                buffer.append(StatusCommand.MESSAGES);
                buffer.append(StatusCommand.SP);

                buffer.append(mailbox.getMessageCount());

                buffer.append(StatusCommand.SP);
            }

            if (statusDataItems.recent) {
                buffer.append(StatusCommand.RECENT);
                buffer.append(StatusCommand.SP);
                buffer.append(mailbox.getRecentCount(false));
                buffer.append(StatusCommand.SP);
            }

            if (statusDataItems.uidNext) {
                buffer.append(StatusCommand.UIDNEXT);
                buffer.append(StatusCommand.SP);
                buffer.append(mailbox.getUidNext());
                buffer.append(StatusCommand.SP);
            }

            if (statusDataItems.uidValidity) {
                buffer.append(StatusCommand.UIDVALIDITY);
                buffer.append(StatusCommand.SP);
                buffer.append(mailbox.getUidValidity());
                buffer.append(StatusCommand.SP);
            }

            if (statusDataItems.unseen) {
                buffer.append(StatusCommand.UNSEEN);
                buffer.append(StatusCommand.SP);
                buffer.append(mailbox.getUnseenCount());
                buffer.append(StatusCommand.SP);
            }
        } catch (MailboxManagerException e) {
            if (logger != null && logger.isDebugEnabled()) { 
                logger.debug("STATUS command failed: ", e); 
            }
            throw new MailboxException(e);
        }
        if ( buffer.charAt( buffer.length() - 1 ) == ' ' ) {
            buffer.setLength( buffer.length() - 1 );
        }
        buffer.append(')');
        final StatusResponseMessage result = 
            new StatusResponseMessage(command, buffer.toString(), tag);
        return result;
    }
}
