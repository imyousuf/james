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
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.api.user.User;
import org.apache.james.imap.message.request.imap4rev1.SubscribeRequest;
import org.apache.james.imapserver.processor.base.AbstractImapRequestProcessor;
import org.apache.james.imapserver.processor.base.ImapSessionUtils;

public class SubscribeProcessor extends AbstractImapRequestProcessor {

    private final IMAPSubscriber subscriber;
    
    public SubscribeProcessor(final ImapProcessor next,
            final StatusResponseFactory factory, final IMAPSubscriber subscriber) {
        super(next, factory);
        this.subscriber = subscriber;
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof SubscribeRequest);
    }

    protected void doProcess(ImapRequest message,
            ImapSession session, String tag, ImapCommand command, Responder responder) {
        final SubscribeRequest request = (SubscribeRequest) message;
        final String mailboxName = request.getMailboxName();
        final User user = ImapSessionUtils.getUser(session);
        final String userName = user.getUserName();
        try {
            subscriber.subscribe(userName, mailboxName);
            
            unsolicitedResponses(session, responder, false);
            okComplete(command, tag, responder);
            
        } catch (SubscriptionException e) {
            getLogger().debug("Subscription failed", e);
            unsolicitedResponses(session, responder, false);
            
            final HumanReadableTextKey exceptionKey = e.getKey();
            final HumanReadableTextKey displayTextKey;
            if (exceptionKey == null) {
                displayTextKey = HumanReadableTextKey.GENERIC_SUBSCRIPTION_FAILURE;
            } else {
                displayTextKey = exceptionKey;
            }
            no(command, tag, responder, displayTextKey);
        }
    }
}
