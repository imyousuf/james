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

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.api.protocol.AbstractCommandDispatcher;
import org.apache.james.api.protocol.CommandHandler;
import org.apache.james.api.protocol.LineHandler;
import org.apache.james.api.protocol.Response;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.pop3server.POP3Request;
import org.apache.james.pop3server.POP3Response;
import org.apache.james.pop3server.POP3Session;

public class POP3CommandDispatcherLineHandler extends
        AbstractCommandDispatcher<POP3Session> implements LineHandler<POP3Session>, LogEnabled {
    private final static String[] mandatoryCommands = { "USER", "PASS", "LIST" };
    private final CommandHandler<POP3Session> unknownHandler = new UnknownCmdHandler();
    /** This log is the fall back shared by all instances */
    private static final Log FALLBACK_LOG = LogFactory
            .getLog(POP3CommandDispatcherLineHandler.class);

    private final Charset charSet = Charset.forName("US-ASCII");
    
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
        return Arrays.asList(mandatoryCommands);
    }

    /**
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#getUnknownCommandHandler()
     */
    protected CommandHandler<POP3Session> getUnknownCommandHandler() {
        return unknownHandler;
    }

    /**
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#getUnknownCommandHandlerIdentifier()
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

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.LineHandler#onLine(org.apache.james.api.protocol.ProtocolSession, byte[])
     */
    public void onLine(POP3Session session, byte[] line) {
        String curCommandName = null;
        String curCommandArgument = null;
        String cmdString;

        cmdString = new String(line, charSet).trim();

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
                session.getLogger().debug("Command received: PASS <password omitted>");
            }
        }

        // fetch the command handlers registered to the command
        List<CommandHandler<POP3Session>> commandHandlers = getCommandHandlers(curCommandName, session);
        if (commandHandlers == null) {
            // end the session
            POP3Response resp = new POP3Response(POP3Response.ERR_RESPONSE, "Local configuration error: unable to find a command handler.");
            resp.setEndSession(true);
            session.writeResponse(resp);
        } else {
            int count = commandHandlers.size();
            for (int i = 0; i < count; i++) {
                Response response = commandHandlers.get(i).onCommand(session, new POP3Request(curCommandName, curCommandArgument));
                if (response != null) {
                    session.writeResponse(response);
                    break;
                }
            }

        }
       
    }

    /**
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.serviceLog = log;
    }

}
