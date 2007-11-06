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
package org.apache.james.imapserver.processor.base;

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.ImapResponseMessage;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.imap.message.response.imap4rev1.legacy.CommandFailedResponse;
import org.apache.james.imap.message.response.imap4rev1.legacy.ErrorResponse;
import org.apache.james.imapserver.store.MailboxException;

abstract public class AbstractImapRequestProcessor extends
        AbstractChainedImapProcessor implements ImapConstants {

    private final StatusResponseFactory factory;    
    public AbstractImapRequestProcessor(final ImapProcessor next, final StatusResponseFactory factory) {
        super(next);
        this.factory = factory;
    }

    protected final void doProcess(
            final ImapMessage acceptableMessage, final Responder responder, 
            final ImapSession session) {
        final ImapRequest request = (ImapRequest) acceptableMessage;
        process(request, responder, session);
    }

    protected final void process(final ImapRequest message, final Responder responder,
            final ImapSession session) {
        final Logger logger = getLogger();
        final ImapCommand command = message.getCommand();
        final String tag = message.getTag();
        try {
            doProcess(message, command, tag, responder, session);
        } catch (MailboxException e) {
            if (logger != null) {
                logger.debug("error processing command ", e);
            }
             final ImapResponseMessage response = new CommandFailedResponse(command, e.getResponseCode(), e
                    .getMessage(), tag);
             responder.respond(response);
        } catch (AuthorizationException e) {
            if (logger != null) {
                logger.debug("error processing command ", e);
            }
            final String msg = "Authorization error: Lacking permissions to perform requested operation.";
            final ImapResponseMessage response = new CommandFailedResponse(command, null, msg, tag);
            responder.respond(response);
            
        } catch (ProtocolException e) {
            if (logger != null) {
                logger.debug("error processing command ", e);
            }
            final String msg = e.getMessage() + " Command should be '"
                    + command.getExpectedMessage() + "'";
            final ImapResponseMessage response = new ErrorResponse(msg, tag);
            responder.respond(response);
            
        }
    }

    final void doProcess(final ImapRequest message,
            final ImapCommand command, final String tag, Responder responder, ImapSession session)
            throws MailboxException, AuthorizationException, ProtocolException {
        if (!command.validForState(session.getState())) {
            ImapResponseMessage response = new CommandFailedResponse(command,
                    "Command not valid in this state.", tag);
            responder.respond(response);
            
        } else {
            doProcess(message, session, tag, command, responder);
        }
    }

    protected void okComplete(final ImapCommand command, final String tag, 
            final ImapProcessor.Responder responder) {
        final StatusResponse response = factory.taggedOk(tag, command, HumanReadableTextKey.COMPLETED);
        responder.respond(response);
    }
    
    protected abstract void doProcess(final ImapRequest message,
            ImapSession session, String tag, ImapCommand command, Responder responder)
            throws MailboxException, AuthorizationException, ProtocolException;
    

}
