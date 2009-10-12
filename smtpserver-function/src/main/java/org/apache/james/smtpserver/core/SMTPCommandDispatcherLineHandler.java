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

package org.apache.james.smtpserver.core;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.james.smtpserver.CommandHandler;
import org.apache.james.smtpserver.LineHandler;
import org.apache.james.smtpserver.SMTPResponse;
import org.apache.james.smtpserver.SMTPRetCode;
import org.apache.james.smtpserver.SMTPSession;
import org.apache.james.socket.AbstractCommandDispatcher;
import org.apache.james.socket.LogEnabled;


public class SMTPCommandDispatcherLineHandler extends AbstractCommandDispatcher<CommandHandler> implements LogEnabled, LineHandler {

    /** This log is the fall back shared by all instances */
    private static final Log FALLBACK_LOG = LogFactory.getLog(SMTPCommandDispatcherLineHandler.class);
    
    /** Non context specific log should only be used when no context specific log is available */
    private Log serviceLog = FALLBACK_LOG;
    

    private final CommandHandler unknownHandler = new UnknownCmdHandler();

    private final static String[] mandatoryCommands = { "MAIL" , "RCPT", "DATA"};


    /**
     * @see org.apache.james.smtpserver.LineHandler#onLine(org.apache.james.smtpserver.SMTPSession, byte[])
     */
    public void onLine(SMTPSession session, byte[] line) {
        String cmdString;
        try {
            cmdString = new String(line, "US-ASCII");
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

            List<CommandHandler> commandHandlers = getCommandHandlers(curCommandName, session);
            //fetch the command handlers registered to the command
            if(commandHandlers == null) {
                //end the session
                SMTPResponse resp = new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Local configuration error: unable to find a command handler.");
                resp.setEndSession(true);
                session.writeSMTPResponse(resp);
            } else {
                int count = commandHandlers.size();
                for(int i = 0; i < count; i++) {
                    SMTPResponse response = commandHandlers.get(i).onCommand(session, curCommandName, curCommandArgument);
                    
                    session.writeSMTPResponse(response);
                    
                    //if the response is received, stop processing of command handlers
                    if(response != null) {
                        break;
                    }
                    
                    // NOTE we should never hit this line, otherwise we ended the CommandHandlers with
                    // no responses.
                    // (The note is valid for i == count-1) 
                }

            }        
        } catch (UnsupportedEncodingException e) {
            // This should never happen, anyway return a error message and disconnect is prolly the best thing todo here
            session.getLogger().error("Unable to parse line",e);
            //end the session
            SMTPResponse resp = new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Unable to parse line.");
            resp.setEndSession(true);
            session.writeSMTPResponse(resp);
        }
    }

    /**
     * @see org.apache.james.socket.ExtensibleHandler#getMarkerInterfaces()
     */
    @SuppressWarnings("unchecked")
    public List getMarkerInterfaces() {
        List res = new LinkedList();
        res.add(CommandHandler.class);
        return res;
    }

    /**
     * @see org.apache.james.socket.AbstractCommandDispatcher#getLog()
     */
    protected Log getLog() {
        return serviceLog;
    }


    /**
     * @see org.apache.james.socket.AbstractCommandDispatcher#getUnknownCommandHandlerIdentifier()
     */
    protected String getUnknownCommandHandlerIdentifier() {
        return UnknownCmdHandler.UNKNOWN_COMMAND;
    }

    /**
     * @see org.apache.james.socket.AbstractCommandDispatcher#getMandatoryCommands()
     */
    protected List<String> getMandatoryCommands() {
        return Arrays.asList(mandatoryCommands);
    }

    /**
     * @see org.apache.james.socket.AbstractCommandDispatcher#getUnknownCommandHandler()
     */
    protected CommandHandler getUnknownCommandHandler() {
        return unknownHandler;
    }

    /**
     * @see org.apache.james.socket.LogEnabled#setLog(org.apache.commons.logging.Log)
     */
    public void setLog(Log log) {
        this.serviceLog = log;
    }
}
