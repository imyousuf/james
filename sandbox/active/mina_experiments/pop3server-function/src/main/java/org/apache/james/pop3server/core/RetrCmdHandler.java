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

import org.apache.james.pop3server.CommandHandler;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.socket.BytesWrittenResetOutputStream;
import org.apache.james.util.stream.ExtraDotOutputStream;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
  * Handles RETR command
  */
public class RetrCmdHandler implements CommandHandler {

	private final static String COMMAND_NAME = "RETR";


    /**
     * Handler method called upon receipt of a RETR command.
     * This command retrieves a particular mail message from the
     * mailbox.
     *
	 * @see org.apache.james.pop3server.CommandHandler#onCommand(org.apache.james.pop3server.POP3Session, java.lang.String, java.lang.String)
	 */
    public POP3Response onCommand(POP3Session session, String command, String parameters) {
        POP3Response response = null;
        if (session.getHandlerState() == POP3Session.TRANSACTION) {
            int num = 0;
            try {
                num = Integer.parseInt(parameters.trim());
            } catch (Exception e) {
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Usage: RETR [mail number]");
                return response;
            }
            try {
                Mail mc = session.getUserMailbox().get(num);
                Mail dm = (Mail) session.getState().get(POP3Session.DELETED);

                if (mc != dm) {
                    response = new POP3Response(POP3Response.OK_RESPONSE, "Message follows");
                    session.writePOP3Response(response);
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
                                .append("Message (")
                                .append(num)
                                .append(") already deleted.");
                    response = new POP3Response(POP3Response.ERR_RESPONSE,responseBuffer.toString());
                }
            } catch (IOException ioe) {
                response = new POP3Response(POP3Response.ERR_RESPONSE,"Error while retrieving message.");
            } catch (MessagingException me) {
                response = new POP3Response(POP3Response.ERR_RESPONSE,"Error while retrieving message.");
            } catch (IndexOutOfBoundsException iob) {
                StringBuilder responseBuffer =
                    new StringBuilder(64)
                            .append("Message (")
                            .append(num)
                            .append(") does not exist.");
                response = new POP3Response(POP3Response.ERR_RESPONSE,responseBuffer.toString());
            }
        } else {
            response = new POP3Response(POP3Response.ERR_RESPONSE);
        }
        return response;
    }


    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

}
