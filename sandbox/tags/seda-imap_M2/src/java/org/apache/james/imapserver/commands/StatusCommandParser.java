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

import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.commands.CommandParser.CharacterValidator;
import org.apache.james.imapserver.commands.CommandParser.NoopCharValidator;

class StatusCommandParser extends CommandParser
{
    public StatusCommandParser(ImapCommand command) {
        super(command);
    }

    StatusDataItems statusDataItems( ImapRequestLineReader request )
            throws ProtocolException
    {
        StatusDataItems items = new StatusDataItems();

        request.nextWordChar();
        consumeChar( request, '(' );
        CharacterValidator validator = new NoopCharValidator();
        String nextWord = consumeWord( request, validator );
        while ( ! nextWord.endsWith(")" ) ) {
            addItem( nextWord, items );
            nextWord = consumeWord( request, validator );
        }
        // Got the closing ")", may be attached to a word.
        if ( nextWord.length() > 1 ) {
            addItem( nextWord.substring(0, nextWord.length() - 1 ), items );
        }

        return items;
    }

    private void addItem( String nextWord, StatusDataItems items )
            throws ProtocolException
    {
        if ( nextWord.equals( StatusCommand.MESSAGES ) ) {
            items.messages = true;
        }
        else if ( nextWord.equals( StatusCommand.RECENT ) ) {
            items.recent = true;
        }
        else if ( nextWord.equals( StatusCommand.UIDNEXT ) ) {
            items.uidNext = true;
        }
        else if ( nextWord.equals( StatusCommand.UIDVALIDITY ) ) {
            items.uidValidity = true;
        }
        else if ( nextWord.equals( StatusCommand.UNSEEN ) ) {
            items.unseen = true;
        }
        else {
            throw new ProtocolException( "Unknown status item: '" + nextWord + "'" );
        }
    }

    protected AbstractImapCommandMessage decode(ImapCommand command, ImapRequestLineReader request, String tag) throws ProtocolException {
        final String mailboxName = mailbox( request );
        final StatusDataItems statusDataItems = statusDataItems( request );
        endLine( request );
        final StatusCommandMessage result = 
            new StatusCommandMessage(command, mailboxName, statusDataItems, tag);
        return result;
    }
}
