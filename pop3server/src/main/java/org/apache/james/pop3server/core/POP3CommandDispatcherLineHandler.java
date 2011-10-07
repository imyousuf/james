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

package org.apache.james.pop3server.core;

import java.util.Arrays;
import java.util.List;

import javax.annotation.Resource;

import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.handler.AbstractCommandDispatcher;
import org.apache.james.protocols.api.handler.CommandHandler;

/**
 * Dispatch POP3 {@link CommandHandler}
 */
public class POP3CommandDispatcherLineHandler extends AbstractCommandDispatcher<POP3Session> {
    private final static String[] mandatoryCommands = { "USER", "PASS", "LIST" };
    private final CommandHandler<POP3Session> unknownHandler = new UnknownCmdHandler();
    private MailboxManager manager;

    @Resource(name = "mailboxmanager")
    public void setMailboxManager(MailboxManager manager) {
        this.manager = manager;
    }

    /**
     * @see org.apache.james.protocols.api.handler.AbstractCommandDispatcher#getMandatoryCommands()
     */
    protected List<String> getMandatoryCommands() {
        return Arrays.asList(mandatoryCommands);
    }

    /**
     * @see org.apache.james.protocols.api.handler.AbstractCommandDispatcher#getUnknownCommandHandler()
     */
    protected CommandHandler<POP3Session> getUnknownCommandHandler() {
        return unknownHandler;
    }

    /**
     * @see org.apache.james.protocols.api.handler.AbstractCommandDispatcher#getUnknownCommandHandlerIdentifier()
     */
    protected String getUnknownCommandHandlerIdentifier() {
        return UnknownCmdHandler.COMMAND_NAME;
    }

    
    @Override
    public Response onLine(POP3Session session, byte[] line) {
        MailboxSession mSession = (MailboxSession) session.getState().get(POP3Session.MAILBOX_SESSION);

        // notify the mailboxmanager about the start of the processing
        manager.startProcessingRequest(mSession);

        // do the processing
        Response response = super.onLine(session, line);

        // notify the mailboxmanager about the end of the processing
        manager.endProcessingRequest(mSession);

        return response;
    }

}
