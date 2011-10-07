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
import org.apache.james.pop3server.StartTlsPop3Response;
import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.james.protocols.api.handler.CommandHandler;

/**
 * Handler which offer STARTTLS implementation for POP3. STARTTLS is started
 * with the STSL command
 */
public class StlsCmdHandler implements CommandHandler<POP3Session>, CapaCapability {
    public final static String COMMAND_NAME = "STLS";

    /**
     * @see CommandHandler#onCommand(org.apache.james.protocols.api.ProtocolSession, Request)
     */
    public Response onCommand(POP3Session session, Request request) {
        POP3Response response;
        // check if starttls is supported, the state is the right one and it was
        // not started before
        if (session.isStartTLSSupported() && session.getHandlerState() == POP3Session.AUTHENTICATION_READY && session.isTLSStarted() == false) {
            response = new StartTlsPop3Response(POP3Response.OK_RESPONSE, "Begin TLS negotiation");
            return response;

        } else {
            response = new POP3Response(POP3Response.ERR_RESPONSE);
            return response;
        }
    }

    /**
     * @see org.apache.james.pop3server.core.CapaCapability#getImplementedCapabilities(org.apache.james.pop3server.POP3Session)
     */
    public List<String> getImplementedCapabilities(POP3Session session) {
        List<String> caps = new ArrayList<String>();
        if (session.isStartTLSSupported() && session.getHandlerState() == POP3Session.AUTHENTICATION_READY) {
            caps.add(COMMAND_NAME);
            return caps;
        }
        return caps;
    }

    /**
     * @see org.apache.james.protocols.api.handler.CommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }
}
