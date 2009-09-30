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



package org.apache.james.pop3server;

import java.util.ArrayList;
import java.util.List;

import org.apache.james.services.MailRepository;
import org.apache.james.util.POP3BeforeSMTPHelper;

/**
  * Handles PASS command
  */
public class PassCmdHandler implements CommandHandler {

	private final static String COMMAND_NAME ="PASS";
    /**
     * @see org.apache.james.pop3server.CommandHandler#onCommand(POP3Session)
     */
    public void onCommand(POP3Session session) {
        doPASS(session,session.getCommandArgument());
    }

    /**
     * Handler method called upon receipt of a PASS command.
     * Reads in and validates the password.
     *
     * @param argument the first argument parsed by the parseCommand method
     */
    private void doPASS(POP3Session session,String argument) {
        String responseString = null;
        if (session.getHandlerState() == POP3Handler.AUTHENTICATION_USERSET && argument != null) {
            String passArg = argument;
            if (session.getConfigurationData().getUsersRepository().test(session.getUser(), passArg)) {
                try {
                    MailRepository inbox = session.getConfigurationData().getMailServer().getUserInbox(session.getUser());
                    if (inbox == null) {
                        throw new IllegalStateException("MailServer returned a null inbox for "+session.getUser());
                    }
                    session.setUserInbox(inbox);
                    RsetCmdHandler.stat(session);
                    
                    // Store the ipAddress to use it later for pop before smtp 
                    POP3BeforeSMTPHelper.addIPAddress(session.getRemoteIPAddress());
                    
                    StringBuilder responseBuffer =
                        new StringBuilder(64)
                                .append(POP3Handler.OK_RESPONSE)
                                .append(" Welcome ")
                                .append(session.getUser());
                    responseString = responseBuffer.toString();
                    session.setHandlerState(POP3Handler.TRANSACTION);
                    session.writeResponse(responseString);
                } catch (RuntimeException e) {
                    session.getLogger().error("Unexpected error accessing mailbox for "+session.getUser(),e);
                    responseString = POP3Handler.ERR_RESPONSE + " Unexpected error accessing mailbox";
                    session.setHandlerState(POP3Handler.AUTHENTICATION_READY);
                    session.writeResponse(responseString);
                }
            } else {
                responseString = POP3Handler.ERR_RESPONSE + " Authentication failed.";
                session.setHandlerState(POP3Handler.AUTHENTICATION_READY);
                session.writeResponse(responseString);
            }
        } else {
            responseString = POP3Handler.ERR_RESPONSE;
            session.writeResponse(responseString);
        }
    }

    /**
     * @see org.apache.james.pop3server.CommandHandler#getCommands()
     */
	public List<String> getCommands() {
		List<String> commands = new ArrayList<String>();
		commands.add(COMMAND_NAME);
		return commands;
	}


}
