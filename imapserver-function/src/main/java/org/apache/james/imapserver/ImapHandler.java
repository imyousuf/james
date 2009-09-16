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

package org.apache.james.imapserver;

import java.io.IOException;
import java.net.Socket;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.commons.logging.Log;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.encode.ImapResponseComposer;
import org.apache.james.imap.encode.base.ImapResponseComposerImpl;
import org.apache.james.imap.main.ImapRequestHandler;
import org.apache.james.imap.main.ImapSessionImpl;
import org.apache.james.imap.main.OutputStreamImapResponseWriter;
import org.apache.james.socket.ProtocolHandler;
import org.apache.james.socket.ProtocolContext;

/**
 * Handles IMAP connections.
 */
public class ImapHandler implements ProtocolHandler
{
    
    private ProtocolContext helper;

    private static final byte[] EMERGENCY_SIGNOFF = {'*',' ', 'B', 'Y', 'E', ' ', 
        'S', 'e', 'r', 'v', 'e', 'r', ' ', 'f', 'a', 'u', 'l', 't', '\r', '\n'};

    private final String hello;
    private final ImapRequestHandler requestHandler;
    private ImapSession session;

    public ImapHandler(final ImapRequestHandler requestHandler, final String hello) {
        super();
        this.requestHandler = requestHandler;
        this.hello = hello;
    }

    /**
     * Resets the handler data to a basic state.
     */
    public void resetHandler() {
        
        // Clear user data
        try {
            if (session != null) {
                session.logout();
            }
        } catch (Exception e) {
            getLogger().warn("Failed to close mailbox: " + e.getMessage());
            getLogger().debug(e.getMessage(), e);
        }
        session = null;
    }

    /**
     * @see ConnectionHandler#handleConnection(Socket)
     */
    public void handleProtocol() throws IOException {
            getLogger().debug(
                "Connection from " + helper.getRemoteHost() + " (" + helper.getRemoteIP()
                        + ") opened.");
            final OutputStreamImapResponseWriter writer = new OutputStreamImapResponseWriter( helper.getOutputStream() );
            ImapResponseComposer response = new ImapResponseComposerImpl( writer);

            // Write welcome message
                 
            response.hello(hello);

            setUpSession();
            
            helper.getWatchdog().start();
            while ( handleRequest() ) {
                helper.getWatchdog().reset();
            }
            helper.getWatchdog().stop();
            if (session != null) {
                session.logout();
            }
            
            getLogger().info(
                    "Connection from " + helper.getRemoteHost() + " (" + helper.getRemoteIP()
                            + ") closed.");
    }

    /**
     * Sets up a session.
     */
    private void setUpSession() {
        final ImapSessionImpl session = new ImapSessionImpl();
        session.setLog(helper.getLogger());
        this.session = session;
    }

    private boolean handleRequest() {
        final boolean result = requestHandler.handleRequest( helper.getInputStream(), helper.getOutputStream(), session );
        return result;
    }
    
    /**
     * Method which will be called on error
     *  
     * @param e the RuntimeException
     */
    public void fatalFailure(RuntimeException e, ProtocolContext context) {
        try {
            context.getOutputStream().write(EMERGENCY_SIGNOFF);
        } catch (Throwable t) {
            getLogger().debug("Write emergency signoff failed.", t);
        }
    }
    
    public Log getLogger() {
        return helper.getLogger();
    }

    public void setProtocolHandlerHelper(ProtocolContext phh) {
        this.helper = phh;
    }

}

