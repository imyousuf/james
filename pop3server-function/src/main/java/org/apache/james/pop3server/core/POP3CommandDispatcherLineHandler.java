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

import org.apache.james.api.protocol.AbstractCommandDispatcher;
import org.apache.james.api.protocol.CommandHandler;
import org.apache.james.api.protocol.Response;
import org.apache.james.pop3server.POP3Request;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;

/**
 * Dispatch 
 * @author norman
 *
 */
public class POP3CommandDispatcherLineHandler extends
        AbstractCommandDispatcher<POP3Session> {
    private final static String[] mandatoryCommands = { "USER", "PASS", "LIST" };
    private final CommandHandler<POP3Session> unknownHandler = new UnknownCmdHandler();
  


    /**
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#getMandatoryCommands()
     */
    protected List<String> getMandatoryCommands() {
        return Arrays.asList(mandatoryCommands);
    }

    /**
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#getUnknownCommandHandler()
     */
    protected CommandHandler<POP3Session> getUnknownCommandHandler() {
        return unknownHandler;
    }

    /**
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#getUnknownCommandHandlerIdentifier()
     */
    protected String getUnknownCommandHandlerIdentifier() {
        return UnknownCmdHandler.COMMAND_NAME;
    }

    @Override
    protected void dispatchCommand(POP3Session session, String command, String argument) {
        // fetch the command handlers registered to the command
        List<CommandHandler<POP3Session>> commandHandlers = getCommandHandlers(command, session);
        if (commandHandlers == null) {
            // end the session
            POP3Response resp = new POP3Response(POP3Response.ERR_RESPONSE, "Local configuration error: unable to find a command handler.");
            resp.setEndSession(true);
            session.writeResponse(resp);
        } else {
            int count = commandHandlers.size();
            for (int i = 0; i < count; i++) {
                Response response = commandHandlers.get(i).onCommand(session, new POP3Request(command, argument));
                if (response != null) {
                    session.writeResponse(response);
                    break;
                }
            }

        }
       
    }


}
