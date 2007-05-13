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
package org.apache.james.experimental.imapserver.decode;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.experimental.imapserver.ImapDecoder;
import org.apache.james.experimental.imapserver.ImapRequestLineReader;
import org.apache.james.experimental.imapserver.ProtocolException;
import org.apache.james.experimental.imapserver.commands.ImapCommandFactory;
import org.apache.james.experimental.imapserver.commands.StandardImapCommandFactory;
import org.apache.james.experimental.imapserver.message.BaseImapMessageFactory;
import org.apache.james.experimental.imapserver.message.ImapRequestMessage;
import org.apache.james.experimental.imapserver.message.ImapMessageFactory;

public class StandardImapDecoder extends AbstractLogEnabled implements ImapDecoder {

    private static final String INVALID_COMMAND = "Invalid command.";
    // TODO: inject dependency
    private final ImapMessageFactory messageFactory = new BaseImapMessageFactory();
    private final ImapCommandFactory commandFactory = new StandardImapCommandFactory();
    private final ImapCommandParserFactory imapCommands = new ImapCommandParserFactory(messageFactory, commandFactory);
    private static final String REQUEST_SYNTAX = "Protocol Error: Was expecting <tag SPACE command [arguments]>";

    /**
     * @see org.apache.avalon.framework.logger.AbstractLogEnabled#enableLogging(org.apache.avalon.framework.logger.Logger)
     */
    public void enableLogging(Logger logger) { 
        super.enableLogging(logger);
        setupLogger(imapCommands);
    }
    
    public ImapRequestMessage decode(ImapRequestLineReader request) {
        ImapRequestMessage message;
        final Logger logger = getLogger(); 
        
        try {
            final String tag = AbstractImapCommandParser.tag( request );    
            message = decodeCommandTagged(request, logger, tag);
        }
        catch ( ProtocolException e ) {
            logger.debug("error parsing request", e);
            message = messageFactory.createBadRequestMessage( REQUEST_SYNTAX );
        }
        return message;
    }

    private ImapRequestMessage decodeCommandTagged(final ImapRequestLineReader request, final Logger logger, final String tag) {
        ImapRequestMessage message;
        if (logger.isDebugEnabled()) { 
            logger.debug( "Got <tag>: " + tag );
        }
        try {
            final String commandName = AbstractImapCommandParser.atom( request );
            message = decodeCommandNamed(request, tag, commandName, logger);
        }
        catch ( ProtocolException e ) {
            logger.debug("Error during initial request parsing", e);            
            message = messageFactory.createErrorMessage(REQUEST_SYNTAX , tag);
        }
        return message;
    }

    private ImapRequestMessage decodeCommandNamed(final ImapRequestLineReader request, 
            final String tag, String commandName, final Logger logger) {
        ImapRequestMessage message;
        if (logger.isDebugEnabled()) { 
            logger.debug( "Got <command>: " + commandName); 
        }
        final ImapCommandParser command = imapCommands.getParser( commandName );
        if ( command == null ) {
            logger.info("Missing command implementation.");
            message = messageFactory.createErrorMessage(INVALID_COMMAND, tag);
        } else {
            message = command.parse( request, tag );
        }
        return message;
    }
}
