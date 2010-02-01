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

import org.apache.james.api.protocol.CommandHandler;
import org.apache.james.api.protocol.Request;
import org.apache.james.api.protocol.Response;
import org.apache.james.dsn.DSNStatus;
import org.apache.james.smtpserver.protocol.SMTPResponse;
import org.apache.james.smtpserver.protocol.SMTPRetCode;
import org.apache.james.smtpserver.protocol.SMTPSession;

/**
  * Handles HELP command
  */
public class HelpCmdHandler implements CommandHandler<SMTPSession> {
    /**
     * The name of the command handled by the command handler
     */
    private final static String COMMAND_NAME = "HELP";


    /**
     * handles HELP command
     *
    **/
    public Response onCommand(SMTPSession session, Request request){
        return new SMTPResponse(SMTPRetCode.UNIMPLEMENTED_COMMAND, DSNStatus.getStatus(DSNStatus.PERMANENT,DSNStatus.SYSTEM_NOT_CAPABLE)+" " + COMMAND_NAME + " is not supported");
    }

    /**
     * @see org.apache.james.smtpserver.protocol.CommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        Collection<String> implCommands = new ArrayList<String>();
        implCommands.add(COMMAND_NAME);
        
        return implCommands;
    }
}
