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

import org.apache.james.management.SpoolFilter;
import org.apache.james.management.SpoolManagementService;
import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;

/**
 * Handler called upon receipt of a FLUSHSPOOL command
 */
 public class FlushSpoolCmdHandler  implements CommandHandler {
 

    private final static String COMMAND_NAME = "FLUSHSPOOL";
    private CommandHelp help = new CommandHelp("flushspool [spoolrepositoryname] ([key] | [header=name] [regex=value])","try to resend the mail assing to the given key. If no key is given all mails get resend");
    private SpoolManagementService spoolManagement;

    /**
     * Set the SpoolManagementService
     * 
     * @param spoolManagement
     *            the SpoolManagementService
     */
    @Resource(name = "spoolmanagement")
    public final void setSpoolManagement(SpoolManagementService spoolManagement) {
        this.spoolManagement = spoolManagement;
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
        int count = 0;
        String[] args = null;

        if (parameters != null)
            args = parameters.split(" ");

        // check if the command was called correct
        if ((parameters == null || parameters.trim().equals(""))
                || (args.length < 1 || args.length > 3 || (!args[1].startsWith(RemoteManagerSession.KEY_IDENTIFIER) && (args.length > 1  
                && !args[1].startsWith(RemoteManagerSession.HEADER_IDENTIFIER))) || (args.length == 3 && !args[2].startsWith(RemoteManagerSession.REGEX_IDENTIFIER)))) {
            response = new RemoteManagerResponse("Usage: " + help.getSyntax());
            return response;
        }

        String url = args[0];
        String key = null;
        String header = null;
        String regex = null;
        
        if (args[1].startsWith(RemoteManagerSession.KEY_IDENTIFIER)) {
            key = args[1].substring(RemoteManagerSession.KEY_IDENTIFIER.length()); 
        } else {
            header = args[1].substring(RemoteManagerSession.HEADER_IDENTIFIER.length());
            regex = args[2].substring(RemoteManagerSession.REGEX_IDENTIFIER.length()); 
        }
        
        try {
            if (key != null) {
                count = spoolManagement.resendSpoolItems(url, key, null, SpoolFilter.ERRORMAIL_FILTER);
            } else {
                count = spoolManagement.resendSpoolItems(url, key, null, new SpoolFilter(SpoolFilter.ERROR_STATE,header,regex));
            }
            response = new RemoteManagerResponse("Number of flushed mails: " + count);
            

        } catch (Exception e) {
            response = new RemoteManagerResponse("Error accessing the spoolrepository "
                            + e.getMessage());
            session.getLogger().error(
                    "Error accessing the spoolrepository " + e.getMessage());
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

}