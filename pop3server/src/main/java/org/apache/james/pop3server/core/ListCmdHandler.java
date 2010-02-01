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

import javax.mail.MessagingException;

import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.protocols.api.CommandHandler;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.mailet.Mail;

/**
  * Handles LIST command
  */
public class ListCmdHandler implements CommandHandler<POP3Session> {


    /**
     * Handler method called upon receipt of a LIST command.
     * Returns the number of messages in the mailbox and its
     * aggregate size, or optionally, the number and size of
     * a single message.
     *
     * @param argument the first argument parsed by the parseCommand method
     */

    public Response onCommand(POP3Session session, Request request) {
        POP3Response response = null;
        String parameters = request.getArgument();
        
        if (session.getHandlerState() == POP3Session.TRANSACTION) {
            Mail dm = (Mail) session.getState().get(POP3Session.DELETED);

            if (parameters == null) {

                long size = 0;
                int count = 0;

                try {

                    for (Mail mc:session.getUserMailbox()) {
                        if (mc != dm) {
                            size += mc.getMessageSize();
                            count++;
                        }
                    }
                    StringBuilder responseBuffer =
                        new StringBuilder(32)
                                .append(count)
                                .append(" ")
                                .append(size);
                    response = new POP3Response(POP3Response.OK_RESPONSE, responseBuffer.toString());
                    count = 0;
                    for (Mail mc:session.getUserMailbox()) {
                        if (mc != dm) {
                            responseBuffer =
                                new StringBuilder(16)
                                        .append(count)
                                        .append(" ")
                                        .append(mc.getMessageSize());
                            response.appendLine(responseBuffer.toString());
                        }
                        count++;
                    }
                    response.appendLine(".");
                } catch (MessagingException me) {
                    response = new POP3Response(POP3Response.ERR_RESPONSE);
                }
            } else {
                int num = 0;
                try {
                    num = Integer.parseInt(parameters);
                    Mail mc = session.getUserMailbox().get(num);
                    if (mc != dm) {
                        StringBuilder responseBuffer =
                            new StringBuilder(64)
                                    .append(num)
                                    .append(" ")
                                    .append(mc.getMessageSize());
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
                } catch (MessagingException me) {
                    response = new POP3Response(POP3Response.ERR_RESPONSE);
                }
            }
        } else {
            response = new POP3Response(POP3Response.ERR_RESPONSE);
        }
        return response;
    }

    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add("LIST");
        return commands;
    }

}
