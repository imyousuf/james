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
import java.util.Map;

import javax.annotation.Resource;

import org.apache.james.api.vut.management.VirtualUserTableManagementException;
import org.apache.james.api.vut.management.VirtualUserTableManagementService;
import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;

public class ListAllMappingsCmdHandler implements CommandHandler {
    private CommandHelp help = new CommandHelp("listallmappings ([table=virtualusertablename])","list all mappings");

    public final static String COMMAND_NAME = "LISTALLMAPPINGS";

    protected VirtualUserTableManagementService vutManagement;

    @Resource(name = "virtualusertablemanagement")
    public final void setVirtualUserTableManagementService(VirtualUserTableManagementService vutManagement) {
        this.vutManagement = vutManagement;
    }

    /**
     * @see org.apache.james.remotemanager.CommandHandler#onCommand(org.apache.james.remotemanager.RemoteManagerSession,
     *      java.lang.String, java.lang.String)
     */
    public RemoteManagerResponse onCommand(RemoteManagerSession session, String command, String parameters) {
        RemoteManagerResponse response;
        String[] args = null;
        String table = null;

        if (parameters != null)
            args = parameters.split(" ");

        // check if the command was called correct
        if (args != null && args.length > 1) {
            response = new RemoteManagerResponse("Usage: " + help.getSyntax());
            return response;
        }

        if (args != null && args[0].startsWith("table=")) {
            table = args[0].substring("table=".length());

        }

        try {
            Map mappings = vutManagement.getAllMappings(table);
            if (mappings == null) {
                response = new RemoteManagerResponse("No mappings found");
            } else {
                response = new RemoteManagerResponse("Mappings:");

                Iterator m = mappings.keySet().iterator();
                while (m.hasNext()) {
                    String key = m.next().toString();
                    response.appendLine(key + "  -> " + mappings.get(key));
                }
            }
        } catch (VirtualUserTableManagementException e) {
            session.getLogger().error("Error on listing all mapping: " + e);
            response = new RemoteManagerResponse("Error on listing all mapping: " + e);
        } catch (IllegalArgumentException e) {
            session.getLogger().error("Error on listing all mapping: " + e);
            response = new RemoteManagerResponse("Error on listing all mapping: " + e);
        }
        return response;
    }

    /**
     * @see org.apache.james.socket.CommonCommandHandler#getImplCommands()
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
