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

package org.apache.james.imapserver.processor.imap4rev1;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.ImapResponseMessage;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.imap.message.request.imap4rev1.LoginRequest;
import org.apache.james.imap.message.response.imap4rev1.legacy.CommandCompleteResponse;
import org.apache.james.imap.message.response.imap4rev1.legacy.CommandFailedResponse;
import org.apache.james.imapserver.processor.base.AbstractImapRequestProcessor;
import org.apache.james.imapserver.processor.base.AuthorizationException;
import org.apache.james.imapserver.processor.base.ImapSessionUtils;
import org.apache.james.imapserver.store.MailboxException;
import org.apache.james.services.User;
import org.apache.james.services.UsersRepository;


public class LoginProcessor extends AbstractImapRequestProcessor {
	
    private final UsersRepository users;
    
	public LoginProcessor(final ImapProcessor next, final UsersRepository users) {
        super(next);
        this.users = users;
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof LoginRequest);
    }

    protected ImapResponseMessage doProcess(ImapRequest message, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        final LoginRequest request = (LoginRequest) message;
        final ImapResponseMessage result = doProcess(request, session, tag, command);
		return result;
	}

	private ImapResponseMessage doProcess(LoginRequest request, ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
		final String userid = request.getUserid();
		final String passwd = request.getPassword();
		final ImapResponseMessage result = doProcess(userid, passwd, session, tag, command);
		return result;
	}
	
	private ImapResponseMessage doProcess(final String userid, final String password, 
			ImapSession session, String tag, ImapCommand command) throws MailboxException, AuthorizationException, ProtocolException {
        final ImapResponseMessage result;
        if ( users.test( userid, password ) ) {
            User user = users.getUserByName( userid );
            session.authenticated();
            ImapSessionUtils.setUser( session, user ); 
            result = new CommandCompleteResponse(command, tag);
        }
        else {
            result = new CommandFailedResponse( command, "Invalid login/password", tag );
        }
        return result;
	}
}
