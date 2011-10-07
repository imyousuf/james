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
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.handler.CommandHandler;

/**
 * Handles DELE command
 */
public class DeleCmdHandler implements CommandHandler<POP3Session> {
    private final static String COMMAND_NAME = "DELE";

    /**
     * Handler method called upon receipt of a DELE command. This command
     * deletes a particular mail message from the mailbox.
     */
    @SuppressWarnings("unchecked")
    public Response onCommand(POP3Session session, Request request) {
        POP3Response response = null;
        if (session.getHandlerState() == POP3Session.TRANSACTION) {
            int num = 0;
            try {
                num = Integer.parseInt(request.getArgument());
            } catch (Exception e) {
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Usage: DELE [mail number]");
                return response;
            }
            try {
                List<MessageMetaData> uidList = (List<MessageMetaData>) session.getState().get(POP3Session.UID_LIST);
                List<Long> deletedUidList = (List<Long>) session.getState().get(POP3Session.DELETED_UID_LIST);

                Long uid = uidList.get(num - 1).getUid();

                if (deletedUidList.contains(uid)) {
                    StringBuilder responseBuffer = new StringBuilder(64).append("Message (").append(num).append(") already deleted.");
                    response = new POP3Response(POP3Response.ERR_RESPONSE, responseBuffer.toString());
                } else {
                    deletedUidList.add(uid);
                    // we are replacing our reference with "DELETED", so we have
                    // to dispose the no-more-referenced mail object.
                    response = new POP3Response(POP3Response.OK_RESPONSE, "Message deleted");
                }
            } catch (IndexOutOfBoundsException iob) {
                StringBuilder responseBuffer = new StringBuilder(64).append("Message (").append(num).append(") does not exist.");
                response = new POP3Response(POP3Response.ERR_RESPONSE, responseBuffer.toString());
            }
        } else {
            response = new POP3Response(POP3Response.ERR_RESPONSE);
        }
        return response;
    }

    /**
     * @see org.apache.james.protocols.api.handler.CommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

}
