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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.mailet.Mail;

/**
  * Handles TOP command
  */
public class TopCmdHandler extends RetrCmdHandler implements CapaCapability {
	private final static String COMMAND_NAME = "TOP";


	/**
     * Handler method called upon receipt of a TOP command.
     * This command retrieves the top N lines of a specified
     * message in the mailbox.
     *
     * The expected command format is
     *  TOP [mail message number] [number of lines to return]
     *
	 */
    @SuppressWarnings("unchecked")
    @Override
    public Response onCommand(POP3Session session, Request request) {
        POP3Response response = null;
        String parameters = request.getArgument();
        if (parameters == null) {
            response = new POP3Response(POP3Response.ERR_RESPONSE, "Usage: TOP [mail number] [Line number]");
            return response;
        }
        
        String argument = "";
        String argument1 = "";
        int pos = parameters.indexOf(" ");
        if (pos > 0) {
            argument = parameters.substring(0,pos);
            argument1 = parameters.substring(pos+1);
        }

        if (session.getHandlerState() == POP3Session.TRANSACTION) {
            int num = 0;
            int lines = 0;
            try {
                num = Integer.parseInt(argument);
                lines = Integer.parseInt(argument1);
            } catch (NumberFormatException nfe) {
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Usage: TOP [mail number] [Line number]");
                return response;
            }
            try {
                Mail mc = session.getUserMailbox().get(num);
                Mail dm = (Mail) session.getState().get(POP3Session.DELETED);

                if (mc != dm) {
                    response = new POP3Response(POP3Response.OK_RESPONSE, "Message follows");
                    try {
                        for (Enumeration e = mc.getMessage().getAllHeaderLines(); e.hasMoreElements(); ) {
                        	response.appendLine(e.nextElement().toString());
                        }

                       	writeMessageContentTo(mc, response, lines);
                       	
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
                    response = new POP3Response(POP3Response.ERR_RESPONSE, responseBuffer.toString());
                }
            } catch (IOException ioe) {
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Error while retrieving message.");
            } catch (MessagingException me) {
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Error while retrieving message.");
            } catch (IndexOutOfBoundsException iob) {
                StringBuilder exceptionBuffer =
                    new StringBuilder(64)
                            .append("Message (")
                            .append(num)
                            .append(") does not exist.");
                response = new POP3Response(POP3Response.ERR_RESPONSE, exceptionBuffer.toString());
            }
        } else {
            response = new POP3Response(POP3Response.ERR_RESPONSE);
        }
        return response;    }


   
    

   /**
     * @see org.apache.james.pop3server.core.CapaCapability#getImplementedCapabilities(org.apache.james.pop3server.POP3Session)
     */
	public List<String> getImplementedCapabilities(POP3Session session) {
		List<String> caps = new ArrayList<String>();
		if (session.getHandlerState() == POP3Session.TRANSACTION) {
			caps.add(COMMAND_NAME);
			return caps;
		}
		return caps;
	}

	/**
	 * @see org.apache.james.api.protocol.CommonCommandHandler#getImplCommands()
	 */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

}
