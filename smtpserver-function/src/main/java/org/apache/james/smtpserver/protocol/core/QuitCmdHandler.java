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

package org.apache.james.smtpserver.protocol.core;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.protocol.SMTPResponse;
import org.apache.james.smtpserver.protocol.SMTPRetCode;
import org.apache.james.smtpserver.protocol.SMTPSession;
import org.apache.james.smtpserver.protocol.hook.HookResult;
import org.apache.james.smtpserver.protocol.hook.QuitHook;

/**
 * Handles QUIT command
 */
public class QuitCmdHandler extends AbstractHookableCmdHandler<QuitHook> {

    /**
     * The name of the command handled by the command handler
     */
    private final static String COMMAND_NAME = "QUIT";

    /**
     * Handler method called upon receipt of a QUIT command. This method informs
     * the client that the connection is closing.
     * 
     * @param session
     *            SMTP session object
     * @param argument
     *            the argument passed in with the command by the SMTP client
     */
    private SMTPResponse doQUIT(SMTPSession session, String argument) {
        SMTPResponse ret;
        if ((argument == null) || (argument.length() == 0)) {
            StringBuilder response = new StringBuilder();
            response.append(
                    DSNStatus.getStatus(DSNStatus.SUCCESS,
                            DSNStatus.UNDEFINED_STATUS)).append(" ").append(
                    session.getHelloName()).append(
                    " Service closing transmission channel");
            ret = new SMTPResponse(SMTPRetCode.SYSTEM_QUIT, response);
        } else {
            ret = new SMTPResponse(
                    SMTPRetCode.SYNTAX_ERROR_COMMAND_UNRECOGNIZED, DSNStatus
                            .getStatus(DSNStatus.PERMANENT,
                                    DSNStatus.DELIVERY_INVALID_ARG)
                            + " Unexpected argument provided with QUIT command");
        }
        ret.setEndSession(true);
        return ret;
    }

    /**
     * @see org.apache.james.smtpserver.protocol.CommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        Collection<String> implCommands = new ArrayList<String>();
        implCommands.add(COMMAND_NAME);

        return implCommands;
    }

    /**
     * @see org.apache.james.smtpserver.protocol.core.AbstractHookableCmdHandler#doCoreCmd(org.apache.james.smtpserver.protocol.SMTPSession,
     *      java.lang.String, java.lang.String)
     */
    protected SMTPResponse doCoreCmd(SMTPSession session, String command,
            String parameters) {
        return doQUIT(session, parameters);
    }

    /**
     * @see org.apache.james.smtpserver.protocol.core.AbstractHookableCmdHandler#doFilterChecks(org.apache.james.smtpserver.protocol.SMTPSession,
     *      java.lang.String, java.lang.String)
     */
    protected SMTPResponse doFilterChecks(SMTPSession session, String command,
            String parameters) {
        return null;
    }

    /**
     * @see org.apache.james.smtpserver.protocol.core.AbstractHookableCmdHandler#getHookInterface()
     */
    protected Class<QuitHook> getHookInterface() {
        return QuitHook.class;
    }

    /**
     * @see org.apache.james.smtpserver.protocol.core.AbstractHookableCmdHandler#callHook(java.lang.Object, org.apache.james.smtpserver.protocol.SMTPSession, java.lang.String)
     */
    protected HookResult callHook(QuitHook rawHook, SMTPSession session, String parameters) {
        return rawHook.doQuit(session);
    }

}
