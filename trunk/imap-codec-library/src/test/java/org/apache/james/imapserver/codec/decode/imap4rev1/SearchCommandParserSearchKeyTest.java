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
import java.util.Date;

import javax.mail.Flags;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.ProtocolException;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.api.imap.message.request.SearchKey;
import org.apache.james.api.imap.message.request.DayMonthYear;
import org.apache.james.imapserver.codec.decode.ImapRequestLineReader;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.Constraint;

public class SearchCommandParserSearchKeyTest extends MockObjectTestCase {

    private static final DayMonthYear DATE = new DayMonthYear(1, 1, 2000);
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

    public void testShouldParseAll() throws Exception {
        SearchKey key = SearchKey.buildAll();
        checkValid("ALL\r\n", key);
        checkValid("all\r\n", key);
        checkValid("alL\r\n", key);
        checkInvalid("al\r\n", key);
        checkInvalid("alm\r\n", key);
        checkInvalid("alm\r\n", key);
    }
    
    public void testShouldParseAnswered() throws Exception {
        SearchKey key = SearchKey.buildAnswered();
        checkValid("ANSWERED\r\n", key);
        checkValid("answered\r\n", key);
        checkValid("aNSWEred\r\n", key);
        checkInvalid("a\r\n", key);
        checkInvalid("an\r\n", key);
        checkInvalid("ans\r\n", key);
        checkInvalid("answ\r\n", key);
        checkInvalid("answe\r\n", key);
        checkInvalid("answer\r\n", key);
        checkInvalid("answere\r\n", key);
    }
    
    public void testShouldParseBcc() throws Exception {
        SearchKey key = SearchKey.buildBcc("Somebody");
        checkValid("BCC Somebody\r\n", key);
        checkValid("BCC \"Somebody\"\r\n", key);
        checkValid("bcc Somebody\r\n", key);
        checkValid("bcc \"Somebody\"\r\n", key);
        checkValid("Bcc Somebody\r\n", key);
        checkValid("Bcc \"Somebody\"\r\n", key);
        checkInvalid("b\r\n", key);
        checkInvalid("bc\r\n", key);
        checkInvalid("bg\r\n", key);
    }
    
    public void testShouldParseBefore() throws Exception {
        SearchKey key = SearchKey.buildBefore(DATE);
        checkValid("BEFORE 1-Jan-2000\r\n", key);
        checkValid("before 1-Jan-2000\r\n", key);
        checkValid("BEforE 1-Jan-2000\r\n", key);
        checkInvalid("b\r\n", key);
        checkInvalid("B\r\n", key);
        checkInvalid("BE\r\n", key);
        checkInvalid("BEf\r\n", key);
        checkInvalid("BEfo\r\n", key);
        checkInvalid("BEfor\r\n", key);
        checkInvalid("BEforE\r\n", key);
        checkInvalid("BEforE \r\n", key);
        checkInvalid("BEforE 1\r\n", key);
        checkInvalid("BEforE 1-\r\n", key);
        checkInvalid("BEforE 1-J\r\n", key);
        checkInvalid("BEforE 1-Ja\r\n", key);
        checkInvalid("BEforE 1-Jan\r\n", key);
        checkInvalid("BEforE 1-Jan-\r\n", key);
    }

    private void checkValid(String input, final SearchKey key) throws Exception {
        ImapRequestLineReader reader = new ImapRequestLineReader(new ByteArrayInputStream(input.getBytes("US-ASCII")), 
                new ByteArrayOutputStream());

        assertEquals(key, parser.searchKey(reader));
    }


    private void checkInvalid(String input, final SearchKey key) throws Exception {
        ImapRequestLineReader reader = new ImapRequestLineReader(new ByteArrayInputStream(input.getBytes("US-ASCII")), 
                new ByteArrayOutputStream());

        try {
            parser.searchKey(reader);
            fail("Expected protocol exception to be throw since input is invalid");
        } catch (ProtocolException e) {
            //expected
        }
    }
}
