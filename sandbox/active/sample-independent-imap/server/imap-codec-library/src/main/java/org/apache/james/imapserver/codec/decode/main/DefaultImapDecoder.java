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
import org.apache.james.api.imap.ImapSessionState;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.imapserver.codec.ProtocolException;
import org.apache.james.imapserver.codec.decode.ImapCommandParser;
import org.apache.james.imapserver.codec.decode.ImapCommandParserFactory;
import org.apache.james.imapserver.codec.decode.ImapDecoder;
import org.apache.james.imapserver.codec.decode.ImapRequestLineReader;
import org.apache.james.imapserver.codec.decode.base.AbstractImapCommandParser;

public class DefaultImapDecoder extends AbstractLogEnabled implements ImapDecoder {

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
    
    public ImapMessage decode(ImapRequestLineReader request, ImapSession session) {
        ImapMessage message;
        final Logger logger = getLogger(); 
        
        try {
            final String tag = AbstractImapCommandParser.tag( request );    
            message = decodeCommandTagged(request, logger, tag, session);
        }
        catch ( ProtocolException e ) {
            logger.debug("Cannot parse tag", e);
            
            // When the tag cannot be read, there is something seriously wrong.
            // It is probably not possible to recover
            // and (since this may indicate an attack) wiser not to try
            message = messageFactory.bye(HumanReadableTextKey.ILLEGAL_TAG);
        }
        return message;
    }

    private ImapMessage decodeCommandTagged(final ImapRequestLineReader request, final Logger logger, 
            final String tag, final ImapSession session) {
        ImapMessage message;
        if (logger.isDebugEnabled()) { 
            logger.debug( "Got <tag>: " + tag );
        }
        try {
            final String commandName = AbstractImapCommandParser.atom( request );
            message = decodeCommandNamed(request, tag, commandName, logger, session);
        }
        catch ( ProtocolException e ) {
            logger.debug("Error during initial request parsing", e);    
            message = unknownCommand(tag, session);
        }
        return message;
    }

    private ImapMessage unknownCommand(final String tag,
            final ImapSession session) {
        ImapMessage message;
        if (session.getState() == ImapSessionState.NON_AUTHENTICATED) {
            message = messageFactory.bye(HumanReadableTextKey.BYE_UNKNOWN_COMMAND);
            session.logout();
        } else {
            message = messageFactory.taggedBad(tag, null, HumanReadableTextKey.UNKNOWN_COMMAND);
        }
        return message;
    }

    private ImapMessage decodeCommandNamed(final ImapRequestLineReader request, 
            final String tag, String commandName, final Logger logger, final ImapSession session) {
        ImapMessage message;
        if (logger.isDebugEnabled()) { 
            logger.debug( "Got <command>: " + commandName); 
        }
        final ImapCommandParser command = imapCommands.getParser( commandName );
        if ( command == null ) {
            logger.info("Missing command implementation.");
            message = unknownCommand(tag, session);
        } else {
            message = command.parse( request, tag );
        }
        return message;
    }
}
