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
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.util.mail.SMTPRetCode;
import org.apache.james.util.mail.dsn.DSNStatus;

/**
  * Handles HELP command
  */
public class HelpCmdHandler implements CommandHandler {
    /**
     * The name of the command handled by the command handler
     */
    private final static String COMMAND_NAME = "HELP";


    /**
     * handles HELP command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(org.apache.james.smtpserver.SMTPSession, java.lang.String, java.lang.String) 
    **/
    public SMTPResponse onCommand(SMTPSession session, String command, String arguments) {
        return new SMTPResponse(SMTPRetCode.UNIMPLEMENTED_COMMAND, DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.SYSTEM_NOT_CAPABLE)+" " + COMMAND_NAME + " is not supported");
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
