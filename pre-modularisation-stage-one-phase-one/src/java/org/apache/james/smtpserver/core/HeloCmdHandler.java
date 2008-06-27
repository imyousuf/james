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


/**
  * Handles HELO command
  */
public class HeloCmdHandler extends AbstractLogEnabled implements CommandHandler {

    /**
     * The name of the command handled by the command handler
     */
    private final static String COMMAND_NAME = "HELO";   
      
    /**
     * process HELO command
     *
     * @see org.apache.james.smtpserver.CommandHandler#onCommand(SMTPSession)
    **/
    public void onCommand(SMTPSession session) {
        doHELO(session, session.getCommandArgument());
    }

    /**
     * @param session SMTP session object
     * @param argument the argument passed in with the command by the SMTP client
     */
    private void doHELO(SMTPSession session, String argument) {
        String responseString = null;

        session.getConnectionState().put(SMTPSession.CURRENT_HELO_MODE, COMMAND_NAME);
        session.getResponseBuffer().append("250 ").append(
                session.getConfigurationData().getHelloName())
                .append(" Hello ").append(argument).append(" (").append(
                        session.getRemoteHost()).append(" [").append(
                        session.getRemoteIPAddress()).append("])");
        responseString = session.clearResponseBuffer();
        session.writeResponse(responseString);
    }
    
    /**
     * @see org.apache.james.smtpserver.CommandHandler#getImplCommands()
     */
    public Collection getImplCommands() {
        Collection implCommands = new ArrayList();
        implCommands.add("HELO");
        
        return implCommands;
    } 
}
