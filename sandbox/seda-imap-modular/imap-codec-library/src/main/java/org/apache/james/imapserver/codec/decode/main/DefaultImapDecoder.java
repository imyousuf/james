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
package org.apache.james.imapserver.codec.decode.main;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.imapserver.codec.decode.ImapCommandParser;
import org.apache.james.imapserver.codec.decode.ImapCommandParserFactory;
import org.apache.james.imapserver.codec.decode.ImapDecoder;
import org.apache.james.imapserver.codec.decode.ImapRequestLineReader;
import org.apache.james.imapserver.codec.decode.base.AbstractImapCommandParser;

public class DefaultImapDecoder extends AbstractLogEnabled implements ImapDecoder {

    private static final String INVALID_COMMAND = "Invalid command.";
    private static final String REQUEST_SYNTAX = "Protocol Error: Was expecting <tag SPACE command [arguments]>";

    private final Imap4Rev1MessageFactory messageFactory;
    private final ImapCommandParserFactory imapCommands;
    
    
    public DefaultImapDecoder (final Imap4Rev1MessageFactory messageFactory, final ImapCommandParserFactory imapCommands) {
        this.messageFactory = messageFactory;
        this.imapCommands = imapCommands;
    }
    
    /**
     * @see org.apache.avalon.framework.logger.AbstractLogEnabled#enableLogging(org.apache.avalon.framework.logger.Logger)
     */
    public void enableLogging(Logger logger) { 
        super.enableLogging(logger);
        setupLogger(imapCommands);
    }
    
    public ImapMessage decode(ImapRequestLineReader request) {
        ImapMessage message;
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

    private ImapMessage decodeCommandTagged(final ImapRequestLineReader request, final Logger logger, final String tag) {
        ImapMessage message;
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

    private ImapMessage decodeCommandNamed(final ImapRequestLineReader request, 
            final String tag, String commandName, final Logger logger) {
        ImapMessage message;
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
