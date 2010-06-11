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
import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;

/**
 * Handler called upon receipt of an DELUSER command.
 */
public class DelUserCmdHandler implements CommandHandler {

    private final static String COMMAND_NAME = "DELUSER";
    private CommandHelp help = new CommandHelp("deluser [username]", "delete existing user");

    private UsersStore uStore;

    /**
     * Sets the users store.
     * 
     * @param users
     *            the users to set
     */
    @Resource(name = "users-store")
    public final void setUsers(UsersStore uStore) {
        this.uStore = uStore;
    }

    /**
     * @see org.apache.james.remotemanager.CommandHandler#getHelp()
     */
    public CommandHelp getHelp() {
        return help;
    }

    /**
     * @see org.apache.james.remotemanager.CommandHandler#onCommand(org.apache.james.remotemanager.RemoteManagerSession,
     *      java.lang.String, java.lang.String)
     */
    public RemoteManagerResponse onCommand(RemoteManagerSession session, String command, String parameters) {
        RemoteManagerResponse response;
        String user = parameters;
        if ((user == null) || (user.equals(""))) {

            response = new RemoteManagerResponse("Usage: " + help.getSyntax());
            return response;
        } else {
            UsersRepository users = uStore.getRepository(((String) session.getState().get(RemoteManagerSession.CURRENT_USERREPOSITORY)));
            if (users.contains(user)) {
                try {
                    users.removeUser(user);
                    StringBuilder responseBuffer = new StringBuilder(64).append("User ").append(user).append(" deleted");
                    String responseString = responseBuffer.toString();
                    response = new RemoteManagerResponse(responseString);
                    session.getLogger().info(responseString);
                } catch (Exception e) {          
                    StringBuilder exceptionBuffer = new StringBuilder(128).append("Error deleting user ").append(user).append(" : ").append(e.getMessage());
                    String exception = exceptionBuffer.toString();
                    response = new RemoteManagerResponse(exception);
                    session.getLogger().error(exception,e);
                }
            } else {
                StringBuilder responseBuffer = new StringBuilder(64).append("User ").append(user).append(" doesn't exist");
                String responseString = responseBuffer.toString();
                response = new RemoteManagerResponse(responseString);
            }
        }
        return response;
    }

    /**
     * @see org.apache.james.socket.shared.CommonCommandHandler#getImplCommands()
     */
    public Collection<String> getImplCommands() {
        List<String> commands = new ArrayList<String>();
        commands.add(COMMAND_NAME);
        return commands;
    }

}
