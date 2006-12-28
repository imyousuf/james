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

package org.apache.james.smtpserver;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.james.smtpserver.core.UnknownCmdHandler;
import org.apache.james.util.mail.SMTPRetCode;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

public class SMTPCommandDispatcherLineHandler extends AbstractLogEnabled implements LineHandler, ExtensibleHandler {

    /**
     * The list of available command handlers
     */
    private HashMap commandHandlerMap = new HashMap();

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

            List commandHandlers = getCommandHandlers(curCommandName);
            //fetch the command handlers registered to the command
            if(commandHandlers == null) {
                //end the session
                SMTPResponse resp = new SMTPResponse(SMTPRetCode.LOCAL_ERROR, "Local configuration error: unable to find a command handler.");
                resp.setEndSession(true);
                session.writeSMTPResponse(resp);
            } else {
                int count = commandHandlers.size();
                for(int i = 0; i < count; i++) {
                    SMTPResponse response = ((CommandHandler)commandHandlers.get(i)).onCommand(session, curCommandName, curCommandArgument);
                    
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
            // TODO Define what to do
            e.printStackTrace();
        }
    }

    /**
     * @see org.apache.james.smtpserver.ExtensibleHandler#getMarkerInterfaces()
     */
    public List getMarkerInterfaces() {
        List res = new LinkedList();
        res.add(CommandHandler.class);
        return res;
    }

    /**
     * @throws WiringException 
     * @see org.apache.james.smtpserver.ExtensibleHandler#wireExtensions(java.lang.Class, java.util.List)
     */
    public void wireExtensions(Class interfaceName, List extension) throws WiringException {
        this.commandHandlerMap = new HashMap();

        for (Iterator it = extension.iterator(); it.hasNext(); ) {
            CommandHandler handler = (CommandHandler) it.next();
            Collection implCmds = handler.getImplCommands();
    
            for (Iterator i = implCmds.iterator(); i.hasNext(); ) {
                String commandName = ((String) i.next()).trim().toUpperCase(Locale.US);
                if (getLogger().isInfoEnabled()) {
                    getLogger().info(
                            "Added Commandhandler: " + handler.getClass() + " for command "+commandName);
                }
                addToMap(commandName, (CommandHandler) handler);
            }
        }

        addToMap(UnknownCmdHandler.UNKNOWN_COMMAND, unknownHandler);

        if (commandHandlerMap.size() < 2) {
            if (getLogger().isErrorEnabled()) {
                getLogger().error("No commandhandlers configured");
            }
            throw new WiringException("No commandhandlers configured");
        } else {
            boolean found = true;
            for (int i = 0; i < mandatoryCommands.length; i++) {
                if (!commandHandlerMap.containsKey(mandatoryCommands[i])) {
                    if (getLogger().isErrorEnabled()) {
                        getLogger().error(
                                "No commandhandlers configured for the command:"
                                        + mandatoryCommands[i]);
                    }
                    found = false;
                    break;
                }
            }

            if (!found) {
                throw new WiringException(
                        "No commandhandlers configured for mandatory commands");
            }


        }

    }
    

    /**
     * Add it to map (key as command name, value is an array list of commandhandlers)
     *
     * @param commandName the command name which will be key
     * @param cmdHandler The commandhandler object
     */
    private void addToMap(String commandName, CommandHandler cmdHandler) {
        ArrayList handlers = (ArrayList)commandHandlerMap.get(commandName);
        if(handlers == null) {
            handlers = new ArrayList();
            commandHandlerMap.put(commandName, handlers);
        }
        handlers.add(cmdHandler);
    }


    /**
     * Returns all the configured commandhandlers for the specified command
     *
     * @param command the command name which will be key
     * @return List of commandhandlers
     */
    List getCommandHandlers(String command) {
        if (command == null) {
            return null;
        }
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("Lookup command handler for command: " + command);
        }
        List handlers =  (List)commandHandlerMap.get(command);
        if(handlers == null) {
            handlers = (List)commandHandlerMap.get(UnknownCmdHandler.UNKNOWN_COMMAND);
        }

        return handlers;
    }

}
