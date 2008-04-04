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

import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.ImapResponseMessage;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.mailboxmanager.MailboxExistsException;
import org.apache.james.mailboxmanager.MailboxNotFoundException;

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
        final ImapCommand command = message.getCommand();
        final String tag = message.getTag();
        doProcess(message, command, tag, responder, session);
    }

    protected void no(final ImapCommand command, final String tag, final Responder responder, final MessagingException e) {
        final Logger logger = getLogger();
        final ImapResponseMessage response;
        if (e instanceof MailboxExistsException) {
            response = factory.taggedNo(tag, command, HumanReadableTextKey.FAILURE_MAILBOX_EXISTS);
        } else if (e instanceof MailboxNotFoundException) {
            response = factory.taggedNo(tag, command, HumanReadableTextKey.FAILURE_NO_SUCH_MAILBOX);
        } else {
            if (logger != null) {
                logger.info(e.getMessage());
                logger.debug("Processing failed:", e);
            }
            response = factory.taggedNo(tag, command, HumanReadableTextKey.GENERIC_FAILURE_DURING_PROCESSING);
        }
        responder.respond(response);
    }

    final void doProcess(final ImapRequest message,
            final ImapCommand command, final String tag, Responder responder, ImapSession session) {
        if (!command.validForState(session.getState())) {
            ImapResponseMessage response = factory.taggedNo(tag, command, HumanReadableTextKey.INVALID_COMMAND);
            responder.respond(response);
            
        } else {
            doProcess(message, session, tag, command, responder);
        }
    }
    
    protected void unsolicitedResponses(final ImapSession session, 
            final ImapProcessor.Responder responder, boolean omitExpunged, boolean useUids) {
        final List responses = session.unsolicitedResponses(omitExpunged, useUids);
        respond(responder, responses);
    }
    
    protected void unsolicitedResponses(final ImapSession session, 
            final ImapProcessor.Responder responder, boolean useUids) {
        final List responses = session.unsolicitedResponses(useUids);
        respond(responder, responses);
    }

    private void respond(final ImapProcessor.Responder responder, final List responses) {
        for (final Iterator it=responses.iterator(); it.hasNext();) {
            ImapResponseMessage message = (ImapResponseMessage) it.next();
            responder.respond(message);
        }
    }

    protected void okComplete(final ImapCommand command, final String tag, 
            final ImapProcessor.Responder responder) {
        final StatusResponse response = factory.taggedOk(tag, command, HumanReadableTextKey.COMPLETED);
        responder.respond(response);
    }
    
    protected void no(final ImapCommand command, final String tag, 
            final ImapProcessor.Responder responder, final HumanReadableTextKey displayTextKey) {
        final StatusResponse response = factory.taggedNo(tag, command, displayTextKey);
        responder.respond(response);
    }
    
    protected void no(final ImapCommand command, final String tag, 
            final ImapProcessor.Responder responder, final HumanReadableTextKey displayTextKey,
            final StatusResponse.ResponseCode responseCode) {
        final StatusResponse response = factory.taggedNo(tag, command, displayTextKey, responseCode);
        responder.respond(response);
    }
    
    protected void bye(final ImapProcessor.Responder responder) {
        final StatusResponse response = factory.bye(HumanReadableTextKey.BYE);
        responder.respond(response);
    }
    
    protected abstract void doProcess(final ImapRequest message,
            ImapSession session, String tag, ImapCommand command, Responder responder);
    

}
