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

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.mail.dsn.DSNStatus;
import org.apache.mailet.MailAddress;

/**
 * Handles MAIL command
 */
public class MailCmdHandler extends AbstractLogEnabled implements
    CommandHandler {

    /**
     * handles MAIL command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
     */
    public void onCommand(SMTPSession session) {
    session.getSMTPResponse().setRawSMTPResponse(
        doMAIL(session, session.getCommandArgument()));
    }

    /**
     * Handler method called upon receipt of a MAIL command.
     * Sets up handler to deliver mail as the stated sender.
     *
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    private String doMAIL(SMTPSession session, String argument) {

    StringBuffer responseBuffer = session.getResponseBuffer();
    String responseString = null;

    MailAddress sender = (MailAddress) session.getState().get(
        SMTPSession.SENDER);
    responseBuffer.append(
        "250 "
            + DSNStatus.getStatus(DSNStatus.SUCCESS,
                DSNStatus.ADDRESS_OTHER) + " Sender <").append(
        sender).append("> OK");
    responseString = session.clearResponseBuffer();
    return responseString;

    }

    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
    Collection implCommands = new ArrayList();
    implCommands.add("MAIL");

    return implCommands;
    }
}
