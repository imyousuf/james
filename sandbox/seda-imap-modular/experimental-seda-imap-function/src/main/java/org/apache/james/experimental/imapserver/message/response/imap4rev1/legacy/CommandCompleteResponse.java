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

package org.apache.james.experimental.imapserver.message.response.imap4rev1.legacy;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.encode.ImapResponse;
import org.apache.james.experimental.imapserver.message.response.AbstractImapResponse;
import org.apache.james.experimental.imapserver.message.response.ImapResponseMessage;
import org.apache.james.imapserver.store.MailboxException;

/**
 * 
 * TODO: replace this with ok response?
 * @deprecated responses should correspond directly to the specification
 */
public class CommandCompleteResponse extends AbstractImapResponse implements ImapResponseMessage {

    /**
     * Creates a command completed response message that
     * does not write unsolicited responses.
     * @param command <code>ImapCommand</code>, not null
     * @return <code>ImapResponseMessage</code>, not null
     */
    public static final ImapResponseMessage createWithNoUnsolictedResponses(final ImapCommand command, String tag) {
        final CommandCompleteResponse result = new CommandCompleteResponse(command, tag);
        return result;
    }
    
    private final boolean useUids;
    private final boolean writeUnsolicited;
    
    private CommandCompleteResponse(final ImapCommand command, String tag) {
        super(command, tag);
        writeUnsolicited = false;
        this.useUids = false;
    }
    
    /**
     * Constructs a command completed response message
     * that writes unsolicited responses.
     * 
     * @see #createWithNoUnsolictedResponses(ImapCommand)
     * @param useUids true if uids should be used, false otherwise
     * @param command <code>ImapCommand</code>, not null
     */
    public CommandCompleteResponse(final boolean useUids, final ImapCommand command, final String tag) {
        super(command, tag);
        writeUnsolicited = true;
        this.useUids = useUids;
    }

    protected void doEncode(ImapResponse response, ImapSession session, ImapCommand command, String tag) throws MailboxException {
        if (writeUnsolicited) {
            session.unsolicitedResponses( response, useUids);
        }
        response.commandComplete( command , tag );
    }
}
