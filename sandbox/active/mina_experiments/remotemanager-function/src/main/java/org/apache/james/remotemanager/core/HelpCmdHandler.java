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

import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.CommandHelp;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;
import org.apache.james.socket.WiringException;
import org.apache.james.socket.shared.ExtensibleHandler;

public class HelpCmdHandler implements CommandHandler, ExtensibleHandler{

    private final static String COMMAND_NAME = "HELP";
    private CommandHelp help = new CommandHelp("help","displays this help");

    private List<CommandHandler> extensions;
    
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
        for (int i = 0; i < extensions.size(); i++) {
            CommandHandler cmd = extensions.get(i);
            CommandHelp help = cmd.getHelp();
            if (help != null) {
                if (response == null) {
                    response = new RemoteManagerResponse(help.getSyntax() + "\t" + help.getDescription());
                } else {
                    response.appendLine(help.getSyntax() + "\t" + help.getDescription());
                }
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

    /**
     * @see org.apache.james.socket.shared.ExtensibleHandler#getMarkerInterfaces()
     */
    @SuppressWarnings("unchecked")
    public List<Class<?>> getMarkerInterfaces() {
        List mList = new ArrayList();
        mList.add(CommandHandler.class);
        return mList;
    }

    /**
     * @see org.apache.james.socket.shared.ExtensibleHandler#wireExtensions(java.lang.Class, java.util.List)
     */
    public void wireExtensions(Class interfaceName, List extension) throws WiringException {
        if (interfaceName.equals(CommandHandler.class)) {
            extensions = extension;
        }
    }

}
