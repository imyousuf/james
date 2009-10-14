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

package org.apache.james.pop3server.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.pop3server.CommandHandler;
import org.apache.james.pop3server.LineHandler;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;
import org.apache.james.socket.shared.AbstractCommandDispatcher;
import org.apache.james.socket.shared.LogEnabled;

public class POP3CommandDispatcherLineHandler extends
        AbstractCommandDispatcher<CommandHandler> implements LineHandler, LogEnabled {
    private final static String[] mandatoryCommands = { "USER", "PASS", "LIST" };
    private final UnknownCmdHandler unknownHandler = new UnknownCmdHandler();
    /** This log is the fall back shared by all instances */
    private static final Log FALLBACK_LOG = LogFactory
            .getLog(POP3CommandDispatcherLineHandler.class);

    /**
     * Non context specific log should only be used when no context specific log
     * is available
     */
    private Log serviceLog = FALLBACK_LOG;

    /**
     * @see org.apache.james.socket.shared.AbstractCommandDispatcher#getLog()
     */
    protected Log getLog() {
        return serviceLog;
    }

    /**
     * @see org.apache.james.socket.shared.AbstractCommandDispatcher#getMandatoryCommands()
     */
    protected List<String> getMandatoryCommands() {
        return Arrays.asList(mandatoryCommands);
    }

    /**
     * @see org.apache.james.socket.shared.AbstractCommandDispatcher#getUnknownCommandHandler()
     */
    protected CommandHandler getUnknownCommandHandler() {
        return unknownHandler;
    }

    /**
     * @see org.apache.james.socket.shared.AbstractCommandDispatcher#getUnknownCommandHandlerIdentifier()
     */
    protected String getUnknownCommandHandlerIdentifier() {
        return UnknownCmdHandler.COMMAND_NAME;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.james.socket.ExtensibleHandler#getMarkerInterfaces()
     */
    @SuppressWarnings("unchecked")
    public List<Class<?>> getMarkerInterfaces() {
        List list = new ArrayList();
        list.add(CommandHandler.class);
        return list;
    }

    /**
     * @see org.apache.james.pop3server.LineHandler#onLine(org.apache.james.pop3server.POP3Session,
     *      java.lang.String)
     */
    public void onLine(POP3Session session, String cmdString) {
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
            // Don't display password in logger
            if (!curCommandName.equals("PASS")) {
                session.getLogger().debug("Command received: " + cmdString);
            } else {
                session.getLogger().debug(
                        "Command received: PASS <password omitted>");
            }
        }

        // fetch the command handlers registered to the command
        List<CommandHandler> commandHandlers = getCommandHandlers(
                curCommandName, session);
        if (commandHandlers == null) {
            // end the session
            POP3Response resp = new POP3Response(POP3Response.ERR_RESPONSE,
                    "Local configuration error: unable to find a command handler.");
            resp.setEndSession(true);
            session.writePOP3Response(resp);
        } else {
            int count = commandHandlers.size();
            for (int i = 0; i < count; i++) {
                POP3Response response = commandHandlers.get(i).onCommand(
                        session, curCommandName, curCommandArgument);
                if (response != null) {
                    session.writePOP3Response(response);
                    break;
                }
            }

        }
    }

    /**
     * @see org.apache.james.socket.shared.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.serviceLog = log;
    }

}
