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

import java.io.InputStream;
import java.io.OutputStream;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.imapserver.commands.CommandParser;
import org.apache.james.imapserver.commands.ImapCommand;
import org.apache.james.imapserver.commands.ImapCommandFactory;
import org.apache.james.imapserver.encode.OutputStreamImapResponseWriter;

/**
 * @version $Revision: 109034 $
 */
public final class ImapRequestHandler extends AbstractLogEnabled {
    
    private final ImapCommandFactory imapCommands = new ImapCommandFactory();
    private static final String REQUEST_SYNTAX = "Protocol Error: Was expecting <tag SPACE command [arguments]>";

    /**
     * @see org.apache.avalon.framework.logger.AbstractLogEnabled#enableLogging(org.apache.avalon.framework.logger.Logger)
     */
    public void enableLogging(Logger logger) { 
        super.enableLogging(logger);
        setupLogger(imapCommands);
    }
    
    /**
     * This method parses IMAP commands read off the wire in handleConnection.
     * Actual processing of the command (possibly including additional back and
     * forth communication with the client) is delegated to one of a number of
     * command specific handler methods.  The primary purpose of this method is
     * to parse the raw command string to determine exactly which handler should
     * be called.  It returns true if expecting additional commands, false otherwise.
     *
     * @return whether additional commands are expected.
     */
    public boolean handleRequest( InputStream input,
                                  OutputStream output,
                                  ImapSession session )
            throws ProtocolException
    {
        ImapRequestLineReader request = new ImapRequestLineReader( input, output );
        setupLogger(request);
        
        try {
            request.nextChar();
        }
        catch ( ProtocolException e ) {
            getLogger().debug("Cannot handling request: ", e);
            return false;
        }

        ImapResponse response = new ImapResponse( new OutputStreamImapResponseWriter( output ));
        response.enableLogging(getLogger()); 

        doProcessRequest( request, response, session );

        // Consume the rest of the line, throwing away any extras. This allows us
        // to clean up after a protocol error.
        request.consumeLine();

        return true;
    }

    private void doProcessRequest( ImapRequestLineReader request,
                                   ImapResponse response,
                                   ImapSession session)
    {
        String tag = null;
        String commandName = null;

        try {
            tag = CommandParser.tag( request );
        }
        catch ( ProtocolException e ) {
            getLogger().debug("error parsing request", e);
            response.badResponse( REQUEST_SYNTAX );
            return;
        }

        final Logger logger = getLogger(); 
        if (logger.isDebugEnabled()) { 
            logger.debug( "Got <tag>: " + tag );
        }
        
        try {
            commandName = CommandParser.atom( request );
        }
        catch ( ProtocolException e ) {
            getLogger().debug("error parsing request", e);            
            response.commandError( REQUEST_SYNTAX , tag);
            return;
        }

        if (logger.isDebugEnabled()) { 
            logger.debug( "Got <command>: " + commandName); 
        }
        
        ImapCommand command = imapCommands.getCommand( commandName );
        if ( command == null )
        {
            response.commandError( "Invalid command.", tag);
            return;
        }

        command.process( request, response, session, tag );
    }


}
