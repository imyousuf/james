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

import javax.mail.Flags;

import org.apache.james.experimental.imapserver.ImapResponse;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.commands.ImapCommand;
import org.apache.james.experimental.imapserver.store.MailboxException;
import org.apache.james.mailboxmanager.MessageResult;

class SelectResponseMessage extends AbstractCommandResponseMessage {
    private final Flags permanentFlags;
    private final boolean writeable ;
    private final int recentCount;
    private final long uidValidity;
    private final MessageResult firstUnseen;
    private final int messageCount;

    public SelectResponseMessage(ImapCommand command, final Flags permanentFlags,
            final boolean writeable, final int recentCount, 
            final long uidValidity, final MessageResult firstUnseen,
            final int messageCount, final String tag) {
        super(command, tag);
        this.permanentFlags = permanentFlags;
        this.writeable = writeable;
        this.recentCount = recentCount;
        this.uidValidity = uidValidity;
        this.firstUnseen = firstUnseen;
        this.messageCount = messageCount;
    }        
    
    void doEncode(ImapResponse response, ImapSession session, ImapCommand command, String tag) throws MailboxException {
        response.flagsResponse(permanentFlags);
        response.recentResponse(recentCount);
        response.okResponse("UIDVALIDITY " + uidValidity, null);
        response.existsResponse(messageCount);
        if (firstUnseen != null) {
            response.okResponse("UNSEEN " + firstUnseen.getMsn(), "Message "
                    + firstUnseen.getMsn() + " is the first unseen");
        } else {
            response.okResponse(null, "No messages unseen");
        }
        response.permanentFlagsResponse(permanentFlags);
        if (!writeable) {
            response.commandComplete(command, "READ-ONLY", tag);
        } else {
            response.commandComplete(command, "READ-WRITE", tag);
        }
    }
}
