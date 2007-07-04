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
import org.apache.james.imap.message.request.imap4rev1.LogoutRequest;
import org.apache.james.imap.message.response.imap4rev1.legacy.LogoutResponse;
import org.apache.james.imapserver.processor.base.AbstractImapRequestProcessor;
import org.apache.james.imapserver.processor.base.AuthorizationException;
import org.apache.james.imapserver.store.MailboxException;

public class LogoutProcessor extends AbstractImapRequestProcessor {

    public LogoutProcessor(final ImapProcessor next) {
        super(next);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof LogoutRequest);
    }

    protected ImapResponseMessage doProcess(ImapRequest message,
            ImapSession session, String tag, ImapCommand command)
            throws MailboxException, AuthorizationException, ProtocolException {
        final LogoutRequest request = (LogoutRequest) message;
        final ImapResponseMessage result = doProcess(request, session, tag,
                command);
        return result;
    }

    private ImapResponseMessage doProcess(LogoutRequest request,
            ImapSession session, String tag, ImapCommand command)
            throws MailboxException, AuthorizationException, ProtocolException {
        final ImapResponseMessage result = doProcess(session, tag, command);
        return result;
    }

    private ImapResponseMessage doProcess(ImapSession session, String tag,
            ImapCommand command) throws MailboxException,
            AuthorizationException, ProtocolException {
        session.logout();
        final LogoutResponse result = new LogoutResponse(command, tag);
        return result;
    }
}
