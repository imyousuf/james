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
import java.util.Iterator;
import java.util.List;

import javax.mail.MessagingException;

import org.apache.james.imap.mailbox.MailboxSession;
import org.apache.james.imap.mailbox.MessageRange;
import org.apache.james.imap.mailbox.MessageResult;
import org.apache.james.imap.mailbox.MessageResult.FetchGroup;
import org.apache.james.imap.mailbox.util.FetchGroupImpl;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.protocols.api.CommandHandler;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;

/**
  * Handles STAT command
  */
public class StatCmdHandler implements CommandHandler<POP3Session> {
	private final static String COMMAND_NAME = "STAT";

	/**
     * Handler method called upon receipt of a STAT command.
     * Returns the number of messages in the mailbox and its
     * aggregate size.
     *
	 */
    public Response onCommand(POP3Session session, Request request) {
        POP3Response response = null;
        if (session.getHandlerState() == POP3Session.TRANSACTION) {
  
            try {
            	List<Long> uidList = (List<Long>) session.getState().get(POP3Session.UID_LIST);
                List<Long> deletedUidList = (List<Long>) session.getState().get(POP3Session.DELETED_UID_LIST);
                long size = 0;
                int count = 0;
                if (uidList.isEmpty() == false) {
                    MailboxSession mailboxSession = (MailboxSession) session.getState().get(POP3Session.MAILBOX_SESSION);
                    Iterator<MessageResult> results =  session.getUserMailbox().getMessages(MessageRange.range(uidList.get(0), uidList.get(uidList.size() -1)), new FetchGroupImpl(FetchGroup.MINIMAL), mailboxSession);


                    List<MessageResult> validResults = new ArrayList<MessageResult>();
                    while (results.hasNext()) {
                        MessageResult result = results.next();
                        if (deletedUidList.contains(result.getUid()) == false) {
                            size += result.getSize();
                            count++;
                            validResults.add(result);
                        }
                    }
                }
                StringBuilder responseBuffer =
                    new StringBuilder(32)
                            .append(count)
                            .append(" ")
                            .append(size);
                response = new POP3Response(POP3Response.OK_RESPONSE,responseBuffer.toString());
            } catch (MessagingException me) {
                response = new POP3Response(POP3Response.ERR_RESPONSE);
            }
        } else {
            response = new POP3Response(POP3Response.ERR_RESPONSE);
        }
        return response;
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
