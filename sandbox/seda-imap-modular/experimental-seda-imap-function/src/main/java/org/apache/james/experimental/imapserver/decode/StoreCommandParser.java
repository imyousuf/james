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

import javax.mail.Flags;

import org.apache.james.experimental.imapserver.ImapRequestLineReader;
import org.apache.james.experimental.imapserver.ProtocolException;
import org.apache.james.experimental.imapserver.commands.ImapCommand;
import org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.experimental.imapserver.message.IdRange;
import org.apache.james.experimental.imapserver.message.ImapMessage;
import org.apache.james.experimental.imapserver.message.StoreDirective;

class StoreCommandParser extends AbstractUidCommandParser implements InitialisableCommandFactory
{
    public StoreCommandParser() {
    }

    /**
     * @see org.apache.james.experimental.imapserver.decode.InitialisableCommandFactory#init(org.apache.james.experimental.imapserver.commands.imap4rev1.Imap4Rev1CommandFactory)
     */
    public void init(Imap4Rev1CommandFactory factory)
    {
        final ImapCommand command = factory.getStore();
        setCommand(command);
    }
    
    StoreDirective storeDirective( ImapRequestLineReader request ) throws ProtocolException
    {
        int sign = 0;
        boolean silent = false;

        char next = request.nextWordChar();
        if ( next == '+' ) {
            sign = 1;
            request.consume();
        }
        else if ( next == '-' ) {
            sign = -1;
            request.consume();
        }
        else {
            sign = 0;
        }

        String directive = consumeWord( request, new NoopCharValidator() );
        if ( "FLAGS".equalsIgnoreCase( directive ) ) {
            silent = false;
        }
        else if ( "FLAGS.SILENT".equalsIgnoreCase( directive ) ) {
            silent = true;
        }
        else {
            throw new ProtocolException( "Invalid Store Directive: '" + directive + "'" );
        }
        return new StoreDirective( sign, silent );
    }

    protected ImapMessage decode(ImapCommand command, ImapRequestLineReader request, String tag, boolean useUids) throws ProtocolException {
        final IdRange[] idSet = parseIdRange( request );
        final StoreDirective directive = storeDirective( request );
        final Flags flags = flagList( request );
        endLine( request );
        final ImapMessage result = getMessageFactory().createStoreMessage(command, idSet, directive, flags, useUids, tag);
        return result;
    }
}
