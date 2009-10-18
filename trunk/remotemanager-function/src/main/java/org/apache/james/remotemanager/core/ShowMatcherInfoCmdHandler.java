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
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

import javax.annotation.Resource;

import org.apache.james.management.ProcessorManagementService;
import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;

public class ShowMatcherInfoCmdHandler implements CommandHandler{
    
    private final static String COMMAND_NAME = "SHOWMATCHERINFO";
    private CommandHelp help = new CommandHelp("showmatcherinfo [processorname] [#index]","shows configuration for matcher of specified processor at given index");

    protected ProcessorManagementService processorManagementService;

       
    /**
     * Set the ProcessorManagementService
     * 
     * @param processorManagement the ProcessorManagementService
     */
    @Resource(name="processormanagement")

    public final void setProcessorManagement(ProcessorManagementService processorManagement) {
        this.processorManagementService = processorManagement;
    }
    
    /**
     * @see org.apache.james.remotemanager.CommandHandler#getHelp()
     */
    public CommandHelp getHelp() {
        return help;
    }

    /**
     * @see org.apache.james.remotemanager.CommandHandler#onCommand(org.apache.james.remotemanager.RemoteManagerSession, java.lang.String, java.lang.String)
     */
    public RemoteManagerResponse onCommand(RemoteManagerSession session, String command, String params) {
        RemoteManagerResponse response = null;
        Object[] parameters = extractMailetInfoParameters(session, params, "MATCHER");
        if (parameters == null) return response;
        
        // extract parsed parameters
        String processorName = (String) parameters[0];
        int index = ((Integer)parameters[1]).intValue();
        
        String[] matcherParameters = null; 
        try {
            matcherParameters = processorManagementService.getMatcherParameters(processorName, index);
        } catch (RuntimeException e) {
            // fall thru with NULL
        }
        if (matcherParameters == null) {
            response = new RemoteManagerResponse("The index is not referring to an existing matcher");
            return response;
        }
        response = new RemoteManagerResponse("Matcher parameters: " + matcherParameters.length);
        for (int i = 0; i < matcherParameters.length; i++) {
            String parameter = (String) matcherParameters[i];
            response.appendLine("\t" + parameter);
         }
        return response;
    }

    protected Object[] extractMailetInfoParameters(RemoteManagerSession session, String argument, String commandHelp) {
        String[] argList = argument.split(" ");
        boolean argListOK = argument != null && argList != null && argList.length == 2;
        if (!argListOK) {
            session.writeRemoteManagerResponse(new RemoteManagerResponse("Usage: " + getHelp().getSyntax()));
            return null;
        }
        String processorName = argList[0];
        if (!processorExists(processorName)) {
            session.writeRemoteManagerResponse(new RemoteManagerResponse("The list of valid processor names can be retrieved using command LISTPROCESSORS"));;
            return null;
        }
        int index = -1;
        try {
            index = Integer.parseInt(argList[1]) - 1;
        } catch (NumberFormatException e) {
            // fall thru with -1
        }
        if (index < 0) {
            session.writeRemoteManagerResponse(new RemoteManagerResponse("The index parameter must be a positive number"));
            return null;
        }
        
        return new Object[] {processorName, new Integer(index)};
    }
    
    
    protected boolean processorExists(String name) {
        name = name.toLowerCase(Locale.US);
        List processorList = Arrays.asList(processorManagementService.getProcessorNames());
        return processorList.contains(name);
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
