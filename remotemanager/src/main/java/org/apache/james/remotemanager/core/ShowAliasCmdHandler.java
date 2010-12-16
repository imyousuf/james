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

import org.apache.james.protocols.api.Request;
import org.apache.james.protocols.api.Response;
import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;
import org.apache.james.user.api.JamesUser;
import org.apache.james.user.api.User;
import org.apache.james.user.api.UsersRepository;

/**
 * Handler called upon receipt of an SHOWALIAS command.
 * 
 */
public class ShowAliasCmdHandler implements CommandHandler {

    private final static String COMMAND_NAME = "SHOWALIAS";
    private CommandHelp help = new CommandHelp("showalias [username]","shows a user's current email alias");

    private UsersRepository users;

    /**
     * Sets the users repository.
     * 
     * @param users
     *            the users to set
     */
    @Resource(name = "localusersrepository")
    public final void setUsers(UsersRepository users) {
        this.users = users;
    }
    
    /**
     * @see org.apache.james.remotemanager.CommandHandler#getHelp()
     */
    public CommandHelp getHelp() {
        return help;
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.protocols.api.CommandHandler#onCommand(org.apache.james.protocols.api.ProtocolSession, org.apache.james.protocols.api.Request)
     */
    public Response onCommand(RemoteManagerSession session, Request request) {
        RemoteManagerResponse response;
        String parameters = request.getArgument();
        if ( parameters == null || parameters.equals("") ) {
            response = new RemoteManagerResponse("Usage: " + help.getSyntax());
            return response;
        }

        User baseuser = users.getUserByName(parameters);
        if (baseuser == null) {
            response = new RemoteManagerResponse("No such user " + parameters);
            return response;
        } else if (! (baseuser instanceof JamesUser ) ) {
            response = new RemoteManagerResponse("Can't show aliases for this user type.");
            return response;
        }

        JamesUser user = (JamesUser)baseuser;
        if ( user == null ) {
            response = new RemoteManagerResponse("No such user " + parameters);
            return response;
        }

        if ( !user.getAliasing() ) {
            response = new RemoteManagerResponse("User " + parameters + " does not currently have an alias");
            return response;
        }

        String alias = user.getAlias();

        if ( alias == null || alias.equals("") ) {    //  defensive programming -- neither should occur
            String errmsg = "For user " + parameters + ", the system indicates that aliasing is set but no alias was found";
            response = new RemoteManagerResponse(errmsg);
            session.getLogger().error(errmsg);
            return response;
        }

        response = new RemoteManagerResponse("Current alias for " + parameters + " is: " + alias);
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
