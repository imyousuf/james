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

import javax.annotation.Resource;

import org.apache.james.management.DomainListManagementException;
import org.apache.james.management.DomainListManagementService;
import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;

/**
 * Handler which is called when a REMOVEDOMAIN command is dispatched
 *
 */
public class RemoveDomainCmdHandler implements CommandHandler{
    
    private final static String COMMAND_NAME = "REMOVEDOMAIN";
    private CommandHelp help = new CommandHelp("removedomain [domainname]","remove domain from local domains");

    private DomainListManagementService domService;

    @Resource(name="domainlistmanagement")
    public final void setDomainListManagement(DomainListManagementService domService) {
        this.domService = domService;
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
    public RemoteManagerResponse onCommand(RemoteManagerSession session, String command, String parameters) {
        RemoteManagerResponse response = null;
        // check if the command was called correct
        if (parameters == null) {
            response = new RemoteManagerResponse("Usage: REMOVEDOMAIN domain");
            return response;
        }
        
        try {
            if(domService.removeDomain(parameters)) {
                response = new RemoteManagerResponse("Removing domain " + parameters + " successful");
               
            } else {
                response = new RemoteManagerResponse("Removing domain " + parameters + " fail");
            }
        } catch (DomainListManagementException e) {
            session.getLogger().error("Error on removing domain: " + e);
            response = new RemoteManagerResponse("Error on removing domain: " + e);
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
}
