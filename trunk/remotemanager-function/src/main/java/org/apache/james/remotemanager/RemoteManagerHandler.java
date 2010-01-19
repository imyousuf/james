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
import org.apache.james.api.protocol.ConnectHandler;
import org.apache.james.api.protocol.LineHandler;
import org.apache.james.api.protocol.ProtocolHandlerChain;
import org.apache.james.api.protocol.Response;
import org.apache.james.socket.api.CRLFTerminatedReader;
import org.apache.james.socket.api.ProtocolContext;
import org.apache.james.socket.api.ProtocolHandler;


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
   
    private Map<String,Object> stateMap = new HashMap<String, Object>();

    private boolean sessionEnded;

    private ProtocolHandlerChain handlerChain;

    @SuppressWarnings("unchecked")
    private LinkedList<ConnectHandler> connectHandlers;

    @SuppressWarnings("unchecked")
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
    @SuppressWarnings("unchecked")
    public void handleProtocol(ProtocolContext context) throws IOException {
        this.context = context;
        sessionEnded = false;

        context.getWatchdog().start();

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
              //context.getOutputWriter().print(theConfigData.getPrompt());
              //context.getOutputWriter().flush();
              ((LineHandler) lineHandlers.getLast()).onLine(this, line);
          } else {
              sessionEnded = true;
          }
          context.getWatchdog().reset();
          
        }
        context.getWatchdog().stop();
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
        resetState();
        
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

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.LogEnabledSession#getState()
     */
    public Map<String, Object> getState() {
        return stateMap;
    }

    
    /*
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.LogEnabledSession#writeResponse(org.apache.james.api.protocol.Response)
     */
    public void writeResponse(Response response) {
        // Write a single-line or multiline response
        if (response != null) {
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

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.LogEnabledSession#resetState()
     */
    public void resetState() {
        stateMap.clear();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.remotemanager.RemoteManagerSession#popLineHandler()
     */
    public void popLineHandler() {
        if (lineHandlers != null) {
            lineHandlers.removeLast();
        }
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.remotemanager.RemoteManagerSession#pushLineHandler(org.apache.james.remotemanager.LineHandler)
     */
    @SuppressWarnings("unchecked")
    public void pushLineHandler(LineHandler<RemoteManagerSession> lineHandler) {
        if (lineHandlers == null) {
            lineHandlers = new LinkedList<LineHandler>();
        }
        lineHandlers.addLast(lineHandler);
    }


    /*
     * (non-Javadoc)
     * @see org.apache.james.remotemanager.RemoteManagerSession#getAdministrativeAccountData()
     */
    public Map<String, String> getAdministrativeAccountData() {
        return theConfigData.getAdministrativeAccountData();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.LogEnabledSession#getRemoteHost()
     */
    public String getRemoteHost() {
        return  context.getRemoteHost();
    }

    /*
     * (non-Javadoc)
     * @see org.apache.james.api.protocol.LogEnabledSession#getRemoteIPAddress()
     */
    public String getRemoteIPAddress() {
        return context.getRemoteIP();
    }


}
