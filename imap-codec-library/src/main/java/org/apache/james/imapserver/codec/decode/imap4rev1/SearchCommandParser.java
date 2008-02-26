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
package org.apache.james.imapserver.codec.decode.imap4rev1;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.api.imap.message.request.DayMonthYear;
import org.apache.james.api.imap.message.request.SearchKey;
import org.apache.james.imapserver.codec.decode.ImapRequestLineReader;
import org.apache.james.imapserver.codec.decode.InitialisableCommandFactory;

class SearchCommandParser extends AbstractUidCommandParser implements InitialisableCommandFactory
{
    public SearchCommandParser() {
    }

    /**
     * @see org.apache.james.imapserver.codec.decode.InitialisableCommandFactory#init(org.apache.james.api.imap.imap4rev1.Imap4Rev1CommandFactory)
     */
    public void init(Imap4Rev1CommandFactory factory)
    {
        final ImapCommand command = factory.getSearch();
        setCommand(command);
    }
    
    /**
     * Parses the request argument into a valid search term.
     */
    public SearchKey searchKey( ImapRequestLineReader request ) throws ProtocolException {
        final SearchKey result;
        final char next = request.nextWordChar();
        request.consume();
        switch (next) {
            case 'a':
            case 'A':
                result = a(request);
                break;
            case 'b':
            case 'B':
                result = b(request);
                break;
            default:
                throw new ProtocolException("Unknown search key");
        }
        return result;
    }

    private SearchKey b(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        final char next = request.consume();
        switch (next) {
            case 'c':
            case 'C':
                result = bcc(request);
                break;
            case 'E':
            case 'e':
                nextIsF(request);
                nextIsO(request);
                nextIsR(request);
                nextIsE(request);
                final DayMonthYear value = date(request);
                result = SearchKey.buildBefore(value);
                break;
            default:
                throw new ProtocolException("Unknown search key");
        }

        return result;
    }

    private SearchKey bcc(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsC(request);
        final String value = astring(request);
        result = SearchKey.buildBcc(value);
        return result;
    }

    private SearchKey a(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        final char next = request.consume();
        switch (next) {
            case 'l':
            case 'L':
                nextIsL(request);
                result = SearchKey.buildAll();
                break;
            case 'n':
            case 'N':
                nextIsS(request);
                nextIsW(request);
                nextIsE(request);
                nextIsR(request);
                nextIsE(request);
                nextIsD(request);
                result = SearchKey.buildAnswered();
                break;
            default:
                throw new ProtocolException("Unknown search key");
        }
        return result;
    }
    
    private void nextIsO( ImapRequestLineReader request ) throws ProtocolException {
        final char next = request.consume();
        if (next != 'O' && next != 'o') {
            throw new ProtocolException("Unknown search key");
        }
    }
    
    private void nextIsF( ImapRequestLineReader request ) throws ProtocolException {
        final char next = request.consume();
        if (next != 'F' && next != 'f') {
            throw new ProtocolException("Unknown search key");
        }
    }
    
    private void nextIsC( ImapRequestLineReader request ) throws ProtocolException {
        final char next = request.consume();
        if (next != 'C' && next != 'c') {
            throw new ProtocolException("Unknown search key");
        }
    }
    
    private void nextIsD( ImapRequestLineReader request ) throws ProtocolException {
        final char next = request.consume();
        if (next != 'D' && next != 'd') {
            throw new ProtocolException("Unknown search key");
        }
    }
    
    private void nextIsR( ImapRequestLineReader request ) throws ProtocolException {
        final char next = request.consume();
        if (next != 'R' && next != 'r') {
            throw new ProtocolException("Unknown search key");
        }
    }
    
    private void nextIsE( ImapRequestLineReader request ) throws ProtocolException {
        final char next = request.consume();
        if (next != 'E' && next != 'e') {
            throw new ProtocolException("Unknown search key");
        }
    }
    
    private void nextIsW( ImapRequestLineReader request ) throws ProtocolException {
        final char next = request.consume();
        if (next != 'W' && next != 'w') {
            throw new ProtocolException("Unknown search key");
        }
    }
    
    private void nextIsS( ImapRequestLineReader request ) throws ProtocolException {
        final char next = request.consume();
        if (next != 'S' && next != 's') {
            throw new ProtocolException("Unknown search key");
        }
    }
    
    private void nextIsL( ImapRequestLineReader request ) throws ProtocolException {
        final char next = request.consume();
        if (next != 'L' && next != 'l') {
            throw new ProtocolException("Unknown search key");
        }
    }

    protected ImapMessage decode(ImapCommand command, ImapRequestLineReader request, String tag, boolean useUids) throws ProtocolException {
        // Parse the search term from the request
        final SearchKey key = searchKey( request );
        endLine( request );
        final ImapMessage result = getMessageFactory().createSearchMessage(command, key, useUids, tag);
        return result;
    }

}
