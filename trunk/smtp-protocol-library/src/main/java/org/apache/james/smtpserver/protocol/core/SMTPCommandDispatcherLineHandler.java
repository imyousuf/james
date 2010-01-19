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

package org.apache.james.smtpserver.protocol.core;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.api.protocol.AbstractCommandDispatcher;
import org.apache.james.api.protocol.CommandHandler;
import org.apache.james.api.protocol.LineHandler;
import org.apache.james.api.protocol.Response;
import org.apache.james.lifecycle.LogEnabled;
import org.apache.james.smtpserver.protocol.SMTPRequest;
import org.apache.james.smtpserver.protocol.SMTPResponse;
import org.apache.james.smtpserver.protocol.SMTPRetCode;
import org.apache.james.smtpserver.protocol.SMTPSession;


public class SMTPCommandDispatcherLineHandler extends AbstractCommandDispatcher<SMTPSession> implements LogEnabled, LineHandler<SMTPSession> {

    /** This log is the fall back shared by all instances */
    private static final Log FALLBACK_LOG = LogFactory.getLog(SMTPCommandDispatcherLineHandler.class);
    
    /** Non context specific log should only be used when no context specific log is available */
    private Log serviceLog = FALLBACK_LOG;
    

    private final CommandHandler<SMTPSession> unknownHandler = new UnknownCmdHandler();

    private final static String[] mandatoryCommands = { "MAIL" , "RCPT", "DATA"};


    /*
     * (non-Javadoc)
     * @see org.apache.james.smtpserver.protocol.LineHandler#onLine(org.apache.james.smtpserver.protocol.SMTPSession, java.lang.String)
     */
    public void onLine(SMTPSession session, String cmdString) {
        if (cmdString != null) {
            cmdString = cmdString.trim();
        }

        String curCommandArgument = null;
        String curCommandName = null;
        int spaceIndex = cmdString.indexOf(" ");
        if (spaceIndex > 0) {
            curCommandName = cmdString.substring(0, spaceIndex);
            curCommandArgument = cmdString.substring(spaceIndex + 1);
        } else {
            curCommandName = cmdString;
        }
        curCommandName = curCommandName.toUpperCase(Locale.US);

        List<CommandHandler<SMTPSession>> commandHandlers = getCommandHandlers(curCommandName, session);
        // fetch the command handlers registered to the command
        if (commandHandlers == null) {
            // end the session
            SMTPResponse resp = new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Local configuration error: unable to find a command handler.");
            resp.setEndSession(true);
            session.writeResponse(resp);
        } else {
            int count = commandHandlers.size();
            for (int i = 0; i < count; i++) {
                Response response = commandHandlers.get(i).onCommand(session, new SMTPRequest(curCommandName, curCommandArgument));

                session.writeResponse(response);

                // if the response is received, stop processing of command
                // handlers
                if (response != null) {
                    break;
                }

                // NOTE we should never hit this line, otherwise we ended the
                // CommandHandlers with
                // no responses.
                // (The note is valid for i == count-1)
            }

        }
    }

    /**
     * @see org.apache.james.api.protocol.ExtensibleHandler#getMarkerInterfaces()
     */
    @SuppressWarnings("unchecked")
    public List getMarkerInterfaces() {
        List res = new LinkedList();
        res.add(CommandHandler.class);
        return res;
    }

    /**
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#getLog()
     */
    protected Log getLog() {
        return serviceLog;
    }


    /**
     * @see org.apache.james.api.protocol.AbstractCommandDispatcher#getUnknownCommandHandlerIdentifier()
     */
    protected String getUnknownCommandHandlerIdentifier() {
        return UnknownCmdHandler.UNKNOWN_COMMAND;
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
    protected CommandHandler<SMTPSession> getUnknownCommandHandler() {
        return unknownHandler;
    }

    /**
     * @see org.apache.james.lifecycle.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.serviceLog = log;
    }
}
