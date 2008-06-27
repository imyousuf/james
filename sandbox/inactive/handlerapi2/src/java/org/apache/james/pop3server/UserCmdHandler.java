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

/**
  * Handles NOOP command
  */
public class UserCmdHandler implements CommandHandler {

    /**
     * @see org.apache.james.pop3server.CommandHandler#onCommand(POP3Session)
     */
    public void onCommand(POP3Session session) {
        doUSER(session,session.getCommandArgument());
    }

    /**
     * Handler method called upon receipt of a USER command.
     * Reads in the user id.
     *
     * @param command the command parsed by the parseCommand method
     * @param argument the first argument parsed by the parseCommand method
     */
    private void doUSER(POP3Session session,String argument) {
        String responseString = null;
        if (session.getHandlerState() == POP3Handler.AUTHENTICATION_READY && argument != null) {
            session.setUser(argument);
            session.setHandlerState(POP3Handler.AUTHENTICATION_USERSET);
            responseString = POP3Handler.OK_RESPONSE;
        } else {
            responseString = POP3Handler.ERR_RESPONSE;
        }
        session.writeResponse(responseString);
    }


}
