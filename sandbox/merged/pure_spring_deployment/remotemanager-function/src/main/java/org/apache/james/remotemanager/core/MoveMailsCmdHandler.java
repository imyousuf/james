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
 * Handler called upon receipt of a MOVEMAILS command
 */
public class MoveMailsCmdHandler implements CommandHandler {

    private final static String COMMAND_NAME = "MOVEMAILS";
    private CommandHelp help = new CommandHelp(
            "movemails [srcSpoolrepositoryname] [dstSpoolrepositoryname] ([header=headername] [regex=regexValue]) [srcstate=sourcestate] [dststate=destinationstate]",
            "move mails from the source repository to the destination repository");
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
        String[] args = null;

        if (parameters != null)
            args = parameters.split(" ");

        // check if the command was called correct
        if ((parameters == null || parameters.trim().equals("")) || (args.length < 2 || args.length > 6)) {
            response = new RemoteManagerResponse("Usage: " + help.getSyntax());
        }

        String srcUrl = args[0];
        String dstUrl = args[1];

        String dstState = null;
        String srcState = null;
        String header = null;
        String regex = null;

        for (int i = 2; i < args.length; i++) {
            if (args[i].startsWith(RemoteManagerSession.HEADER_IDENTIFIER)) {
                header = args[i].substring(RemoteManagerSession.HEADER_IDENTIFIER.length());
            } else if (args[i].startsWith(RemoteManagerSession.REGEX_IDENTIFIER)) {
                header = args[i].substring(RemoteManagerSession.REGEX_IDENTIFIER.length());
            } else if (args[i].startsWith("srcstate=")) {
                header = args[i].substring("srcstate=".length());
            } else if (args[i].startsWith("dststate=")) {
                header = args[i].substring("dststate=".length());
            } else {
                response = new RemoteManagerResponse("Unexpected parameter " + args[i]);
                response.appendLine("Usage: " + help.getSyntax());
                return response;
            }
        }

        if ((header != null && regex == null) || (header == null && regex != null)) {
            if (regex == null) {
                response = new RemoteManagerResponse("Bad parameters: used header without regex");
            } else {
                response = new RemoteManagerResponse("Bad parameters: used regex without header");
            }
            response.appendLine("Usage: " + help.getSyntax());
            return response;
        }

        try {
            int count = spoolManagement.moveSpoolItems(srcUrl, dstUrl, dstState, new SpoolFilter(srcState, header, regex));

            response = new RemoteManagerResponse("Number of moved mails: " + count);

        } catch (Exception e) {
            response = new RemoteManagerResponse("Error opening the spoolrepository " + e.getMessage());

            session.getLogger().error("Error opeing the spoolrepository " + e.getMessage());
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
