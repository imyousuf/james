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
import org.apache.james.imap.message.request.imap4rev1.LoginRequest;
import org.apache.james.imapserver.processor.base.AbstractMailboxAwareProcessor;
import org.apache.james.imapserver.processor.base.ImapSessionUtils;
import org.apache.james.mailboxmanager.MailboxManagerException;
import org.apache.james.mailboxmanager.manager.MailboxManager;
import org.apache.james.mailboxmanager.manager.MailboxManagerProvider;

/**
 * Processes a <code>LOGIN</code> command.
 */
public class LoginProcessor extends AbstractMailboxAwareProcessor {

    private static final String ATTRIBUTE_NUMBER_OF_FAILURES 
        = "org.apache.james.imapserver.processor.imap4rev1.LoginProcessor.NUMBER_OF_FAILURES";
    // TODO: this should be configurable
    private static final int MAX_FAILURES = 3;

    public LoginProcessor(final ImapProcessor next, final MailboxManagerProvider mailboxManagerProvider, 
            final StatusResponseFactory factory) {
        super(next, mailboxManagerProvider, factory);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof LoginRequest);
    }

    protected void doProcess(ImapRequest message,
            ImapSession session, String tag, ImapCommand command, Responder responder) {
        try
        {
            final LoginRequest request = (LoginRequest) message;
            final String userid = request.getUserid();
            final String passwd = request.getPassword();
            final MailboxManager mailboxManager = getMailboxManager(session);
            if (mailboxManager.isAuthentic(userid, passwd)) {
                session.authenticated();
                ImapSessionUtils.setUserName(session, userid);
                okComplete(command, tag, responder);
            } else {
                final Integer currentNumberOfFailures = (Integer) session.getAttribute(ATTRIBUTE_NUMBER_OF_FAILURES);
                final int failures;
                if (currentNumberOfFailures == null) {
                    failures = 1;
                } else {
                    failures = currentNumberOfFailures.intValue() + 1;
                }
                if (failures < MAX_FAILURES) {
                    session.setAttribute(ATTRIBUTE_NUMBER_OF_FAILURES, new Integer(failures));
                    no(command,tag, responder, HumanReadableTextKey.INVALID_LOGIN);
                } else {
                    bye(responder, HumanReadableTextKey.TOO_MANY_FAILURES);
                    session.logout();
                }
            }
        } catch (MailboxManagerException e) {
            getLogger().debug("Login failed", e);
            final HumanReadableTextKey displayTextKey = HumanReadableTextKey.INVALID_LOGIN;
            no(command, tag, responder, displayTextKey);
        }
    }
}
