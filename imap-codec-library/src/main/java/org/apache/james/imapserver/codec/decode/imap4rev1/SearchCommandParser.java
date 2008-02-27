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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

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
    public static final void main(String[] args) throws Exception {
        final byte[] mixed = "BEforE 1-Jan-2000".getBytes("US-ASCII");
        final byte[] allSmalls = "before 1-Jan-2000".getBytes("US-ASCII");
        final byte[] allCaps = "BEFORE 1-Jan-2000".getBytes("US-ASCII");
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        SearchCommandParser parser = new SearchCommandParser();
        long start  = System.currentTimeMillis();
        long i = 0;
        ByteArrayInputStream bais = new ByteArrayInputStream(allCaps);
        while (i++ < 1000000) {
                bais.reset();
                ImapRequestLineReader request = new ImapRequestLineReader(
                        bais, 
                        baos);
                parser.searchKey(request);
        }
        i = 0;
        bais = new ByteArrayInputStream(allSmalls);
        while (i++ < 1000000) {
            bais.reset();
            ImapRequestLineReader request = new ImapRequestLineReader(
                    bais, 
                    baos);
            parser.searchKey(request);
        }
        i = 0;
        bais = new ByteArrayInputStream(mixed);
        while (i++ < 1000000) {
            bais.reset();
            ImapRequestLineReader request = new ImapRequestLineReader(
                    bais, 
                    baos);
            parser.searchKey(request);
        }
        long end = System.currentTimeMillis();
        System.out.println(end - start);
    }
    
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
        final int cap = consumeAndCap(request);
        switch (cap) {
            case 'A': return a(request);
            case 'B': return b(request);
            case 'C': return cc(request);
            case 'D': return d(request);
            case 'E': throw new ProtocolException("Unknown search key");
            case 'F': return f(request);
            case 'G': throw new ProtocolException("Unknown search key");
            case 'H': return header(request);
            case 'I': throw new ProtocolException("Unknown search key");
            case 'J': throw new ProtocolException("Unknown search key");
            case 'K': return keyword(request);
            case 'L': return larger(request);
            case 'M': throw new ProtocolException("Unknown search key");
            case 'N': return n(request);
            case 'O': return o(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }

    private int consumeAndCap(ImapRequestLineReader request) throws ProtocolException {
        final char next = request.consume();
        final int cap = next > 'Z' ? next ^ 32 : next;
        return cap;
    }

    private SearchKey cc(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsC(request);
        nextIsSpace(request);
        final String value = astring(request);
        result = SearchKey.buildCc(value);
        return result;
    }
    
    private SearchKey o(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'L': return old(request);
            case 'N': return on(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }
    
    private SearchKey n(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'E': return _new(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }
    
    private SearchKey f(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'L': return flagged(request);
            case 'R': return from(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }
    
    private SearchKey d(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'E': return deleted(request);
            case 'R': return draft(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }

    private SearchKey keyword(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsE(request);
        nextIsY(request);
        nextIsW(request);
        nextIsO(request);
        nextIsR(request);
        nextIsD(request);
        nextIsSpace(request);
        final String value = atom(request);
        result = SearchKey.buildKeyword(value);
        return result;
    }
    
    private SearchKey header(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsE(request);
        nextIsA(request);
        nextIsD(request);
        nextIsE(request);
        nextIsR(request);
        nextIsSpace(request);
        final String field = astring(request);
        nextIsSpace(request);
        final String value = astring(request);
        result = SearchKey.buildHeader(field, value);
        return result;
    }
    
    private SearchKey larger(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsA(request);
        nextIsR(request);
        nextIsG(request);
        nextIsE(request);
        nextIsR(request);
        nextIsSpace(request);
        final long value = number(request);
        result = SearchKey.buildLarger(value);
        return result;
    }
    
    private SearchKey from(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsO(request);
        nextIsM(request);
        nextIsSpace(request);
        final String value = astring(request);
        result = SearchKey.buildFrom(value);
        return result;
    }
    
    private SearchKey flagged(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsA(request);
        nextIsG(request);
        nextIsG(request);
        nextIsE(request);
        nextIsD(request);
        result = SearchKey.buildFlagged();
        return result;
    }
    
    private SearchKey old(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsD(request);
        result = SearchKey.buildOld();
        return result;
    }
    
    private SearchKey _new(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsW(request);
        result = SearchKey.buildNew();
        return result;
    }
    
    private SearchKey draft(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsA(request);
        nextIsF(request);
        nextIsT(request);
        result = SearchKey.buildDraft();
        return result;
    }
    
    private SearchKey deleted(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsL(request);
        nextIsE(request);
        nextIsT(request);
        nextIsE(request);
        nextIsD(request);
        result = SearchKey.buildDeleted();
        return result;
    }
    
    private SearchKey b(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'C': return bcc(request);
            case 'E': return before(request);
            case 'O': return body(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }

    private SearchKey body(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsD(request);
        nextIsY(request);
        nextIsSpace(request);
        final String value = astring(request);
        result = SearchKey.buildBody(value);
        return result;
    }

    private SearchKey on(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsSpace(request);
        final DayMonthYear value = date(request);
        result = SearchKey.buildOn(value);
        return result;
    }
    
    private SearchKey before(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsF(request);
        nextIsO(request);
        nextIsR(request);
        nextIsE(request);
        nextIsSpace(request);
        final DayMonthYear value = date(request);
        result = SearchKey.buildBefore(value);
        return result;
    }

    private SearchKey bcc(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsC(request);
        nextIsSpace(request);
        final String value = astring(request);
        result = SearchKey.buildBcc(value);
        return result;
    }

    private SearchKey a(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'L': return all(request);
            case 'N': return answered(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }

    private SearchKey answered(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsS(request);
        nextIsW(request);
        nextIsE(request);
        nextIsR(request);
        nextIsE(request);
        nextIsD(request);
        result = SearchKey.buildAnswered();
        return result;
    }

    private SearchKey all(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsL(request);
        result = SearchKey.buildAll();
        return result;
    }
    
    private void nextIsSpace(ImapRequestLineReader request ) throws ProtocolException {
        final char next = request.consume();
        if (next != ' ') {
            throw new ProtocolException("Unknown search key");
        }
    }
    
    private void nextIsG( ImapRequestLineReader request ) throws ProtocolException {
        final char next = request.consume();
        if (next != 'G' && next != 'g') {
            throw new ProtocolException("Unknown search key");
        }
    }
    
    private void nextIsM( ImapRequestLineReader request ) throws ProtocolException {
        final char next = request.consume();
        if (next != 'M' && next != 'm') {
            throw new ProtocolException("Unknown search key");
        }
    }
    
    private void nextIsA( ImapRequestLineReader request ) throws ProtocolException {
        final char next = request.consume();
        if (next != 'A' && next != 'a') {
            throw new ProtocolException("Unknown search key");
        }
    }
    
    private void nextIsT( ImapRequestLineReader request ) throws ProtocolException {
        final char next = request.consume();
        if (next != 'T' && next != 't') {
            throw new ProtocolException("Unknown search key");
        }
    }
    
    private void nextIsY( ImapRequestLineReader request ) throws ProtocolException {
        final char next = request.consume();
        if (next != 'Y' && next != 'y') {
            throw new ProtocolException("Unknown search key");
        }
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
        request.nextWordChar();
        final SearchKey key = searchKey( request );
        endLine( request );
        final ImapMessage result = getMessageFactory().createSearchMessage(command, key, useUids, tag);
        return result;
    }

}
