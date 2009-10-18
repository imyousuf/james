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

import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.user.UsersStore;
import org.apache.james.management.DomainListManagementService;
import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;
import org.apache.james.services.MailServer;

/**
 * Handler method called upon receipt of an ADDUSER command.
 */
public class AddUserCmdHandler implements CommandHandler{
    
    private final static String COMMAND_NAME = "ADDUSER";
    private CommandHelp help = new CommandHelp("adduser [username] [password]","add a new user");

    private UsersStore uStore;
    private MailServer mailServer;
    private DomainListManagementService domService;

    @Resource(name="domainlistmanagement")
    public final void setDomainListManagement(DomainListManagementService domService) {
        this.domService = domService;
    }
    
    /**
     * Sets the users store.
     * @param users the users to set
     */
    @Resource(name="users-store")
    public final void setUsers(UsersStore uStore) {
        this.uStore = uStore;
    }
    
    /**
     * Sets the mail server.
     * @param mailServer the mailServer to set
     */
    @Resource(name="James")
    public final void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
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
        
        int breakIndex = -1;
        if ((parameters == null) ||
            (parameters.equals("")) ||
            ((breakIndex = parameters.indexOf(" ")) < 0)) {
            response = new RemoteManagerResponse("Usage: " + getHelp().getSyntax());
            return response;
        }
        String username = parameters.substring(0,breakIndex);
        String passwd = parameters.substring(breakIndex + 1);
        if (username.equals("") || passwd.equals("")) {
            response = new RemoteManagerResponse("Usage: " + getHelp().getSyntax());
            return response;
        }
        UsersRepository users = uStore.getRepository((String)session.getState().get(RemoteManagerSession.CURRENT_USERREPOSITORY));

        boolean success = false;
        if (users.contains(username)) {
            StringBuilder responseBuffer =
                new StringBuilder(64)
                        .append("User ")
                        .append(username)
                        .append(" already exists");
            response = new RemoteManagerResponse(responseBuffer.toString());
            return response;
        } else {
            if((username.indexOf("@") < 0) == false) {
                if(mailServer.supportVirtualHosting() == false) {
                    response = new RemoteManagerResponse("Virtualhosting not supported");
                    return response;
                }
                String domain = username.split("@")[1];
                if (domService.containsDomain(domain) == false) {
                    response = new RemoteManagerResponse("Domain not exists: " + domain);
                    return response;
                }
            }
            success = users.addUser(username, passwd);
        }
        if ( success ) {
            StringBuilder responseBuffer =
                new StringBuilder(64)
                        .append("User ")
                        .append(username)
                        .append(" added");
            session.getLogger().info(responseBuffer);

            response = new RemoteManagerResponse(responseBuffer.toString());
            return response;
        } else {
            session.getLogger().error("Error adding user " + username);
            response = new RemoteManagerResponse("Error adding user " + username);
            return response;
        }
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
