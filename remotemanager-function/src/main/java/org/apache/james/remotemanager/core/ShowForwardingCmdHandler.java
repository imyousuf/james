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

import org.apache.james.api.user.JamesUser;
import org.apache.james.api.user.User;
import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.user.UsersStore;
import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;
import org.apache.mailet.MailAddress;

/**
 * Handler called upon receipt of an SHOWFORWARDING command.
 *
 */
public class ShowForwardingCmdHandler implements CommandHandler{

    private final static String COMMAND_NAME = "SHOWFORWARDING";
    private CommandHelp help = new CommandHelp("showforwarding [username]","shows a user's current email forwarding");

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

    public RemoteManagerResponse onCommand(RemoteManagerSession session, String command, String parameters) {
        RemoteManagerResponse response;
        if ( parameters == null || parameters.equals("") ) {
            response = new RemoteManagerResponse("Usage: " + help.getSyntax());
            return response;
        }

        UsersRepository users = uStore.getRepository((String) session.getState().get(RemoteManagerSession.CURRENT_USERREPOSITORY));

        
        // Verify user exists
        User baseuser = users.getUserByName(parameters);
        if (baseuser == null) {
            response = new RemoteManagerResponse("No such user " + parameters);
            return response;
        } else if (! (baseuser instanceof JamesUser ) ) {
            response = new RemoteManagerResponse("Can't set forwarding for this user type.");
            return response;
        }
        JamesUser user = (JamesUser)baseuser;
        if ( user == null ) {
            response = new RemoteManagerResponse("No such user " + parameters);
            return response;
        }

        if ( !user.getForwarding() ) {
            response = new RemoteManagerResponse("User " + parameters + " is not currently being forwarded");
            return response;
        }

        MailAddress fwdAddr = user.getForwardingDestination();

        if ( fwdAddr == null ) {    //  defensive programming -- should not occur
            String errmsg = "For user " + parameters + ", the system indicates that forwarding is set but no forwarding destination was found";
            response = new RemoteManagerResponse(errmsg);
            session.getLogger().error(errmsg);
            return response;
        }

        response = new RemoteManagerResponse("Current forwarding destination for " + parameters + " is: " + fwdAddr);
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
