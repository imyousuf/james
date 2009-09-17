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

import javax.mail.MessagingException;

import org.apache.mailet.Mail;

/**
  * Handles LIST command
  */
public class ListCmdHandler implements CommandHandler {

    /**
     * @see org.apache.james.pop3server.CommandHandler#onCommand(POP3Session)
     */
    public void onCommand(POP3Session session) {
        doLIST(session,session.getCommandArgument());
    }

    /**
     * Handler method called upon receipt of a LIST command.
     * Returns the number of messages in the mailbox and its
     * aggregate size, or optionally, the number and size of
     * a single message.
     *
     * @param argument the first argument parsed by the parseCommand method
     */
    private void doLIST(POP3Session session,String argument) {
        if (session.getHandlerState() == POP3Handler.TRANSACTION) {
            if (argument == null) {
                long size = 0;
                int count = 0;
                try {
                    for (Mail mc:session.getUserMailbox()) {
                        if (mc != POP3Handler.DELETED) {
                            size += mc.getMessageSize();
                            count++;
                        }
                    }
                    StringBuilder responseBuffer =
                        new StringBuilder(32)
                                .append(POP3Handler.OK_RESPONSE)
                                .append(" ")
                                .append(count)
                                .append(" ")
                                .append(size);
                    session.writeResponse(responseBuffer.toString());
                    count = 0;
                    for (Mail mc:session.getUserMailbox()) {
                        if (mc != POP3Handler.DELETED) {
                            responseBuffer =
                                new StringBuilder(16)
                                        .append(count)
                                        .append(" ")
                                        .append(mc.getMessageSize());
                            session.writeResponse(responseBuffer.toString());
                        }
                        count++;
                    }
                    session.writeResponse(".");
                } catch (MessagingException me) {
                    session.writeResponse(POP3Handler.ERR_RESPONSE);
                }
            } else {
                int num = 0;
                try {
                    num = Integer.parseInt(argument);
                    Mail mc = session.getUserMailbox().get(num);
                    if (mc != POP3Handler.DELETED) {
                        StringBuilder responseBuffer =
                            new StringBuilder(64)
                                    .append(POP3Handler.OK_RESPONSE)
                                    .append(" ")
                                    .append(num)
                                    .append(" ")
                                    .append(mc.getMessageSize());
                        session.writeResponse(responseBuffer.toString());
                    } else {
                        StringBuilder responseBuffer =
                            new StringBuilder(64)
                                    .append(POP3Handler.ERR_RESPONSE)
                                    .append(" Message (")
                                    .append(num)
                                    .append(") already deleted.");
                        session.writeResponse(responseBuffer.toString());
                    }
                } catch (IndexOutOfBoundsException npe) {
                    StringBuilder responseBuffer =
                        new StringBuilder(64)
                                .append(POP3Handler.ERR_RESPONSE)
                                .append(" Message (")
                                .append(num)
                                .append(") does not exist.");
                    session.writeResponse(responseBuffer.toString());
                } catch (NumberFormatException nfe) {
                    StringBuilder responseBuffer =
                        new StringBuilder(64)
                                .append(POP3Handler.ERR_RESPONSE)
                                .append(" ")
                                .append(argument)
                                .append(" is not a valid number");
                    session.writeResponse(responseBuffer.toString());
                } catch (MessagingException me) {
                    session.writeResponse(POP3Handler.ERR_RESPONSE);
                }
            }
        } else {
            session.writeResponse(POP3Handler.ERR_RESPONSE);
        }
    }

}
