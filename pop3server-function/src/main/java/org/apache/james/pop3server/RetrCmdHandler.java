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

import org.apache.james.socket.BytesWrittenResetOutputStream;
import org.apache.james.util.stream.ExtraDotOutputStream;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
  * Handles RETR command
  */
public class RetrCmdHandler implements CommandHandler {

	private final static String COMMAND_NAME = "RETR";

    /**
     * @see org.apache.james.pop3server.CommandHandler#onCommand(POP3Session)
     */
    public void onCommand(POP3Session session) {
        doRETR(session,session.getCommandArgument());
    }

    /**
     * Handler method called upon receipt of a RETR command.
     * This command retrieves a particular mail message from the
     * mailbox.
     *
     * @param argument the first argument parsed by the parseCommand method
     */
    private void doRETR(POP3Session session,String argument) {
        String responseString = null;
        if (session.getHandlerState() == POP3Handler.TRANSACTION) {
            int num = 0;
            try {
                num = Integer.parseInt(argument.trim());
            } catch (Exception e) {
                responseString = POP3Handler.ERR_RESPONSE + " Usage: RETR [mail number]";
                session.writeResponse(responseString);
                return;
            }
            try {
                Mail mc = session.getUserMailbox().get(num);
                if (mc != POP3Handler.DELETED) {
                    responseString = POP3Handler.OK_RESPONSE + " Message follows";
                    session.writeResponse(responseString);
                    try {
                        ExtraDotOutputStream edouts =
                                new ExtraDotOutputStream(session.getOutputStream());
                        OutputStream nouts = new BytesWrittenResetOutputStream(edouts,
                                                                  session.getWatchdog(),
                                                                  session.getConfigurationData().getResetLength());
                        mc.getMessage().writeTo(nouts);
                        nouts.flush();
                        edouts.checkCRLFTerminator();
                        edouts.flush();
                    } finally {
                        session.writeResponse(".");
                    }
                } else {
                    StringBuilder responseBuffer =
                        new StringBuilder(64)
                                .append(POP3Handler.ERR_RESPONSE)
                                .append(" Message (")
                                .append(num)
                                .append(") already deleted.");
                    responseString = responseBuffer.toString();
                    session.writeResponse(responseString);
                }
            } catch (IOException ioe) {
                responseString = POP3Handler.ERR_RESPONSE + " Error while retrieving message.";
                session.writeResponse(responseString);
            } catch (MessagingException me) {
                responseString = POP3Handler.ERR_RESPONSE + " Error while retrieving message.";
                session.writeResponse(responseString);
            } catch (IndexOutOfBoundsException iob) {
                StringBuilder responseBuffer =
                    new StringBuilder(64)
                            .append(POP3Handler.ERR_RESPONSE)
                            .append(" Message (")
                            .append(num)
                            .append(") does not exist.");
                responseString = responseBuffer.toString();
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
