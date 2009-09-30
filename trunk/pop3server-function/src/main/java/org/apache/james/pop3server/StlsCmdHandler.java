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

package org.apache.james.pop3server;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Handler which offer STARTTLS implementation for POP3. STARTTLS is started
 * with the STSL command
 * 
 * 
 */
public class StlsCmdHandler implements CommandHandler, CapaCapability {
    public final static String COMMAND_NAME = "STLS";

    /**
     * @see org.apache.james.pop3server.CommandHandler#onCommand(org.apache.james.pop3server.POP3Session)
     */
    public void onCommand(POP3Session session) {
        // check if starttls is supported, the state is the right one and it was
        // not started before
        if (session.isStartTLSSupported() && session.getHandlerState() == POP3Handler.AUTHENTICATION_READY
                && session.isTLSStarted() == false) {
            session.writeResponse(POP3Handler.OK_RESPONSE + " Begin TLS negotiation");
            try {
                session.startTLS();
            } catch (IOException e) {
                session.getLogger().info("Error while trying to secure connection", e);

                // disconnect
                session.endSession();
            }
        } else {
            session.writeResponse(POP3Handler.ERR_RESPONSE);
        }
    }

    /**
     * @see org.apache.james.pop3server.CommandHandler#getCommands()
     */
    public List<String> getCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

    /**
     * @see org.apache.james.pop3server.CapaCapability#getImplementedCapabilities(org.apache.james.pop3server.POP3Session)
     */
    public List<String> getImplementedCapabilities(POP3Session session) {
        List<String> caps = new ArrayList<String>();
        if (session.isStartTLSSupported() && session.getHandlerState() == POP3Handler.AUTHENTICATION_READY) {
            caps.add(COMMAND_NAME);
            return caps;
        }
        return caps;
    }
}
