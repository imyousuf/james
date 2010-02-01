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

import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.protocols.api.CommandHandler;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.mailet.Mail;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
  * Handles RETR command
  */
public class RetrCmdHandler implements CommandHandler<POP3Session> {

	private final static String COMMAND_NAME = "RETR";


    /**
     * Handler method called upon receipt of a RETR command.
     * This command retrieves a particular mail message from the
     * mailbox.
     *
	 */
    public Response onCommand(POP3Session session, Request request) {
        POP3Response response = null;
        String parameters = request.getArgument();
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
                    try {
                    	
                    	// write the full mail to the client
                        writeMessageContentTo(mc, response, -1);
                        
                    } finally {
                    	response.appendLine(".");
                      
                    }
                    return response;
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


    /**
     * Writes the content of the Mail to the client
     *
     * @param mail the Mail to write
     * @param lines the number of lines to write to the client. If -1 is given it write every line of the given MimeMessage to the client
     * @param session the POP3Session to use
     *
     * @throws MessagingException if the MimeMessage is not set for this Mail
     * @throws IOException if an error occurs while reading or writing from the stream
     */
	protected void writeMessageContentTo(Mail mail,
			POP3Response response, int lines)
			throws IOException, MessagingException {
		String line;
		BufferedReader br;
		MimeMessage message = mail.getMessage();

		if (message != null) {
			br = new BufferedReader(new InputStreamReader(message
					.getRawInputStream()));
			try {

				while (lines == -1 || lines > 0) {
					if ((line = br.readLine()) == null) {
						break;
					}

					// add extra dot if line starts with dot
					if (line.startsWith(".")) {
						line = "." + line;
					}
					response.appendLine(line);

					lines--;

				}

			} finally {
				br.close();
			}
		} else {
			throw new MessagingException("No message set for this Mail!");
		}
	}
	
	/*
	 * (non-Javadoc)
	 * @see org.apache.james.api.protocol.CommonCommandHandler#getImplCommands()
	 */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

}
