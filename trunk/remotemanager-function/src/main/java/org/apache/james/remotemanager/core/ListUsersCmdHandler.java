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

import javax.annotation.Resource;

import org.apache.james.api.user.UsersRepository;
import org.apache.james.api.user.UsersStore;
import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;
import org.apache.james.services.MailServer;

public class ListUsersCmdHandler implements CommandHandler{

    private final static String COMMAND_NAME = "LISTUSERS";
    private CommandHelp help = new CommandHelp("listusers","display existing accounts");

    protected UsersStore uStore;
    protected MailServer mailServer;

    /**
     * Sets the users store.
     * 
     * @param users
     *            the users to set
     */
    @Resource(name = "org.apache.james.api.user.UsersStore")
    public final void setUsers(UsersStore uStore) {
        this.uStore = uStore;
    }

    
    /**
     * Sets the mail server.
     * @param mailServer the mailServer to set
     */
    @Resource(name="org.apache.james.services.MailServer")
    public final void setMailServer(MailServer mailServer) {
        this.mailServer = mailServer;
    }
    
    /**
     * @see org.apache.james.remotemanager.CommandHandler#getHelp()
     */
    public CommandHelp getHelp() {
        return help;
    }
    public RemoteManagerResponse onCommand(RemoteManagerSession session, String command, String parameters) {
        RemoteManagerResponse response;
        UsersRepository users = uStore.getRepository(((String) session.getState().get(RemoteManagerSession.CURRENT_USERREPOSITORY)));

        if (parameters == null) {
            response = new RemoteManagerResponse("Existing accounts " + users.countUsers());
            for (Iterator<String> it = users.list(); it.hasNext();) {
                response.appendLine("user: " + it.next());
            }
            return response;
        } else {
            if(mailServer.supportVirtualHosting() == false) {
                response = new RemoteManagerResponse("Virtualhosting not supported");
                return response;
            }
        
            ArrayList<String> userList = getDomainUserList(users,parameters);
            response = new RemoteManagerResponse("Existing accounts from domain " + parameters + " " + userList.size());
            for (int i = 0; i <userList.size(); i++) {
                response.appendLine("user: " + userList.get(i));
            }
            return response;
        }        
    }

    /**
     * Return an ArrayList which contains all usernames for the given domain
     * 
     * @param domain the domain
     * @return ArrayList which contains the users
     */
    protected ArrayList<String> getDomainUserList(UsersRepository users, String domain) {
        ArrayList<String> userList = new ArrayList<String>();
        
        for (Iterator<String> it = users.list(); it.hasNext();) {
           String user = it.next();
           if (user.endsWith(domain)) {
               userList.add(user);
           }
        }
        
        return userList;
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
