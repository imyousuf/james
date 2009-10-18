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



package org.apache.james.remotemanager;

import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.james.api.protocol.ProtocolHandlerChain;
import org.apache.james.socket.api.CRLFTerminatedReader;
import org.apache.james.socket.api.ProtocolContext;
import org.apache.james.socket.api.ProtocolHandler;
import org.apache.james.socket.api.Watchdog;


/**
 * Provides a console-based administration interface covering most of the management 
 * functionality found in the classes from package org.apache.james.management
 * 
 */
public class RemoteManagerHandler implements ProtocolHandler, RemoteManagerSession {
 
    /**
     * The per-service configuration data that applies to all handlers
     */
    private final RemoteManagerHandlerConfigurationData theConfigData;
    private ProtocolContext context;
   
    private Map<Object,Object> stateMap = new HashMap<Object, Object>();

    private boolean sessionEnded;

    private ProtocolHandlerChain handlerChain;

    private LinkedList<ConnectHandler> connectHandlers;

    private LinkedList<LineHandler> lineHandlers;
    
    public RemoteManagerHandler(final RemoteManagerHandlerConfigurationData theConfigData, final ProtocolHandlerChain handlerChain) {
        this.theConfigData = theConfigData; 
        this.handlerChain = handlerChain;
        connectHandlers = handlerChain.getHandlers(ConnectHandler.class);
        lineHandlers = handlerChain.getHandlers(LineHandler.class);
    }

    /**
     * @see org.apache.avalon.cornerstone.services.connection.ConnectionHandler#handleConnection(Socket)
     */
    public void handleProtocol(ProtocolContext context) throws IOException {
        this.context = context;
        sessionEnded = false;
        getState().put(RemoteManagerSession.CURRENT_USERREPOSITORY, "LocalUsers");


        //Session started - RUN all connect handlers
        if(connectHandlers != null) {
            int count = connectHandlers.size();
            for(int i = 0; i < count; i++) {
                connectHandlers.get(i).onConnect(this);
                if(sessionEnded) {
                    break;
                }
            }
        }

        context.writeLoggedResponse("Please enter your login and password");
        String login = null;
        String password = null;
        do {
            if (login != null) {
                final String message = "Login failed for " + login;
                context.writeLoggedFlushedResponse(message);
            }
            context.writeLoggedFlushedResponse("Login id:");
            login = context.getInputReader().readLine().trim();
            context.writeLoggedFlushedResponse("Password:");
            password = context.getInputReader().readLine().trim();
        } while (!password.equals(theConfigData.getAdministrativeAccountData().get(login)) || password.length() == 0);

        StringBuilder messageBuffer =
            new StringBuilder(64)
                    .append("Welcome ")
                    .append(login)
                    .append(". HELP for a list of commands");
        context.getOutputWriter().println( messageBuffer.toString() );
        context.getOutputWriter().flush();
        if (context.getLogger().isInfoEnabled()) {
            StringBuilder infoBuffer =
                new StringBuilder(128)
                        .append("Login for ")
                        .append(login)
                        .append(" successful");
            context.getLogger().info(infoBuffer.toString());
        }

        context.getWatchdog().start();
        while(!sessionEnded) {
            String line = null;
            // parse the command
            try {
                line = context.getInputReader().readLine();
                if (line != null) {
                    line = line.trim();
                }
            } catch (CRLFTerminatedReader.TerminationException te) {
                context.writeLoggedFlushedResponse("-ERR Syntax error at character position " + te.position() + ". CR and LF must be CRLF paired.  See RFC 1939 #3.");
            }

          if (line == null) {
              break;
          }

          if (lineHandlers.size() > 0) {
              context.getOutputWriter().print(theConfigData.getPrompt());
              context.getOutputWriter().flush();
              
              ((LineHandler) lineHandlers.getLast()).onLine(this, line);
          } else {
              sessionEnded = true;
          }
          context.getWatchdog().reset();
          
        }
        context.getWatchdog().stop();
        if (context.getLogger().isInfoEnabled()) {
            StringBuilder infoBuffer =
                new StringBuilder(64)
                        .append("Logout for ")
                        .append(login)
                        .append(".");
            context.getLogger().info(infoBuffer.toString());
        }
       
    }

    /**
     * @see org.apache.james.socket.AbstractJamesHandler#fatalFailure(java.lang.RuntimeException, ProtocolContext)
     */
    public void fatalFailure(RuntimeException e, ProtocolContext context) {
        context.getOutputWriter().println("Unexpected Error: "+e.getMessage());
        context.getOutputWriter().flush();
    }

    /**
     * Resets the handler data to a basic state.
     */
    public void resetHandler() {
        sessionEnded = true;
        
        // clear the state map
        getState().clear();
        
        // empty any previous line handler and add self (command dispatcher)
        // as the default.
        lineHandlers = handlerChain.getHandlers(LineHandler.class);
    }

  

    /**
     * (non-Javadoc)
     * @see org.apache.james.remotemanager.RemoteManagerSession#getLogger()
     */
    public Log getLogger() {
        return context.getLogger();
    }

    /**
     * (non-Javadoc)
     * @see org.apache.james.remotemanager.RemoteManagerSession#getState()
     */
    public Map<Object, Object> getState() {
        return stateMap;
    }

    /**
     * (non-Javadoc)
     * @see org.apache.james.remotemanager.RemoteManagerSession#getWatchdog()
     */
    public Watchdog getWatchdog() {
        return context.getWatchdog();
    }

    /**
     * @see org.apache.james.remotemanager.RemoteManagerSession#writeRemoteManagerResponse(org.apache.james.remotemanager.RemoteManagerResponse)
     */
    public void writeRemoteManagerResponse(RemoteManagerResponse response) {
        // Write a single-line or multiline response
        if (response != null) {
            if (response.getRawLine() != null) {
                context.writeLoggedFlushedResponse(response.getRawLine());
            } 
            
            List<CharSequence> responseList = response.getLines();
            if (responseList != null) {
                for (int k = 0; k < responseList.size(); k++) {
                    final CharSequence line = responseList.get(k);
                    context.writeLoggedFlushedResponse(line.toString());
                }
            }
           
            if (response.isEndSession()) {
                sessionEnded = true;
            }
        }
    }


}
