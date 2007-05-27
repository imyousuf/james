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
package org.apache.james.experimental.imapserver.processor.base;

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.experimental.imapserver.AuthorizationException;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.message.request.ImapRequest;
import org.apache.james.experimental.imapserver.message.response.ImapResponseMessage;
import org.apache.james.experimental.imapserver.message.response.imap4rev1.CommandFailedResponse;
import org.apache.james.experimental.imapserver.message.response.imap4rev1.ErrorResponse;
import org.apache.james.experimental.imapserver.processor.ImapProcessor;
import org.apache.james.imapserver.store.MailboxException;

abstract public class AbstractImapRequestProcessor extends
        AbstractChainedImapProcessor {

    public AbstractImapRequestProcessor(final ImapProcessor next) {
        super(next);
    }

    protected final ImapResponseMessage doProcess(
            ImapMessage acceptableMessage, ImapSession session) {
        final ImapRequest request = (ImapRequest) acceptableMessage;
        final ImapResponseMessage result = process(request, session);
        return result;
    }

    protected final ImapResponseMessage process(ImapRequest message,
            ImapSession session) {
        ImapResponseMessage result;
        final Logger logger = getLogger();
        final ImapCommand command = message.getCommand();
        final String tag = message.getTag();
        try {
            result = doProcess(message, command, tag, session);
        } catch (MailboxException e) {
            if (logger != null) {
                logger.debug("error processing command ", e);
            }
            result = new CommandFailedResponse(command, e.getResponseCode(), e
                    .getMessage(), tag);
        } catch (AuthorizationException e) {
            if (logger != null) {
                logger.debug("error processing command ", e);
            }
            String msg = "Authorization error: Lacking permissions to perform requested operation.";
            result = new CommandFailedResponse(command, null, msg, tag);
        } catch (ProtocolException e) {
            if (logger != null) {
                logger.debug("error processing command ", e);
            }
            String msg = e.getMessage() + " Command should be '"
                    + command.getExpectedMessage() + "'";
            result = new ErrorResponse(msg, tag);
        }
        return result;
    }

    final ImapResponseMessage doProcess(final ImapRequest message,
            final ImapCommand command, final String tag, ImapSession session)
            throws MailboxException, AuthorizationException, ProtocolException {
        ImapResponseMessage result;
        if (!command.validForState(session.getState())) {
            result = new CommandFailedResponse(command,
                    "Command not valid in this state", tag);
        } else {
            result = doProcess(message, session, tag, command);
        }
        return result;
    }

    protected abstract ImapResponseMessage doProcess(final ImapRequest message,
            ImapSession session, String tag, ImapCommand command)
            throws MailboxException, AuthorizationException, ProtocolException;
}
