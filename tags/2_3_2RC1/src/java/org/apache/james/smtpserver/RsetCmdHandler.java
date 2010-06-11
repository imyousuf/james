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

package org.apache.james.smtpserver;

import org.apache.james.util.mail.dsn.DSNStatus;

/**
  * Handles RSET command
  */
public class RsetCmdHandler implements CommandHandler {
    /**
     * The name of the command handled by the command handler
     */
    private final static String COMMAND_NAME = "RSET";

    /*
     * handles RSET command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
    **/
    public void onCommand(SMTPSession session) {
        doRSET(session, session.getCommandArgument());
    }


    /**
     * Handler method called upon receipt of a RSET command.
     * Resets message-specific, but not authenticated user, state.
     *
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doRSET(SMTPSession session, String argument) {
        String responseString = "";
        if ((argument == null) || (argument.length() == 0)) {
            // remember the ehlo mode
            Object currentHeloMode = session.getState().get(SMTPSession.CURRENT_HELO_MODE);
            session.resetState();
            // start again with the old helo mode
            if (currentHeloMode != null) {
                session.getState().put(SMTPSession.CURRENT_HELO_MODE,currentHeloMode);
            }
            responseString = "250 "+DSNStatus.getStatus(DSNStatus.SUCCESS,DSNStatus.UNDEFINED_STATUS)+" OK";
        } else {
            responseString = "500 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_INVALID_ARG)+" Unexpected argument provided with RSET command";
        }
        session.writeResponse(responseString);
    }



}
