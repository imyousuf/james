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

import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.protocols.api.CommandHandler;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.mailet.Mail;

/**
  * Handles UIDL command
  */
public class UidlCmdHandler implements CommandHandler<POP3Session>, CapaCapability {
	private final static String COMMAND_NAME = "UIDL";

	/**
     * Handler method called upon receipt of a UIDL command.
     * Returns a listing of message ids to the client.
     *
	 */
    public Response onCommand(POP3Session session, Request request) {
        POP3Response response = null;
        String parameters = request.getArgument();
        if (session.getHandlerState() == POP3Session.TRANSACTION) {
            Mail dm = (Mail) session.getState().get(POP3Session.DELETED);

            if (parameters == null) {
                response = new POP3Response(POP3Response.OK_RESPONSE,"unique-id listing follows");
                int count = 0;
                for (Mail mc:session.getUserMailbox()) {
                    if (mc != dm) {
                        StringBuilder responseBuffer =
                            new StringBuilder(64)
                                    .append(count)
                                    .append(" ")
                                    .append(mc.getName());
                        response.appendLine(responseBuffer.toString());
                    }
                    count++;
                }
                response.appendLine(".");
            } else {
                int num = 0;
                try {
                    num = Integer.parseInt(parameters);
                    Mail mc = (Mail) session.getUserMailbox().get(num);
                    if (mc != dm) {
                        StringBuilder responseBuffer =
                            new StringBuilder(64)
                                    .append(num)
                                    .append(" ")
                                    .append(mc.getName());
                        response = new POP3Response(POP3Response.OK_RESPONSE, responseBuffer.toString());

                    } else {
                        StringBuilder responseBuffer =
                            new StringBuilder(64)
                                    .append("Message (")
                                    .append(num)
                                    .append(") already deleted.");
                        response = new POP3Response(POP3Response.ERR_RESPONSE, responseBuffer.toString());
                    }
                } catch (IndexOutOfBoundsException npe) {
                    StringBuilder responseBuffer =
                        new StringBuilder(64)
                                .append("Message (")
                                .append(num)
                                .append(") does not exist.");
                    response = new POP3Response(POP3Response.ERR_RESPONSE, responseBuffer.toString());
                } catch (NumberFormatException nfe) {
                    StringBuilder responseBuffer =
                        new StringBuilder(64)
                                .append(parameters)
                                .append(" is not a valid number");
                    response = new POP3Response(POP3Response.ERR_RESPONSE, responseBuffer.toString());
                }
            }
        } else {
            response = new POP3Response(POP3Response.ERR_RESPONSE);
        }
        return response;
    }

    
	
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
	 * (non-Javadoc)
	 * @see org.apache.james.api.protocol.CommonCommandHandler#getImplCommands()
	 */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }
}
