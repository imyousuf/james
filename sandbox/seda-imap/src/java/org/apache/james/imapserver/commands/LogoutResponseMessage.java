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

import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.store.MailboxException;

class LogoutResponseMessage extends AbstractCommandResponseMessage implements ImapCommandMessage {

    public LogoutResponseMessage(final ImapCommand command, final String tag) {
        super(command, tag);
    }

    void doEncode(ImapResponse response, ImapSession session, ImapCommand command, String tag) throws MailboxException {
        response.byeResponse( LogoutCommand.BYE_MESSAGE );
        response.commandComplete( command, tag );
        // TODO: think about how this will work with SEDA
        session.closeConnection();            
    }

    public ImapResponseMessage process(ImapSession session) {
        return this;
    }
}
