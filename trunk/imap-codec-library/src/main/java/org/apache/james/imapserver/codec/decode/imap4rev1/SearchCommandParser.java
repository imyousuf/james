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
import org.apache.james.api.imap.message.IdRange;
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
        final char next = request.nextChar();
        if (next >= '0' && next <= '9' || next == '*') {
            return sequenceSet(request);
        } else {
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
                case 'P': throw new ProtocolException("Unknown search key");
                case 'Q': throw new ProtocolException("Unknown search key");
                case 'R': return recent(request);
                case 'S': return s(request);
                case 'T': return t(request);
                case 'U': return u(request);
                default:
                    throw new ProtocolException("Unknown search key");
            }
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

    private SearchKey u(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'I': return uid(request);
            case 'N': return un(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }
    
    private SearchKey un(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'A': return unanswered(request);
            case 'D': return und(request);
            case 'F': return unflagged(request);
            case 'K': return unkeyword(request);
            case 'S': return unseen(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }
    
    private SearchKey und(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'E': return undeleted(request);
            case 'R': return undraft(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }
    
    private SearchKey t(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'E': return text(request);
            case 'O': return to(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }
    
    private SearchKey s(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'E': return se(request);
            case 'I': return since(request);
            case 'M': return smaller(request);
            case 'U': return subject(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }
    
    private SearchKey se(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'E': return seen(request);
            case 'N': return sen(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }
    
    private SearchKey sen(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'T': return sent(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }
    
    private SearchKey sent(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'B': return sentBefore(request);
            case 'O': return sentOn(request);
            case 'S': return sentSince(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }
    
    private SearchKey o(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'L': return old(request);
            case 'N': return on(request);
            case 'R': return or(request);
            default:
                throw new ProtocolException("Unknown search key");
        }
    }
    
    private SearchKey n(ImapRequestLineReader request) throws ProtocolException {
        final int next = consumeAndCap(request);
        switch (next) {
            case 'E': return _new(request);
            case 'O': return not(request);
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
    
    private SearchKey unkeyword(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsE(request);
        nextIsY(request);
        nextIsW(request);
        nextIsO(request);
        nextIsR(request);
        nextIsD(request);
        nextIsSpace(request);
        final String value = atom(request);
        result = SearchKey.buildUnkeyword(value);
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
    
    private SearchKey smaller(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsA(request);
        nextIsL(request);
        nextIsL(request);
        nextIsE(request);
        nextIsR(request);
        nextIsSpace(request);
        final long value = number(request);
        result = SearchKey.buildSmaller(value);
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
    
    private SearchKey unseen(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsE(request);
        nextIsE(request);
        nextIsN(request);
        result = SearchKey.buildUnseen();
        return result;
    }
    
    private SearchKey undraft(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsA(request);
        nextIsF(request);
        nextIsT(request);
        result = SearchKey.buildUndraft();
        return result;
    }
    
    private SearchKey undeleted(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsL(request);
        nextIsE(request);
        nextIsT(request);
        nextIsE(request);
        nextIsD(request);
        result = SearchKey.buildUndeleted();
        return result;
    }
    
    private SearchKey unflagged(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsL(request);
        nextIsA(request);
        nextIsG(request);
        nextIsG(request);
        nextIsE(request);
        nextIsD(request);
        result = SearchKey.buildUnflagged();
        return result;
    }
    
    private SearchKey unanswered(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsN(request);
        nextIsS(request);
        nextIsW(request);
        nextIsE(request);
        nextIsR(request);
        nextIsE(request);
        nextIsD(request);
        result = SearchKey.buildUnanswered();
        return result;
    }
    
    private SearchKey old(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsD(request);
        result = SearchKey.buildOld();
        return result;
    }
    
    private SearchKey or(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsSpace(request);
        final SearchKey firstKey = searchKey(request);
        nextIsSpace(request);
        final SearchKey secondKey = searchKey(request);
        result = SearchKey.buildOr(firstKey, secondKey);
        return result;
    }
    
    private SearchKey not(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsT(request);
        nextIsSpace(request);
        final SearchKey nextKey = searchKey(request);
        result = SearchKey.buildNot(nextKey);
        return result;
    }
    
    private SearchKey _new(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsW(request);
        result = SearchKey.buildNew();
        return result;
    }
    
    private SearchKey recent(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsE(request);
        nextIsC(request);
        nextIsE(request);
        nextIsN(request);
        nextIsT(request);
        result = SearchKey.buildRecent();
        return result;
    }
    
    private SearchKey seen(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsN(request);
        result = SearchKey.buildSeen();
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
    
    private SearchKey sentBefore(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsE(request);
        nextIsF(request);
        nextIsO(request);
        nextIsR(request);
        nextIsE(request);
        nextIsSpace(request);
        final DayMonthYear value = date(request);
        result = SearchKey.buildSentBefore(value);
        return result;
    }
    
    private SearchKey sentSince(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsI(request);
        nextIsN(request);
        nextIsC(request);
        nextIsE(request);
        nextIsSpace(request);
        final DayMonthYear value = date(request);
        result = SearchKey.buildSentSince(value);
        return result;
    }
    
    private SearchKey since(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsN(request);
        nextIsC(request);
        nextIsE(request);
        nextIsSpace(request);
        final DayMonthYear value = date(request);
        result = SearchKey.buildSince(value);
        return result;
    }
    
    private SearchKey sentOn(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsN(request);
        nextIsSpace(request);
        final DayMonthYear value = date(request);
        result = SearchKey.buildSentOn(value);
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
    
    private SearchKey text(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsX(request);
        nextIsT(request);
        nextIsSpace(request);
        final String value = astring(request);
        result = SearchKey.buildText(value);
        return result;
    }
    
    private SearchKey uid(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsD(request);
        nextIsSpace(request);
        final IdRange[] range = parseIdRange(request);
        result = SearchKey.buildUidSet(range);
        return result;
    }
    
    private SearchKey sequenceSet(ImapRequestLineReader request) throws ProtocolException {
        final IdRange[] range = parseIdRange(request);
        final SearchKey result = SearchKey.buildSequenceSet(range);
        return result;
    }
    
    private SearchKey to(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsSpace(request);
        final String value = astring(request);
        result = SearchKey.buildTo(value);
        return result;
    }
    
    private SearchKey subject(ImapRequestLineReader request) throws ProtocolException {
        final SearchKey result;
        nextIsB(request);
        nextIsJ(request);
        nextIsE(request);
        nextIsC(request);
        nextIsT(request);
        nextIsSpace(request);
        final String value = astring(request);
        result = SearchKey.buildSubject(value);
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
        nextIs(request, 'G', 'g');
    }
    
    private void nextIsM( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'M', 'm');
    }
    
    private void nextIsI( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'I', 'i');
    }
    
    private void nextIsN( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'N', 'n');
    }
    
    private void nextIsA( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'A', 'a');
    }
    
    private void nextIsT( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'T', 't');
    }
    
    private void nextIsY( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'Y','y');
    }
    
    private void nextIsX( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'X', 'x');
    }
    
    private void nextIsO( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'O', 'o');
    }
    
    private void nextIsF( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'F', 'f');
    }
    
    private void nextIsJ( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'J', 'j');
    }
    
    private void nextIsC( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'C', 'c');
    }
    
    private void nextIsD( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'D', 'd');
    }
    
    private void nextIsB( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'B', 'b');
    }
    
    private void nextIsR( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'R', 'r');
    }
    
    private void nextIsE( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'E', 'e');
    }
    
    private void nextIsW( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'W', 'w');
    }
    
    private void nextIsS( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'S', 's');
    }
    
    private void nextIsL( ImapRequestLineReader request ) throws ProtocolException {
        nextIs(request, 'L', 'l');
    }

    private void nextIs( ImapRequestLineReader request, final char upper, final char lower ) throws ProtocolException {
        final char next = request.consume();
        if (next != upper && next != lower) {
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
