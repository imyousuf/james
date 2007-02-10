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
import org.apache.james.services.User;

class LoginCommandMessage extends AbstractImapCommandMessage {
    private final String userid;
    private final String password;
    
    public LoginCommandMessage(final ImapCommand command, final String userid, final String password, String tag) {
        super(tag, command);
        this.userid = userid;
        this.password = password;
    }

    protected ImapResponseMessage doProcess(ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        ImapResponseMessage result;
        if ( session.getUsers().test( userid, password ) ) {
            User user = session.getUsers().getUserByName( userid );
            session.setAuthenticated( user );
            result = CommandCompleteResponseMessage.createWithNoUnsolictedResponses(command, tag);
        }
        else {
            result = new CommandFailedResponseMessage( command, "Invalid login/password", tag );
        }
        return result;
    }
    
}
