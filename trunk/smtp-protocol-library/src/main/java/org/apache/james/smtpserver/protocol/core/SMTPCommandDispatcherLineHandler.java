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

package org.apache.james.smtpserver.protocol.core;

import java.util.Arrays;
import java.util.List;

import org.apache.james.api.protocol.AbstractCommandDispatcher;
import org.apache.james.api.protocol.CommandHandler;
import org.apache.james.api.protocol.Response;
import org.apache.james.smtpserver.protocol.SMTPRequest;
import org.apache.james.smtpserver.protocol.SMTPResponse;
import org.apache.james.smtpserver.protocol.SMTPRetCode;
import org.apache.james.smtpserver.protocol.SMTPSession;


/**
 * Dispatch CommandHandler for SMTP Requests
 * 
 *
 */
public class SMTPCommandDispatcherLineHandler extends AbstractCommandDispatcher<SMTPSession> {


    private final CommandHandler<SMTPSession> unknownHandler = new UnknownCmdHandler();

    private final static String[] mandatoryCommands = { "MAIL" , "RCPT", "DATA"};
    

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#dispatchCommand(org.apache.james.api.protocol.ProtocolSession, java.lang.String, java.lang.String)
     */
    protected void dispatchCommand(SMTPSession session, String command, String argument) {
       
        List<CommandHandler<SMTPSession>> commandHandlers = getCommandHandlers(command, session);
        // fetch the command handlers registered to the command
        if (commandHandlers == null) {
            // end the session
            SMTPResponse resp = new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Local configuration error: unable to find a command handler.");
            resp.setEndSession(true);
            session.writeResponse(resp);
        } else {
            int count = commandHandlers.size();
            for (int i = 0; i < count; i++) {
                Response response = commandHandlers.get(i).onCommand(session, new SMTPRequest(command, argument));

                session.writeResponse(response);

                // if the response is received, stop processing of command
                // handlers
                if (response != null) {
                    break;
                }

                // NOTE we should never hit this line, otherwise we ended the
                // CommandHandlers with
                // no responses.
                // (The note is valid for i == count-1)
            }

        }

    }


    /**
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#getUnknownCommandHandlerIdentifier()
     */
    protected String getUnknownCommandHandlerIdentifier() {
        return UnknownCmdHandler.UNKNOWN_COMMAND;
    }

    /**
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#getMandatoryCommands()
     */
    protected List<String> getMandatoryCommands() {
        return Arrays.asList(mandatoryCommands);
    }

    /**
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#getUnknownCommandHandler()
     */
    protected CommandHandler<SMTPSession> getUnknownCommandHandler() {
        return unknownHandler;
    }

}
