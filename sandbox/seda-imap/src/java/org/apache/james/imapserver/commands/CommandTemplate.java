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

package org.apache.james.imapserver.commands;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.imapserver.AuthorizationException;
import org.apache.james.imapserver.ImapConstants;
import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ImapResponse;
import org.apache.james.imapserver.ImapSession;
import org.apache.james.imapserver.ImapSessionState;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.encode.RecordingImapResponseWriter;
import org.apache.james.imapserver.store.MailboxException;

/**
 * Base class for all command implementations. This class provides common
 * core functionality useful for all {@link org.apache.james.imapserver.commands.ImapCommand} implementations.
 *
 * @version $Revision: 109034 $
 */
abstract class CommandTemplate
        extends AbstractLogEnabled
        implements ImapCommand, ImapConstants, ImapCommandParser
{
    protected CommandParser parser = new CommandParser();

    /**
     * By default, valid in any state (unless overridden by subclass.
     * @see org.apache.james.imapserver.commands.ImapCommand#validForState
     */
    public boolean validForState( ImapSessionState state )
    {
        return true;
    }

    /**
     * Template methods for handling command processing. This method reads
     * argument values (validating them), and checks the request for correctness.
     * If correct, the command processing is delegated to the specific command
     * implemenation.
     *
     * @see ImapCommand#process
     */
    public void process( ImapRequestLineReader request,
                         ImapResponse response,
                         ImapSession session )
    {
        try {
            doProcess( request, response, session );
        }
        catch ( MailboxException e ) {
            getLogger().debug("error processing command ", e);
            response.commandFailed( this, e.getResponseCode(), e.getMessage() );
        }
        catch ( AuthorizationException e ) {
            getLogger().debug("error processing command ", e);
            String msg = "Authorization error: Lacking permissions to perform requested operation.";
            response.commandFailed( this, msg );
        }
        catch ( ProtocolException e ) {
            getLogger().debug("error processing command ", e);
            String msg = e.getMessage() + " Command should be '" +
                    getExpectedMessage() + "'";
            response.commandError( msg );
        }
    }

    
    /**
     * Parses a request into a command message
     * for later processing.
     * @param request <code>ImapRequestLineReader</code>, not null
     * @return <code>ImapCommandMessage</code>, not null
     */
    public ImapCommandMessage parse( ImapRequestLineReader request ) {
        ImapCommandMessage message;
        try {
            
            message = decode(request);
            
        } catch ( ProtocolException e ) {
            getLogger().debug("error processing command ", e);
            String msg = e.getMessage() + " Command should be '" +
                    getExpectedMessage() + "'";
            message = new ErrorResponseMessage( msg );
        }
        return message;
    }

    /**
     * Parses a request into a command message
     * for later processing.
     * @param request <code>ImapRequestLineReader</code>, not null
     * @return <code>ImapCommandMessage</code>, not null
     * @throws ProtocolException if the request cannot be parsed
     */
    protected abstract AbstractImapCommandMessage decode( ImapRequestLineReader request ) 
        throws ProtocolException;
    
    /**
     * This is the method overridden by specific command implementations to
     * perform commend-specific processing.
     *
     * @param request The client request
     * @param response The server response
     * @param session The current client session
     */
    protected final void doProcess( ImapRequestLineReader request,
                                       ImapResponse response,
                                       ImapSession session )
            throws ProtocolException, MailboxException, AuthorizationException {
        AbstractImapCommandMessage message = decode( request );
        ImapResponseMessage responseMessage = message.doProcess( session );
        responseMessage.encode(response, session);
    }

    /**
     * Provides a message which describes the expected format and arguments
     * for this command. This is used to provide user feedback when a command
     * request is malformed.
     *
     * @return A message describing the command protocol format.
     */
    protected String getExpectedMessage()
    {
        StringBuffer syntax = new StringBuffer( "<tag> " );
        syntax.append( getName() );

        String args = getArgSyntax();
        if ( args != null && args.length() > 0 ) {
            syntax.append( " " );
            syntax.append( args );
        }

        return syntax.toString();
    }

    /**
     * Provides the syntax for the command arguments if any. This value is used
     * to provide user feedback in the case of a malformed request.
     *
     * For commands which do not allow any arguments, <code>null</code> should
     * be returned.
     *
     * @return The syntax for the command arguments, or <code>null</code> for
     *         commands without arguments.
     */
    protected abstract String getArgSyntax();


    public CommandParser getParser()
    {
        return parser;
    }
    
    protected abstract class AbstractImapCommandMessage implements ImapCommandMessage {

        public ImapResponseMessage process(ImapSession session) {
            ImapResponseMessage result;
            final Logger logger = getLogger();
            try {
                result = doProcess( session );
            }
            catch ( MailboxException e ) {
                if (logger != null) {
                    logger.debug("error processing command ", e);
                }
                result = new CommandFailedResponseMessage( CommandTemplate.this, e.getResponseCode(), e.getMessage() );
            }
            catch ( AuthorizationException e ) {
                if (logger != null) {
                    logger.debug("error processing command ", e);
                }
                String msg = "Authorization error: Lacking permissions to perform requested operation.";
                result = new CommandFailedResponseMessage( CommandTemplate.this, null, msg );
            }
            catch ( ProtocolException e ) {
                if (logger != null) {
                    logger.debug("error processing command ", e);
                }
                String msg = e.getMessage() + " Command should be '" +
                        getExpectedMessage() + "'";
                result = new ErrorResponseMessage( msg );
            }
            return result;
        }
        
        protected abstract ImapResponseMessage doProcess(ImapSession session) throws MailboxException, AuthorizationException, ProtocolException;
    }
    
    protected class CompleteCommandMessage extends AbstractImapCommandMessage {

        private final boolean useUids;
        
        public CompleteCommandMessage(final boolean useUids) {
            this.useUids = useUids;
        }
        
        protected ImapResponseMessage doProcess(ImapSession session) throws MailboxException, AuthorizationException, ProtocolException {
            final CommandCompleteResponseMessage result = new CommandCompleteResponseMessage(useUids, CommandTemplate.this);
            return result;
        }
        
    }
}
