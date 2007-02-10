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

import java.util.Date;

import javax.mail.Flags;
import javax.mail.internet.MimeMessage;

import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.mailbox.ImapMailboxSession;

class AppendCommandMessage extends AbstractImapCommandMessage {
    private String mailboxName;
    private Flags flags;
    private Date datetime;
    private MimeMessage message;
            
    public AppendCommandMessage(ImapCommand command, String mailboxName, Flags flags, 
            Date datetime, MimeMessage message, String tag) {
        super(tag, command);
        this.mailboxName = mailboxName;
        this.flags = flags;
        this.datetime = datetime;
        this.message = message;
    }

    public Date getDatetime() {
        return datetime;
    }

    public Flags getFlags() {
        return flags;
    }

    public String getMailboxName() {
        return mailboxName;
    }

    public MimeMessage getMessage() {
        return message;
    }
    
    public ImapResponseMessage doProcess( final ImapSession session, String tag, ImapCommand command ) throws MailboxException {
        ImapMailboxSession mailbox = null;
        try {
            mailboxName=session.buildFullName(mailboxName);
            mailbox = session.getMailboxManager().getImapMailboxSession(mailboxName);
        }
        catch ( MailboxManagerException mme ) {
            MailboxException me = new MailboxException(mme);
            me.setResponseCode( "TRYCREATE" );
            throw me;
        }

        try {
            mailbox.appendMessage( message, datetime ,0);
        } catch (MailboxManagerException e) {
            // TODO why not TRYCREATE?
            throw new MailboxException(e);
        }
        return new CommandCompleteResponseMessage(false, command, tag);
    }
}
