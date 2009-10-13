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
import java.util.List;

import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;

public class ListMailetsCmdHandler extends ShowMatcherInfoCmdHandler {

    private final static String COMMAND_NAME = "LISTMAILETS";
    private CommandHelp help = new CommandHelp("listmailets [processorname]","list names of all mailets for specified processor");

    @Override
    public CommandHelp getHelp() {
        return help;
    }

    @Override
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

    @Override
    public RemoteManagerResponse onCommand(RemoteManagerSession session, String command, String params) {
        RemoteManagerResponse response = null;
        if (params == null || !processorExists(params)) {
            response = new RemoteManagerResponse("Usage: " + getHelp().getSyntax());
            response.appendLine("The list of valid processor names can be retrieved using command LISTPROCESSORS");
            return response;
        }
        String[] mailetNames = processorManagementService.getMailetNames(params);
        response = new RemoteManagerResponse("Existing mailets in processor: " + mailetNames.length);
        for (int i = 0; i < mailetNames.length; i++) {
            response.appendLine((i+1) + ". " + mailetNames[i]);
         }
        return response;
    }
}
