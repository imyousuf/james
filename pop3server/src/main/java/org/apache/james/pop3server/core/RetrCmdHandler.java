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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.james.mailbox.Content;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.MessageResult.FetchGroup;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.pop3server.POP3StreamResponse;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.handler.CommandHandler;

/**
 * Handles RETR command
 */
public class RetrCmdHandler implements CommandHandler<POP3Session> {

    private final static String COMMAND_NAME = "RETR";
    private final static FetchGroup GROUP = new FetchGroup() {

        @Override
        public int content() {
            return FULL_CONTENT;
        }

        @Override
        public Set<PartContentDescriptor> getPartContentDescriptors() {
            return null;
        }
        
    };
    /**
     * Handler method called upon receipt of a RETR command. This command
     * retrieves a particular mail message from the mailbox.
     */
    @SuppressWarnings("unchecked")
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
                List<MessageMetaData> uidList = (List<MessageMetaData>) session.getState().get(POP3Session.UID_LIST);
                List<Long> deletedUidList = (List<Long>) session.getState().get(POP3Session.DELETED_UID_LIST);

                MailboxSession mailboxSession = (MailboxSession) session.getState().get(POP3Session.MAILBOX_SESSION);
                Long uid = uidList.get(num - 1).getUid();
                if (deletedUidList.contains(uid) == false) {
                    Iterator<MessageResult> results = session.getUserMailbox().getMessages(MessageRange.one(uid), GROUP, mailboxSession);

                    if (results.hasNext()) {
                        MessageResult result = results.next();

                        Content content = result.getFullContent();                           
                        InputStream in = new CRLFTerminatedInputStream(new ExtraDotInputStream(content.getInputStream()));
                        response = new POP3StreamResponse(POP3Response.OK_RESPONSE, "Message follows", in);
                        return response;

                        

                    } else {
                        StringBuilder responseBuffer = new StringBuilder(64).append("Message (").append(num).append(") does not exist.");
                        response = new POP3Response(POP3Response.ERR_RESPONSE, responseBuffer.toString());
                    }
                } else {

                    StringBuilder responseBuffer = new StringBuilder(64).append("Message (").append(num).append(") already deleted.");
                    response = new POP3Response(POP3Response.ERR_RESPONSE, responseBuffer.toString());
                }
            } catch (IOException ioe) {
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Error while retrieving message.");
            } catch (MailboxException me) {
                response = new POP3Response(POP3Response.ERR_RESPONSE, "Error while retrieving message.");
            } catch (IndexOutOfBoundsException iob) {
                StringBuilder responseBuffer = new StringBuilder(64).append("Message (").append(num).append(") does not exist.");
                response = new POP3Response(POP3Response.ERR_RESPONSE, responseBuffer.toString());
            } catch (NoSuchElementException e) {
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
