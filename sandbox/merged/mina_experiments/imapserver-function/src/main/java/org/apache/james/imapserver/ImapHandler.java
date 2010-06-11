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

import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.encode.ImapResponseComposer;
import org.apache.james.imap.encode.base.ImapResponseComposerImpl;
import org.apache.james.imap.main.ImapRequestHandler;
import org.apache.james.imap.main.ImapSessionImpl;
import org.apache.james.imap.main.OutputStreamImapResponseWriter;
import org.apache.james.socket.shared.ProtocolContext;
import org.apache.james.socket.shared.ProtocolHandler;

/**
 * Handles IMAP connections.
 */
public class ImapHandler implements ProtocolHandler
{

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
        if (session != null) {
            session.logout();
        }
        session = null;
    }

    /**
     * @see ProtocolHandler#handleProtocol(ProtocolContext)
     */
    public void handleProtocol(final ProtocolContext context) throws IOException {
            context.getLogger().debug(
                "Connection from " + context.getRemoteHost() + " (" + context.getRemoteIP()
                        + ") opened.");
            final OutputStreamImapResponseWriter writer = new OutputStreamImapResponseWriter( context.getOutputStream() );
            ImapResponseComposer response = new ImapResponseComposerImpl( writer);

            // Write welcome message
                 
            response.hello(hello);

            setUpSession(context);
            
            context.getWatchdog().start();
            while ( handleRequest(context) ) {
                context.getWatchdog().reset();
            }
            context.getWatchdog().stop();
            if (session != null) {
                session.logout();
            }
            
            context.getLogger().info(
                    "Connection from " + context.getRemoteHost() + " (" + context.getRemoteIP()
                            + ") closed.");
    }

    /**
     * Sets up a session.
     * @param context not null
     */
    private void setUpSession(ProtocolContext context) {
        final ImapSessionImpl session = new ImapSessionImpl();
        session.setLog(context.getLogger());
        this.session = session;
    }

    private boolean handleRequest(ProtocolContext context) {
        final boolean result = requestHandler.handleRequest( context.getInputStream(), context.getOutputStream(), session );
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
            context.getLogger().debug("Write emergency signoff failed.", t);
        }
    }
}

