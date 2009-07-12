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
  * Handles QUIT command
  */
public class QuitCmdHandler implements CommandHandler {

    /**
     * The name of the command handled by the command handler
     */
    private final static String COMMAND_NAME = "QUIT";

    /**
     * handles QUIT command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public void onCommand(SMTPSession session) {
        doQUIT(session, session.getCommandArgument());

    }


    /**
     * Handler method called upon receipt of a QUIT command.
     * This method informs the client that the connection is
     * closing.
     *
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doQUIT(SMTPSession session, String argument) {

        String responseString = "";
        if ((argument == null) || (argument.length() == 0)) {
            session.getResponseBuffer().append("221 "+DSNStatus.getStatus(DSNStatus.SUCCESS,DSNStatus.UNDEFINED_STATUS)+" ")
                          .append(session.getConfigurationData().getHelloName())
                          .append(" Service closing transmission channel");
            responseString = session.clearResponseBuffer();
        } else {
            responseString = "500 "+DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_INVALID_ARG)+" Unexpected argument provided with QUIT command";
        }
        session.writeResponse(responseString);
        session.endSession();
    }


}



