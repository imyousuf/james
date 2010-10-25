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

package org.apache.james.remotemanager.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;

public class ListMappingCmdHandler extends AbstractMappingCmdHandler {
    private CommandHelp help = new CommandHelp("listmapping [user@domain]","list all mappings for the given emailaddress");

    public final static String COMMAND_NAME = "LISTMAPPING";

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.CommandHandler#onCommand(org.apache.james.protocols.api.ProtocolSession, org.apache.james.protocols.api.Request)
     */
    public Response onCommand(RemoteManagerSession session, Request request) {
        RemoteManagerResponse response;
        String parameters = request.getArgument();
        String[] args = null;
        String user = null;
        String domain = null;

        if (parameters != null)
            args = parameters.split(" ");

        // check if the command was called correct
        if (parameters == null || parameters.trim().equals("") || args.length  != 1) {
            response = new RemoteManagerResponse("Usage: " + help.getSyntax());
            return response;
        } else {
            
			if (args[0].indexOf("@") > 0) {
				user = getMappingValue(args[0].split("@")[0]);
				domain = getMappingValue(args[0].split("@")[1]);
			} else {
				response = new RemoteManagerResponse("Usage: "
						+ help.getSyntax());
				return response;

			}

            try {
                Collection<String> mappings = vutManagement.getUserDomainMappings(user, domain);
                if (mappings == null) {
                    response = new RemoteManagerResponse("No mappings found");
                } else {
                    response = new RemoteManagerResponse("Mappings:");

                    Iterator<String> m = mappings.iterator();
                    while (m.hasNext()) {
                        response.appendLine(m.next());
                     }
                }
            

            } catch (IllegalArgumentException e) {
                session.getLogger().error("Error on listing mapping: " + e);
                response = new RemoteManagerResponse("Error on listing mapping: " + e);
            }
        }

        return response;

    }

    /**
     * @see org.apache.james.api.protocol.CommonCommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

    /**
     * @see org.apache.james.remotemanager.CommandHandler#getHelp()
     */
    public CommandHelp getHelp() {
        return help;
    }

}
