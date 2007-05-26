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

import org.apache.james.experimental.imapserver.ImapRequestLineReader;
import org.apache.james.experimental.imapserver.ProtocolException;
import org.apache.james.experimental.imapserver.commands.ImapCommand;
import org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.experimental.imapserver.message.ImapMessage;
import org.apache.james.experimental.imapserver.message.ImapMessageFactory;
class ListCommandParser extends AbstractUidCommandParser  implements InitialisableCommandFactory
{
    public ListCommandParser() {
    }

    /**
     * @see org.apache.james.experimental.imapserver.decode.InitialisableCommandFactory#init(org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory)
     */
    public void init(Imap4Rev1CommandFactory factory)
    {
        final ImapCommand command = factory.getList();
        setCommand(command);
    }
    
    /**
     * Reads an argument of type "list_mailbox" from the request, which is
     * the second argument for a LIST or LSUB command. Valid values are a "string"
     * argument, an "atom" with wildcard characters.
     * @return An argument of type "list_mailbox"
     */
    public String listMailbox( ImapRequestLineReader request ) throws ProtocolException
    {
        char next = request.nextWordChar();
        switch ( next ) {
            case '"':
                return consumeQuoted( request );
            case '{':
                return consumeLiteral( request );
            default:
                return consumeWord( request, new ListCharValidator() );
        }
    }

    private class ListCharValidator extends ATOM_CHARValidator
    {
        public boolean isValid( char chr )
        {
            if ( isListWildcard( chr ) ) {
                return true;
            }
            return super.isValid( chr );
        }
    }

    protected ImapMessage decode(ImapCommand command, ImapRequestLineReader request, String tag, boolean useUids) throws ProtocolException {
        String referenceName = mailbox( request );
        String mailboxPattern = listMailbox( request );
        endLine( request );
        final ImapMessage result = createMessage(command, referenceName, mailboxPattern, tag);
        return result;
    }
    
    protected ImapMessage createMessage(ImapCommand command, final String referenceName, final String mailboxPattern, final String tag) 
    {
        final ImapMessageFactory factory = getMessageFactory();
        final ImapMessage result = factory.createListMessage(command, referenceName, mailboxPattern, tag);
        return result;
    }
}
