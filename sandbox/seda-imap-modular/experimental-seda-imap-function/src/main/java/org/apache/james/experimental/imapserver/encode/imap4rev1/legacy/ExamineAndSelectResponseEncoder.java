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
package org.apache.james.experimental.imapserver.encode.imap4rev1.legacy;

import javax.mail.Flags;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.encode.ImapEncoder;
import org.apache.james.experimental.imapserver.encode.ImapResponseComposer;
import org.apache.james.experimental.imapserver.encode.base.AbstractChainedImapEncoder;
import org.apache.james.experimental.imapserver.message.response.imap4rev1.legacy.ExamineAndSelectResponse;
import org.apache.james.mailboxmanager.MessageResult;

/**
 * @deprecated responses should correspond directly to the specification
 */
public class ExamineAndSelectResponseEncoder extends AbstractChainedImapEncoder {

    
    public ExamineAndSelectResponseEncoder(ImapEncoder next) {
        super(next);
    }

    protected void doEncode(ImapMessage acceptableMessage, ImapResponseComposer composer, ImapSession session) {
        ExamineAndSelectResponse response = (ExamineAndSelectResponse) acceptableMessage;
        final Flags permanentFlags = response.getPermanentFlags();
        composer.flagsResponse(permanentFlags);
        final int recentCount = response.getRecentCount();
        composer.recentResponse(recentCount);
        final long uidValidity = response.getUidValidity();
        composer.okResponse("UIDVALIDITY " + uidValidity, null);
        final int messageCount = response.getMessageCount();
        composer.existsResponse(messageCount);
        final MessageResult firstUnseen = response.getFirstUnseen();
        if (firstUnseen != null) {
            composer.okResponse("UNSEEN " + firstUnseen.getMsn(), "Message "
                    + firstUnseen.getMsn() + " is the first unseen");
        } else {
            composer.okResponse(null, "No messages unseen");
        }
        composer.permanentFlagsResponse(permanentFlags);
        final boolean writeable = response.isWriteable();
        final ImapCommand command = response.getCommand();
        final String tag = response.getTag();
        if (!writeable) {
            composer.commandComplete(command, "READ-ONLY", tag);
        } else {
            composer.commandComplete(command, "READ-WRITE", tag);
        }        
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof ExamineAndSelectResponse);
    }
}
