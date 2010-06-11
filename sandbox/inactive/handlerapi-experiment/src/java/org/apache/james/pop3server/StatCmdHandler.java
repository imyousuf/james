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

import org.apache.mailet.Mail;

import javax.mail.MessagingException;

import java.util.Iterator;

/**
  * Handles STAT command
  */
public class StatCmdHandler implements CommandHandler {

    /**
     * @see org.apache.james.pop3server.CommandHandler#onCommand(POP3Session)
     */
    public void onCommand(POP3Session session) {
        doSTAT(session,session.getCommandArgument());
    }

    /**
     * Handler method called upon receipt of a STAT command.
     * Returns the number of messages in the mailbox and its
     * aggregate size.
     *
     * @param argument the first argument parsed by the parseCommand method
     */
    private void doSTAT(POP3Session session,String argument) {
        String responseString = null;
        if (session.getHandlerState() == POP3Handler.TRANSACTION) {
            long size = 0;
            int count = 0;
            try {
                for (Iterator i = session.getUserMailbox().iterator(); i.hasNext(); ) {
                    Mail mc = (Mail) i.next();
                    if (mc != POP3Handler.DELETED) {
                        size += mc.getMessageSize();
                        count++;
                    }
                }
                StringBuffer responseBuffer =
                    new StringBuffer(32)
                            .append(POP3Handler.OK_RESPONSE)
                            .append(" ")
                            .append(count)
                            .append(" ")
                            .append(size);
                responseString = responseBuffer.toString();
                session.writeResponse(responseString);
            } catch (MessagingException me) {
                responseString = POP3Handler.ERR_RESPONSE;
                session.writeResponse(responseString);
            }
        } else {
            responseString = POP3Handler.ERR_RESPONSE;
            session.writeResponse(responseString);
        }
    }


}
