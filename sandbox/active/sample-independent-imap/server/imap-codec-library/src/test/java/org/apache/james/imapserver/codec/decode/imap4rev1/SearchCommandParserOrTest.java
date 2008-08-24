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
import org.apache.james.api.imap.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.api.imap.message.request.DayMonthYear;
import org.apache.james.api.imap.message.request.SearchKey;
import org.apache.james.imapserver.codec.decode.ImapRequestLineReader;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class SearchCommandParserOrTest extends MockObjectTestCase {
    SearchCommandParser parser;
    Mock mockCommandFactory;
    Mock mockMessageFactory;
    Mock mockCommand;
    Mock mockMessage;
    ImapCommand command;
    ImapMessage message;
    
    protected void setUp() throws Exception {
        super.setUp();
        parser = new SearchCommandParser();
        mockCommandFactory = mock(Imap4Rev1CommandFactory.class);
        mockCommandFactory.expects(once()).method("getSearch");
        mockMessageFactory = mock(Imap4Rev1MessageFactory.class);
        mockCommand = mock(ImapCommand.class);
        command = (ImapCommand) mockCommand.proxy();
        mockMessage = mock(ImapMessage.class);
        message = (ImapMessage) mockMessage.proxy();
        parser.init((Imap4Rev1CommandFactory) mockCommandFactory.proxy());
        parser.setMessageFactory((Imap4Rev1MessageFactory) mockMessageFactory.proxy());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public Input sequence() {
        IdRange[] range = {new IdRange(Long.MAX_VALUE, 100), new IdRange(110), new IdRange(200, 201),
                new IdRange(400, Long.MAX_VALUE)};
        SearchKey key = SearchKey.buildSequenceSet(range);
        return new Input("*:100,110,200:201,400:*", key);
    }
    
    public Input uid() {
        IdRange[] range = {new IdRange(Long.MAX_VALUE, 100), new IdRange(110), new IdRange(200, 201),
                new IdRange(400, Long.MAX_VALUE)};
        SearchKey key = SearchKey.buildUidSet(range);
        return new Input("UID *:100,110,200:201,400:*", key);
    }
    
    public Input header() {
        SearchKey key = SearchKey.buildHeader("FROM", "Smith");
        return new Input("HEADER FROM Smith", key);
    }
    
    public Input date() {
        SearchKey key = SearchKey.buildSince(new DayMonthYear(11,1, 2001));
        return new Input("since 11-Jan-2001", key);
    }
    
    public Input stringUnquoted() {
        SearchKey key = SearchKey.buildFrom("Smith");
        return new Input("FROM Smith", key);
    }
    
    public Input stringQuoted() {
        SearchKey key = SearchKey.buildFrom("Smith And Jones");
        return new Input("FROM \"Smith And Jones\"", key);
    }
    
    public Input draft() {
        SearchKey key = SearchKey.buildDraft();
        return new Input("DRAFT", key);
    }
    
    public void testDraftPermutations() throws Exception {
        checkValid(draft(), draft());
        checkValid(draft(), stringQuoted());
        checkValid(draft(), stringUnquoted());
        checkValid(draft(), sequence());
        checkValid(draft(), header());
        checkValid(draft(), date());
        checkValid(draft(), uid());
    }
    
    public void testDatePermutations() throws Exception {
        checkValid(date(), draft());
        checkValid(date(), stringQuoted());
        checkValid(date(), stringUnquoted());
        checkValid(date(), sequence());
        checkValid(date(), header());
        checkValid(date(), date());
        checkValid(date(), uid());
    }
    
    public void testHeaderPermutations() throws Exception {
        checkValid(header(), draft());
        checkValid(header(), stringQuoted());
        checkValid(header(), stringUnquoted());
        checkValid(header(), sequence());
        checkValid(header(), header());
        checkValid(header(), date());
        checkValid(header(), uid());
    }
    
    public void testSequencePermutations() throws Exception {
        checkValid(sequence(), draft());
        checkValid(sequence(), stringQuoted());
        checkValid(sequence(), stringUnquoted());
        checkValid(sequence(), sequence());
        checkValid(sequence(), header());
        checkValid(sequence(), date());
        checkValid(sequence(), uid());
    }
    
    public void testStringQuotedPermutations() throws Exception {
        checkValid(stringQuoted(), draft());
        checkValid(stringQuoted(), stringQuoted());
        checkValid(stringQuoted(), stringUnquoted());
        checkValid(stringQuoted(), sequence());
        checkValid(stringQuoted(), header());
        checkValid(stringQuoted(), date());
        checkValid(stringQuoted(), uid());
    }
    
    public void testStringUnquotedPermutations() throws Exception {
        checkValid(stringUnquoted(), draft());
        checkValid(stringUnquoted(), stringQuoted());
        checkValid(stringUnquoted(), stringUnquoted());
        checkValid(stringUnquoted(), sequence());
        checkValid(stringUnquoted(), header());
        checkValid(stringUnquoted(), date());
        checkValid(stringUnquoted(), uid());
    }
    
    public void testUidPermutations() throws Exception {
        checkValid(uid(), draft());
        checkValid(uid(), stringQuoted());
        checkValid(uid(), stringUnquoted());
        checkValid(uid(), sequence());
        checkValid(uid(), header());
        checkValid(uid(), date());
        checkValid(uid(), uid());
    }
    
    private void checkValid(Input one, Input two) throws Exception {
        String input = "OR " + one.input + " " + two.input + "\r\n";
        SearchKey key = SearchKey.buildOr(one.key, two.key);
        ImapRequestLineReader reader = new ImapRequestLineReader(new ByteArrayInputStream(input.getBytes("US-ASCII")), 
                new ByteArrayOutputStream());

        assertEquals(key, parser.searchKey(reader, null, false));
    }
    
    public class Input {
        public String input;
        public SearchKey key;
        
        public Input(String input, SearchKey key) {
            super();
            this.input = input;
            this.key = key;
        }
    }
}
