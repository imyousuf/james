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

import javax.mail.Flags;

import org.apache.james.imapserver.ImapRequestLineReader;
import org.apache.james.imapserver.ProtocolException;

class StoreCommandParser extends AbstractUidCommandParser
{
    public StoreCommandParser() {
        super(new StoreCommand());
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

    protected AbstractImapCommandMessage decode(ImapCommand command, ImapRequestLineReader request, String tag, boolean useUids) throws ProtocolException {
        final IdRange[] idSet = parseIdRange( request );
        final StoreDirective directive = storeDirective( request );
        final Flags flags = flagList( request );
        endLine( request );
        final StoreCommandMessage result = 
            new StoreCommandMessage(command, idSet, directive, flags, useUids, tag);
        return result;
    }
}
