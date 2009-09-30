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

import org.apache.mailet.Mail;

/**
  * Handles UIDL command
  */
public class UidlCmdHandler implements CommandHandler, CapaCapability {
	private final static String COMMAND_NAME = "UIDL";

    /**
     * @see org.apache.james.pop3server.CommandHandler#onCommand(POP3Session)
     */
    public void onCommand(POP3Session session) {
        doUIDL(session,session.getCommandArgument());
    }

    /**
     * Handler method called upon receipt of a UIDL command.
     * Returns a listing of message ids to the client.
     *
     * @param argument the first argument parsed by the parseCommand method
     */
    private void doUIDL(POP3Session session,String argument) {
        if (session.getHandlerState() == POP3Handler.TRANSACTION) {
            if (argument == null) {
                String responseString = POP3Handler.OK_RESPONSE + " unique-id listing follows";
                session.writeResponse(responseString);
                int count = 0;
                for (Mail mc:session.getUserMailbox()) {
                    if (mc != POP3Handler.DELETED) {
                        StringBuilder responseBuffer =
                            new StringBuilder(64)
                                    .append(count)
                                    .append(" ")
                                    .append(mc.getName());
                        session.writeResponse(responseBuffer.toString());
                    }
                    count++;
                }
                session.writeResponse(".");
            } else {
                int num = 0;
                try {
                    num = Integer.parseInt(argument);
                    Mail mc = (Mail) session.getUserMailbox().get(num);
                    if (mc != POP3Handler.DELETED) {
                        StringBuilder responseBuffer =
                            new StringBuilder(64)
                                    .append(POP3Handler.OK_RESPONSE)
                                    .append(" ")
                                    .append(num)
                                    .append(" ")
                                    .append(mc.getName());
                        session.writeResponse(responseBuffer.toString());
                    } else {
                        StringBuilder responseBuffer =
                            new StringBuilder(64)
                                    .append(POP3Handler.ERR_RESPONSE)
                                    .append(" Message (")
                                    .append(num)
                                    .append(") already deleted.");
                        session.writeResponse(responseBuffer.toString());
                    }
                } catch (IndexOutOfBoundsException npe) {
                    StringBuilder responseBuffer =
                        new StringBuilder(64)
                                .append(POP3Handler.ERR_RESPONSE)
                                .append(" Message (")
                                .append(num)
                                .append(") does not exist.");
                    session.writeResponse(responseBuffer.toString());
                } catch (NumberFormatException nfe) {
                    StringBuilder responseBuffer =
                        new StringBuilder(64)
                                .append(POP3Handler.ERR_RESPONSE)
                                .append(" ")
                                .append(argument)
                                .append(" is not a valid number");
                    session.writeResponse(responseBuffer.toString());
                }
            }
        } else {
            session.writeResponse(POP3Handler.ERR_RESPONSE);
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
	
	/**
     * @see org.apache.james.pop3server.CapaCapability#getImplementedCapabilities(org.apache.james.pop3server.POP3Session)
     */
	public List<String> getImplementedCapabilities(POP3Session session) {
		List<String> caps = new ArrayList<String>();
		if (session.getHandlerState() == POP3Handler.TRANSACTION) {
			caps.add(COMMAND_NAME);
			return caps;
		}
		return caps;
	}
}
