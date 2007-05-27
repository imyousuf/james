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
package org.apache.james.experimental.imapserver.decode.imap4rev1;

import javax.mail.Message;
import javax.mail.search.SearchTerm;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.experimental.imapserver.decode.ImapRequestLineReader;
import org.apache.james.experimental.imapserver.decode.InitialisableCommandFactory;

class SearchCommandParser extends AbstractUidCommandParser implements InitialisableCommandFactory
{
    public SearchCommandParser() {
    }

    /**
     * @see org.apache.james.experimental.imapserver.decode.InitialisableCommandFactory#init(org.apache.james.api.imap.imap4rev1.Imap4Rev1CommandFactory)
     */
    public void init(Imap4Rev1CommandFactory factory)
    {
        final ImapCommand command = factory.getSearch();
        setCommand(command);
    }
    
    /**
     * Parses the request argument into a valid search term.
     * Not yet implemented - all searches will return everything for now.
     * TODO implement search
     */
    public SearchTerm searchTerm( ImapRequestLineReader request )
            throws ProtocolException
    {
        // Dummy implementation
        // Consume to the end of the line.
        char next = request.nextChar();
        while ( next != '\n' ) {
            request.consume();
            next = request.nextChar();
        }

        // Return a search term that matches everything.
        return new SearchTerm()
        {
            private static final long serialVersionUID = 5290284637903768771L;

            public boolean match( Message message )
            {
                return true;
            }
        };
    }

    protected ImapMessage decode(ImapCommand command, ImapRequestLineReader request, String tag, boolean useUids) throws ProtocolException {
        // Parse the search term from the request
        final SearchTerm searchTerm = searchTerm( request );
        endLine( request );
        final ImapMessage result = getMessageFactory().createSearchImapMessage(command, searchTerm, useUids, tag);
        return result;
    }

}
