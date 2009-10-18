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
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.api.protocol.AbstractCommandDispatcher;
import org.apache.james.api.protocol.LogEnabled;
import org.apache.james.remotemanager.CommandHandler;
import org.apache.james.remotemanager.LineHandler;
import org.apache.james.remotemanager.RemoteManagerResponse;
import org.apache.james.remotemanager.RemoteManagerSession;

public class RemoteManagerCommandDispatcherLineHandler extends AbstractCommandDispatcher<CommandHandler> implements LineHandler, LogEnabled{
    /** This log is the fall back shared by all instances */
    private static final Log FALLBACK_LOG = LogFactory
            .getLog(RemoteManagerCommandDispatcherLineHandler.class);
    private UnknownCmdHandler unknownCmdHandler = new UnknownCmdHandler();
    /**
     * Non context specific log should only be used when no context specific log
     * is available
     */
    private Log serviceLog = FALLBACK_LOG;

    /**
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#getLog()
     */
    protected Log getLog() {
        return serviceLog;
    }

    /**
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#getMandatoryCommands()
     */
    protected List<String> getMandatoryCommands() {
        return new ArrayList<String>();
    }

    /**
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#getUnknownCommandHandler()
     */
    protected CommandHandler getUnknownCommandHandler() {
        return unknownCmdHandler;
    }


    /**
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#getUnknownCommandHandlerIdentifier()
     */
    protected String getUnknownCommandHandlerIdentifier() {
        return UnknownCmdHandler.COMMAND_NAME;
    }

    /**
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.ExtensibleHandler#getMarkerInterfaces()
     */
    @SuppressWarnings("unchecked")
    public List<Class<?>> getMarkerInterfaces() {
        List mList = new ArrayList();
        mList.add(CommandHandler.class);
        return mList;
    }
    /**
     * @see org.apache.james.api.protocol.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.serviceLog = log;
    }

    /**
     * @see org.apache.james.remotemanager.LineHandler#onLine(org.apache.james.remotemanager.RemoteManagerSession, java.lang.String)
     */
    public void onLine(RemoteManagerSession session, String cmdString) {
        String curCommandName = null;
        String curCommandArgument = null;
        if (cmdString == null) {
        }
        int spaceIndex = cmdString.indexOf(" ");
        if (spaceIndex > 0) {
            curCommandName = cmdString.substring(0, spaceIndex);
            curCommandArgument = cmdString.substring(spaceIndex + 1);
        } else {
            curCommandName = cmdString;
        }
        curCommandName = curCommandName.toUpperCase(Locale.US);

        if (session.getLogger().isDebugEnabled()) {
            session.getLogger().debug("Command received: " + cmdString);
        }

        // fetch the command handlers registered to the command
        List<CommandHandler> commandHandlers = getCommandHandlers(
                curCommandName, session);
        if (commandHandlers == null) {
            // end the session
            RemoteManagerResponse resp = new RemoteManagerResponse( "Local configuration error: unable to find a command handler.");
            resp.setEndSession(true);
            session.writeRemoteManagerResponse(resp);
        } else {
            int count = commandHandlers.size();
            for (int i = 0; i < count; i++) {
                RemoteManagerResponse response = commandHandlers.get(i).onCommand(
                        session, curCommandName, curCommandArgument);
                if (response != null) {
                    session.writeRemoteManagerResponse(response);
                    break;
                }
            }

        }
    }

}
