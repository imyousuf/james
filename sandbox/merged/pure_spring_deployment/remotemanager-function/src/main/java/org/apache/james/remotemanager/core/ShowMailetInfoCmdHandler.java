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

public class ShowMailetInfoCmdHandler extends ShowMatcherInfoCmdHandler{
    
    private final static String COMMAND_NAME = "SHOWMAILETINFO";
    private CommandHelp help = new CommandHelp("showmailetinfo [processorname] [#index]","shows configuration for mailet of specified processor at given index");

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
        Object[] parameters = extractMailetInfoParameters(session,params, "MAILET");
        if (parameters == null) return response;
        
        // extract parsed parameters
        String processorName = (String) parameters[0];
        int index = ((Integer)parameters[1]).intValue();
        
        String[] mailetParameters = null; 
        try {
            mailetParameters = processorManagementService.getMailetParameters(processorName, index);
        } catch (RuntimeException e) {
            // fall thru with NULL
        }
        if (mailetParameters == null) {
            response = new RemoteManagerResponse("The index is not referring to an existing mailet");
            return response;
        }
        response = new RemoteManagerResponse("Mailet parameters: " + mailetParameters.length);
        for (int i = 0; i < mailetParameters.length; i++) {
            String parameter = (String) mailetParameters[i];
            response.appendLine("\t" + parameter);
         }
        return response;
    }

}
