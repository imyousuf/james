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

import org.apache.james.mailbox.BadCredentialsException;
import org.apache.james.mailbox.MailboxPath;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.lib.POP3BeforeSMTPHelper;

/**
 * Handles PASS command
 */
public class PassCmdHandler extends RsetCmdHandler {

    private final static String COMMAND_NAME = "PASS";
    private MailboxManager mailboxManager;

    @Resource(name = "mailboxmanager")
    public void setMailboxManager(MailboxManager manager) {
        this.mailboxManager = manager;
    }

    /**
     * Handler method called upon receipt of a PASS command. Reads in and
     * validates the password.
     */
    public Response onCommand(POP3Session session, Request request) {
        String parameters = request.getArgument();
        POP3Response response = null;
        if (session.getHandlerState() == POP3Session.AUTHENTICATION_USERSET && parameters != null) {
            String passArg = parameters;
            try {
                MailboxSession mSession = mailboxManager.login(session.getUser(), passArg, session.getLogger());

                // explicit call start processing because it was not stored
                // before in the session
                mailboxManager.startProcessingRequest(mSession);

                MailboxPath mailboxPath = MailboxPath.inbox(mSession);

                // check if mailbox exists.. if not just create it
                if (mailboxManager.mailboxExists(mailboxPath, mSession) == false) {
                    mailboxManager.createMailbox(mailboxPath, mSession);
                }
                MessageManager mailbox = mailboxManager.getMailbox(mailboxPath, mSession);

                session.getState().put(POP3Session.MAILBOX_SESSION, mSession);
                session.setUserMailbox(mailbox);
                stat(session);

                // Store the ipAddress to use it later for pop before smtp
                POP3BeforeSMTPHelper.addIPAddress(session.getRemoteIPAddress());

                StringBuilder responseBuffer = new StringBuilder(64).append("Welcome ").append(session.getUser());
                response = new POP3Response(POP3Response.OK_RESPONSE, responseBuffer.toString());
                session.setHandlerState(POP3Session.TRANSACTION);
            } catch (BadCredentialsException e) {

                response = new POP3Response(POP3Response.ERR_RESPONSE, "Authentication failed.");
                session.setHandlerState(POP3Session.AUTHENTICATION_READY);
            } catch (MailboxException e) {
                session.getLogger().error("Unexpected error accessing mailbox for " + session.getUser(), e);
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Unexpected error accessing mailbox");
                session.setHandlerState(POP3Session.AUTHENTICATION_READY);
            }
        } else {
            response = new POP3Response(POP3Response.ERR_RESPONSE, "Authentication failed.");

            session.setHandlerState(POP3Session.AUTHENTICATION_READY);
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
