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

import org.apache.commons.collections.ListUtils;
import org.apache.mailet.Mail;

/**
  * Handles QUIT command
  */
public class QuitCmdHandler implements CommandHandler {
	private final static String COMMAND_NAME = "QUIT";

    /**
     * @see org.apache.james.pop3server.CommandHandler#onCommand(POP3Session)
     */
    public void onCommand(POP3Session session) {
        doQUIT(session,session.getCommandArgument());
    }

    /**
     * Handler method called upon receipt of a QUIT command.
     * This method handles cleanup of the POP3Handler state.
     *
     * @param argument the first argument parsed by the parseCommand method
     */
    @SuppressWarnings("unchecked")
    private void doQUIT(POP3Session session,String argument) {
        String responseString = null;
        if (session.getHandlerState() == POP3Handler.AUTHENTICATION_READY ||  session.getHandlerState() == POP3Handler.AUTHENTICATION_USERSET) {
            responseString = POP3Handler.OK_RESPONSE + " Apache James POP3 Server signing off.";
            session.writeResponse(responseString);
            session.endSession();
            return;
        }
        List<Mail> toBeRemoved =  ListUtils.subtract(session.getBackupUserMailbox(), session.getUserMailbox());
        try {
            session.getUserInbox().remove(toBeRemoved);
            // for (Iterator it = toBeRemoved.iterator(); it.hasNext(); ) {
            //    Mail mc = (Mail) it.next();
            //    userInbox.remove(mc.getName());
            //}
            responseString = POP3Handler.OK_RESPONSE + " Apache James POP3 Server signing off.";
            session.writeResponse(responseString);
        } catch (Exception ex) {
            responseString = POP3Handler.ERR_RESPONSE + " Some deleted messages were not removed";
            session.writeResponse(responseString);
            session.getLogger().error("Some deleted messages were not removed: " + ex.getMessage());
        }
        session.endSession();
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
