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

import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.smtpserver.hook.HeloHook;
import org.apache.james.util.mail.SMTPRetCode;
import org.apache.james.util.mail.dsn.DSNStatus;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Handles HELO command
 */
public class HeloCmdHandler extends AbstractHookableCmdHandler implements
        CommandHandler {

    /**
     * The name of the command handled by the command handler
     */
    private final static String COMMAND_NAME = "HELO";

    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
        Collection implCommands = new ArrayList();
        implCommands.add(COMMAND_NAME);

        return implCommands;
    }

    /**
     * @see org.apache.james.smtpserver.core.AbstractHookableCmdHandler#doCoreCmd(org.apache.james.smtpserver.SMTPSession,
     *      java.lang.String, java.lang.String)
     */
    protected SMTPResponse doCoreCmd(SMTPSession session, String command,
            String parameters) {
        session.getConnectionState().put(SMTPSession.CURRENT_HELO_MODE,
                COMMAND_NAME);
        StringBuffer response = new StringBuffer();
        response.append(session.getConfigurationData().getHelloName()).append(
                " Hello ").append(parameters).append(" (").append(
                session.getRemoteHost()).append(" [").append(
                session.getRemoteIPAddress()).append("])");
        return new SMTPResponse(SMTPRetCode.MAIL_OK, response);
    }

    /**
     * @see org.apache.james.smtpserver.core.AbstractHookableCmdHandler#doFilterChecks(org.apache.james.smtpserver.SMTPSession,
     *      java.lang.String, java.lang.String)
     */
    protected SMTPResponse doFilterChecks(SMTPSession session, String command,
            String parameters) {
        session.resetState();

        if (parameters == null) {
            return new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_ARGUMENTS,
                    DSNStatus.getStatus(DSNStatus.PERMANENT,
                            DSNStatus.DELIVERY_INVALID_ARG)
                            + " Domain address required: " + COMMAND_NAME);
        } else {
            // store provided name
            session.getState().put(SMTPSession.CURRENT_HELO_NAME, parameters);
            return null;
        }
    }

    /**
     * @see org.apache.james.smtpserver.core.AbstractHookableCmdHandler#getHookInterface()
     */
    protected Class getHookInterface() {
        return HeloHook.class;
    }

    /**
     * @see org.apache.james.smtpserver.core.AbstractHookableCmdHandler#callHook(java.lang.Object, org.apache.james.smtpserver.SMTPSession, java.lang.String)
     */
    protected SMTPResponse callHook(Object rawHook, SMTPSession session, String parameters) {
        return calcDefaultSMTPResponse(((HeloHook) rawHook).doHelo(session, parameters));
    }


}
