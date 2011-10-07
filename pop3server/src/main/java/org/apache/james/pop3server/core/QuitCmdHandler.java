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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.Resource;
import javax.mail.Flags;

import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.handler.CommandHandler;

/**
 * Handles QUIT command
 */
public class QuitCmdHandler implements CommandHandler<POP3Session> {
    private final static String COMMAND_NAME = "QUIT";
    protected MailboxManager mailboxManager;

    @Resource(name = "mailboxmanager")
    public void setMailboxManager(MailboxManager manager) {
        this.mailboxManager = manager;
    }

    /**
     * Handler method called upon receipt of a QUIT command. This method handles
     * cleanup of the POP3Handler state.
     */
    @SuppressWarnings("unchecked")
    public Response onCommand(POP3Session session, Request request) {
        POP3Response response = null;
        if (session.getHandlerState() == POP3Session.AUTHENTICATION_READY || session.getHandlerState() == POP3Session.AUTHENTICATION_USERSET) {
            response = new POP3Response(POP3Response.OK_RESPONSE, "Apache James POP3 Server signing off.");
            response.setEndSession(true);
            return response;
        }
        MailboxSession mailboxSession = (MailboxSession) session.getState().get(POP3Session.MAILBOX_SESSION);

        List<Long> toBeRemoved = (List<Long>) session.getState().get(POP3Session.DELETED_UID_LIST);
        try {
            MessageManager mailbox = session.getUserMailbox();

            for (int i = 0; i < toBeRemoved.size(); i++) {
                MessageRange range = MessageRange.one(toBeRemoved.get(i));
                mailbox.setFlags(new Flags(Flags.Flag.DELETED), true, false, range, mailboxSession);
                mailbox.expunge(range, mailboxSession);
            }
            response = new POP3Response(POP3Response.OK_RESPONSE, "Apache James POP3 Server signing off.");
        } catch (Exception ex) {
            response = new POP3Response(POP3Response.ERR_RESPONSE, "Some deleted messages were not removed");
            session.getLogger().error("Some deleted messages were not removed", ex);
        }
        response.setEndSession(true);
        try {
            mailboxManager.logout(mailboxSession, false);
        } catch (MailboxException e) {
            // nothing todo on logout
        }

        return response;
    }

    /**
     * @see org.apache.james.protocols.api.handler.CommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

}
