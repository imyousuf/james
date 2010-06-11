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

package org.apache.james.smtpserver.core;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.mail.dsn.DSNStatus;

/**
 * Default command handler for handling unknown commands
 */
public class UnknownCmdHandler implements CommandHandler {

    /**
     * The name of the command handled by the command handler
     */
    public static final String UNKNOWN_COMMAND = "UNKNOWN";

    /**
     * Handler method called upon receipt of an unrecognized command.
     * Returns an error response and logs the command.
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     **/
    public void onCommand(SMTPSession session) {
    String response = doUNKNOWN(session);

    if (response != null) {
        session.getSMTPResponse().setRawSMTPResponse(response);
    }
    }

    private String doUNKNOWN(SMTPSession session) {

    //If there was message failure, don't consider it as an unknown command
    if (session.getState().get(SMTPSession.MESG_FAILED) != null) {
        return null;
    }

    session.getResponseBuffer().append(
        "500 "
            + DSNStatus.getStatus(DSNStatus.PERMANENT,
                DSNStatus.DELIVERY_INVALID_CMD)).append(
        " Command ").append(session.getCommandName()).append(
        " unrecognized.");
    String responseString = session.clearResponseBuffer();

    return responseString;
    }

    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
    Collection implCommands = new ArrayList();
    implCommands.add("UNKNOWN");

    return implCommands;
    }
}
