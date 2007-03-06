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
package org.apache.james.imapserver.decode;

import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ProtocolException;
import org.apache.james.imapserver.commands.ImapCommand;
import org.apache.james.imapserver.commands.ImapCommandFactory;
import org.apache.james.imapserver.message.ImapCommandMessage;

class UidCommandParser extends AbstractImapCommandParser implements DelegatingImapCommandParser,InitialisableCommandFactory {
    private ImapCommandParserFactory parserFactory;

    public UidCommandParser() {
    }
    
    /**
     * @see org.apache.james.imapserver.decode.InitialisableCommandFactory#init(org.apache.james.imapserver.commands.ImapCommandFactory)
     */
    public void init(ImapCommandFactory factory)
    {
        final ImapCommand command = factory.getUid();
        setCommand(command);
    }
    
    /**
     * @see org.apache.james.imapserver.decode.DelegatingImapCommandParser#getParserFactory()
     */
    public ImapCommandParserFactory getParserFactory() {
        return parserFactory;
    }

    /**
     * @see org.apache.james.imapserver.decode.DelegatingImapCommandParser#setParserFactory(org.apache.james.imapserver.decode.ImapCommandParserFactory)
     */
    public void setParserFactory( ImapCommandParserFactory imapCommandFactory )
    {
        this.parserFactory = imapCommandFactory;
    }

    protected ImapCommandMessage decode(ImapCommand command, ImapRequestLineReader request, String tag) throws ProtocolException {
        // TODO: check the logic against the specification:
        // TODO: suspect that it is now bust
        // TODO: the command written may be wrong
        // TODO: this will be easier to fix a little later
        // TODO: also not sure whether the old implementation shares this flaw 
        String commandName = atom( request );
        ImapCommandParser helperCommand = parserFactory.getParser( commandName );
        // TODO: replace abstract class with interface
        if ( helperCommand == null ||
             ! (helperCommand instanceof AbstractUidCommandParser ) ) {
            throw new ProtocolException("Invalid UID command: '" + commandName + "'" );
        }
        final AbstractUidCommandParser uidEnabled = (AbstractUidCommandParser) helperCommand;
        final ImapCommandMessage result = uidEnabled.decode( request, tag, true );
        return result;
    }
    
}
