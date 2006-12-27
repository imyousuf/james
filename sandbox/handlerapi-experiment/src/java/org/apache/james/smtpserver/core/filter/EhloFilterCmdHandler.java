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



package org.apache.james.smtpserver.core.filter;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.mail.SMTPRetCode;
import org.apache.james.util.mail.dsn.DSNStatus;

/**
  * Handles EHLO command
  */
public class EhloFilterCmdHandler extends AbstractLogEnabled implements CommandHandler {

    /**
     * The name of the command handled by the command handler
     */
    private final static String COMMAND_NAME = "EHLO";

    /**
     * processes EHLO command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(org.apache.james.smtpserver.SMTPSession, java.lang.String, java.lang.String) 
    **/
    public SMTPResponse onCommand(SMTPSession session, String command, String arguments) {
        return doEHLO(session, arguments);
    }

    /**
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    private SMTPResponse doEHLO(SMTPSession session, String argument) {
        session.resetState();
        
        if (argument == null) {
            return new SMTPResponse(SMTPRetCode.SYNTAX_ERROR_ARGUMENTS, DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.DELIVERY_INVALID_ARG)+" Domain address required: " + COMMAND_NAME);
        } else {
            // store provided name
            session.getState().put(SMTPSession.CURRENT_HELO_NAME,argument);
            return null;
        }
    }
    
    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
        Collection implCommands = new ArrayList();
        implCommands.add(COMMAND_NAME);
        
        return implCommands;
    }

}
