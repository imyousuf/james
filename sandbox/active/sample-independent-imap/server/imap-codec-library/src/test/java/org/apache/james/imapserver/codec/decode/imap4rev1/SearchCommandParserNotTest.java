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

public class SearchCommandParserNotTest extends MockObjectTestCase {

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

    public void testShouldParseNotSequence() throws Exception {
        IdRange[] range = {new IdRange(Long.MAX_VALUE, 100), new IdRange(110), new IdRange(200, 201),
                new IdRange(400, Long.MAX_VALUE)};
        SearchKey notdKey = SearchKey.buildSequenceSet(range);
        SearchKey key = SearchKey.buildNot(notdKey);
        checkValid("NOT *:100,110,200:201,400:*\r\n", key);
    }
    
    public void testShouldParseNotUid() throws Exception {
        IdRange[] range = {new IdRange(Long.MAX_VALUE, 100), new IdRange(110), new IdRange(200, 201),
                new IdRange(400, Long.MAX_VALUE)};
        SearchKey notdKey = SearchKey.buildUidSet(range);
        SearchKey key = SearchKey.buildNot(notdKey);
        checkValid("NOT UID *:100,110,200:201,400:*\r\n", key);
    }
    
    public void testShouldParseNotHeaderKey() throws Exception {
        SearchKey notdKey = SearchKey.buildHeader("FROM", "Smith");
        SearchKey key = SearchKey.buildNot(notdKey);
        checkValid("NOT HEADER FROM Smith\r\n", key);
        checkValid("NOT header FROM Smith\r\n", key);
    }
    
    public void testShouldParseNotDateParameterKey() throws Exception {
        SearchKey notdKey = SearchKey.buildSince(new DayMonthYear(11,1, 2001));
        SearchKey key = SearchKey.buildNot(notdKey);
        checkValid("NOT since 11-Jan-2001\r\n", key);
        checkValid("NOT SINCE 11-Jan-2001\r\n", key);
    }

    public void testShouldParseNotStringParameterKey() throws Exception {
        SearchKey notdKey = SearchKey.buildFrom("Smith");
        SearchKey key = SearchKey.buildNot(notdKey);
        checkValid("NOT FROM Smith\r\n", key);
        checkValid("NOT FROM \"Smith\"\r\n", key);
    }
    
    public void testShouldParseNotStringQuotedParameterKey() throws Exception {
        SearchKey notdKey = SearchKey.buildFrom("Smith And Jones");
        SearchKey key = SearchKey.buildNot(notdKey);
        checkValid("NOT FROM \"Smith And Jones\"\r\n", key);
    }
    
    public void testShouldParseNotNoParameterKey() throws Exception {
        SearchKey notdKey = SearchKey.buildNew();
        SearchKey key = SearchKey.buildNot(notdKey);
        checkValid("NOT NEW\r\n", key);
        checkValid("Not NEW\r\n", key);
        checkValid("not new\r\n", key);
    }
    
    private void checkValid(String input, final SearchKey key) throws Exception {
        ImapRequestLineReader reader = new ImapRequestLineReader(new ByteArrayInputStream(input.getBytes("US-ASCII")), 
                new ByteArrayOutputStream());

        assertEquals(key, parser.searchKey(reader, null, false));
    }
}
