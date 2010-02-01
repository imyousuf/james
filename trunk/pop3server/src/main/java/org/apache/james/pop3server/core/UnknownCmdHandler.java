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



package org.apache.james.pop3server.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.protocols.api.CommandHandler;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;


/**
  * Default command handler for handling unknown commands
  */
public class UnknownCmdHandler implements CommandHandler<POP3Session> {
    /**
     * The name of the command handled by the command handler
     */
    public static final String COMMAND_NAME = "UNKNOWN";


    /**
     * Handler method called upon receipt of an unrecognized command.
     * Returns an error response and logs the command.    
     *
     */
    public Response onCommand(POP3Session session, Request request) {
        return new POP3Response(POP3Response.ERR_RESPONSE);
    }


	/**
	 * @see org.apache.james.api.protocol.CommonCommandHandler#getImplCommands()
	 */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

}
