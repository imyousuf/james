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

package org.apache.james.experimental.imapserver;

import java.io.IOException;
import java.net.Socket;

import org.apache.avalon.cornerstone.services.connection.ConnectionHandler;
import org.apache.avalon.excalibur.pool.Poolable;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.Constants;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.core.AbstractJamesHandler;
import org.apache.james.experimental.imapserver.encode.writer.OutputStreamImapResponseWriter;
import org.apache.james.imapserver.codec.decode.ImapDecoder;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.apache.james.imapserver.codec.encode.base.ImapResponseComposerImpl;

/**
 * The handler class for IMAP connections.
 * TODO: This is a quick cut-and-paste hack from POP3Handler. This, and the ImapServer
 * should probably be rewritten from scratch.
 */
public class ImapHandler
        extends AbstractJamesHandler
        implements ImapHandlerInterface, ConnectionHandler, Poolable, ImapConstants
{

    // TODO: inject dependency
    private String softwaretype = "JAMES "+VERSION+" Server " + Constants.SOFTWARE_VERSION;
    private ImapRequestHandler requestHandler;
    private ImapSession session;

    /**
     * The per-service configuration data that applies to all handlers
     */
    private ImapHandlerConfigurationData theConfigData;

    /**
     * The session termination status
     */
    private boolean sessionEnded = false;

    /**
     * Set the configuration data for the handler.
     *
     * @param theData the configuration data
     */
    public void setConfigurationData( Object theData )
    {
        if (theData instanceof ImapHandlerConfigurationData) {
            theConfigData = (ImapHandlerConfigurationData) theData;
            final ImapEncoder imapEncoder = theConfigData.getImapEncoder();
            final ImapProcessor imapProcessor = theConfigData.getImapProcessor();
            final ImapDecoder imapDecoder = theConfigData.getImapDecoder();
            requestHandler = new ImapRequestHandler(imapDecoder, imapProcessor, imapEncoder);
        } else {
            throw new IllegalArgumentException("Configuration object does not implement POP3HandlerConfigurationData");
        }
    }

    public void forceConnectionClose(final String message) {
        getLogger().debug("forceConnectionClose: "+message);
        final OutputStreamImapResponseWriter writer = new OutputStreamImapResponseWriter(outs);
        ImapResponseComposer response = new ImapResponseComposerImpl(writer);
        try {
            response.byeResponse(message);
        } catch (IOException e) {
            getLogger().info("Write BYE failed");
            getLogger().debug("Cannot write BYE on connection close", e);
        }
        endSession();
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#endSession()
     */
    public void endSession() {
        sessionEnded = true;
    }

    /**
     * @see org.apache.james.smtpserver.SMTPSession#isSessionEnded()
     */
    public boolean isSessionEnded() {
        return sessionEnded;
    }

    /**
     * Resets the handler data to a basic state.
     */
    public void resetHandler() {
        
        endSession();
        
        // Clear user data
        try {
            if (session != null) {
                session.closeMailbox();
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
    protected void handleProtocol() throws IOException {
        try {
            final OutputStreamImapResponseWriter writer = new OutputStreamImapResponseWriter( outs );
            ImapResponseComposer response = new ImapResponseComposerImpl( writer);

            // Write welcome message
                 
            response.okResponse(null, softwaretype + " Server "
                    + theConfigData.getHelloName() + " is ready.");

            sessionEnded = false;
            session = new ImapSessionImpl( this,
                                           socket.getInetAddress().getHostName(),
                                           socket.getInetAddress().getHostAddress());
            setupLogger(session);

            theWatchdog.start();
            while ( !sessionEnded && handleRequest() ) {
                theWatchdog.reset();
            }
            theWatchdog.stop();
            

            // TODO (?) Write BYE message.
            
            getLogger().info(
                    "Connection from " + remoteHost + " (" + remoteIP
                            + ") closed.");

        }
        catch (ProtocolException e) {
            // TODO: throwing a runtime seems wrong
            throw new RuntimeException(e.getMessage(),e);
        }
    }

    private boolean handleRequest() throws ProtocolException {
        final boolean continuing = requestHandler.handleRequest( in, outs, session );
        if (!continuing) {
            resetHandler();
        }
        return continuing;
    }
    
    /**
     * Method which will be called on error
     *  
     * @param e the RuntimeException
     */
    protected void errorHandler(RuntimeException e) {
        if (e != null && e.getCause() instanceof ProtocolException) {
            out.println("Protocol exception.");
            out.flush();
            StringBuffer exceptionBuffer =
                    new StringBuffer( 128 )
                    .append( "Protocol exception during connection from " )
                    .append( remoteHost )
                    .append( " (" )
                    .append( remoteIP )
                    .append( ") : " )
                    .append( e.getMessage() );
            getLogger().error( exceptionBuffer.toString(), e.getCause() );
        } else {
            super.errorHandler(e);
        }
    }

    public void enableLogging(Logger logger) {
        super.enableLogging(logger);
        setupLogger(requestHandler);
    }
}

