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
import org.apache.james.api.imap.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.api.imap.message.request.DayMonthYear;
import org.apache.james.api.imap.message.request.SearchKey;
import org.apache.james.imapserver.codec.decode.ImapRequestLineReader;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

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
        checkInvalid("bccc\r\n", key);
    }
    
    public void testShouldParseOn() throws Exception {
        SearchKey key = SearchKey.buildOn(DATE);
        checkValid("ON 1-Jan-2000\r\n", key);
        checkValid("on 1-Jan-2000\r\n", key);
        checkValid("oN 1-Jan-2000\r\n", key);
        checkInvalid("o\r\n", key);
        checkInvalid("om\r\n", key);
        checkInvalid("oni\r\n", key);
        checkInvalid("on \r\n", key);
        checkInvalid("on 1\r\n", key);
        checkInvalid("on 1-\r\n", key);
        checkInvalid("on 1-J\r\n", key);
        checkInvalid("on 1-Ja\r\n", key);
        checkInvalid("on 1-Jan\r\n", key);
        checkInvalid("on 1-Jan-\r\n", key);
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
        checkInvalid("BEforEi\r\n", key);
        checkInvalid("BEforE \r\n", key);
        checkInvalid("BEforE 1\r\n", key);
        checkInvalid("BEforE 1-\r\n", key);
        checkInvalid("BEforE 1-J\r\n", key);
        checkInvalid("BEforE 1-Ja\r\n", key);
        checkInvalid("BEforE 1-Jan\r\n", key);
        checkInvalid("BEforE 1-Jan-\r\n", key);
    }
    
    public void testShouldParseBody() throws Exception {
        SearchKey key = SearchKey.buildBody("Text");
        checkValid("BODY Text\r\n", key);
        checkValid("BODY \"Text\"\r\n", key);
        checkValid("body Text\r\n", key);
        checkValid("body \"Text\"\r\n", key);
        checkValid("BodY Text\r\n", key);
        checkValid("BodY \"Text\"\r\n", key);
        checkInvalid("b\r\n", key);
        checkInvalid("Bo\r\n", key);
        checkInvalid("Bod\r\n", key);
        checkInvalid("Bodd\r\n", key);
        checkInvalid("Bodym\r\n", key);   
    }
    
    
    public void testShouldParseCc() throws Exception {
        SearchKey key = SearchKey.buildCc("SomeText");
        checkValid("CC SomeText\r\n", key);
        checkValid("CC \"SomeText\"\r\n", key);
        checkValid("cc SomeText\r\n", key);
        checkValid("cc \"SomeText\"\r\n", key);
        checkValid("Cc SomeText\r\n", key);
        checkValid("Cc \"SomeText\"\r\n", key);
        checkInvalid("c\r\n", key);
        checkInvalid("cd\r\n", key);
        checkInvalid("ccc\r\n", key);
    }

    public void testShouldParseFrom() throws Exception {
        SearchKey key = SearchKey.buildFrom("Someone");
        checkValid("FROM Someone\r\n", key);
        checkValid("FROM \"Someone\"\r\n", key);
        checkValid("from Someone\r\n", key);
        checkValid("from \"Someone\"\r\n", key);
        checkValid("FRom Someone\r\n", key);
        checkValid("FRom \"Someone\"\r\n", key);
        checkInvalid("f\r\n", key);
        checkInvalid("fr\r\n", key);
        checkInvalid("ftom\r\n", key);
        checkInvalid("froml\r\n", key);
    }
    
    public void testShouldParseKeyword() throws Exception {
        SearchKey key = SearchKey.buildKeyword("AFlag");
        checkValid("KEYWORD AFlag\r\n", key);
        checkInvalid("KEYWORD \"AFlag\"\r\n", key);
        checkValid("keyword AFlag\r\n", key);
        checkInvalid("keyword \"AFlag\"\r\n", key);
        checkValid("KEYword AFlag\r\n", key);
        checkInvalid("KEYword \"AFlag\"\r\n", key);
        checkInvalid("k\r\n", key);
        checkInvalid("ke\r\n", key);
        checkInvalid("key\r\n", key);
        checkInvalid("keyw\r\n", key);
        checkInvalid("keywo\r\n", key);
        checkInvalid("keywor\r\n", key);
        checkInvalid("keywordi\r\n", key);
        checkInvalid("keyword \r\n", key);
    }
    

    public void testShouldParseHeader() throws Exception {
        SearchKey key = SearchKey.buildHeader("Field", "Value");
        checkValid("HEADER Field Value\r\n", key);
        checkValid("HEADER \"Field\" \"Value\"\r\n", key);
        checkValid("header Field Value\r\n", key);
        checkValid("header \"Field\" \"Value\"\r\n", key);
        checkValid("HEAder Field Value\r\n", key);
        checkValid("HEAder \"Field\" \"Value\"\r\n", key);
        checkInvalid("h\r\n", key);
        checkInvalid("he\r\n", key);
        checkInvalid("hea\r\n", key);
        checkInvalid("head\r\n", key);
        checkInvalid("heade\r\n", key);
        checkInvalid("header\r\n", key);
        checkInvalid("header field\r\n", key);
        checkInvalid("header field \r\n", key);
    }
    
    private void checkValid(String input, final SearchKey key) throws Exception {
        ImapRequestLineReader reader = new ImapRequestLineReader(new ByteArrayInputStream(input.getBytes("US-ASCII")), 
                new ByteArrayOutputStream());

        assertEquals(key, parser.searchKey(reader));
    }

    public void testShouldParseDeleted() throws Exception {
        SearchKey key = SearchKey.buildDeleted();
        checkValid("DELETED\r\n", key);
        checkValid("deleted\r\n", key);
        checkValid("deLEteD\r\n", key);
        checkInvalid("d\r\n", key);
        checkInvalid("de\r\n", key);
        checkInvalid("del\r\n", key);
        checkInvalid("dele\r\n", key);
        checkInvalid("delet\r\n", key);
        checkInvalid("delete\r\n", key);
    }

    public void testEShouldBeInvalid() throws Exception {
        SearchKey key = SearchKey.buildDeleted();
        checkInvalid("e\r\n", key);
        checkInvalid("ee\r\n", key);
    }
    
    public void testShouldParseDraft() throws Exception {
        SearchKey key = SearchKey.buildDraft();
        checkValid("DRAFT\r\n", key);
        checkValid("draft\r\n", key);
        checkValid("DRaft\r\n", key);
        checkInvalid("D\r\n", key);
        checkInvalid("DR\r\n", key);
        checkInvalid("DRA\r\n", key);
        checkInvalid("DRAF\r\n", key);
    }
    
    public void testShouldParseNew() throws Exception {
        SearchKey key = SearchKey.buildNew();
        checkValid("NEW\r\n", key);
        checkValid("new\r\n", key);
        checkValid("NeW\r\n", key);
        checkInvalid("n\r\n", key);
        checkInvalid("ne\r\n", key);
        checkInvalid("nwe\r\n", key);
    }
    
    public void testShouldParseOld() throws Exception {
        SearchKey key = SearchKey.buildOld();
        checkValid("OLD\r\n", key);
        checkValid("old\r\n", key);
        checkValid("oLd\r\n", key);
        checkInvalid("o\r\n", key);
        checkInvalid("ol\r\n", key);
        checkInvalid("olr\r\n", key);
    }
    
    public void testShouldParseFlagged() throws Exception {
        SearchKey key = SearchKey.buildFlagged();
        checkValid("FLAGGED\r\n", key);
        checkValid("flagged\r\n", key);
        checkValid("FLAGged\r\n", key);
        checkInvalid("F\r\n", key);
        checkInvalid("FL\r\n", key);
        checkInvalid("FLA\r\n", key);
        checkInvalid("FLAG\r\n", key);
        checkInvalid("FLAGG\r\n", key);
        checkInvalid("FLAGGE\r\n", key);
        checkInvalid("FLoas\r\n", key);
    }
    

    public void testShouldParseLarger() throws Exception {
        SearchKey key = SearchKey.buildLarger(1234);
        checkValid("LARGER 1234\r\n", key);
        checkValid("larger 1234\r\n", key);
        checkValid("larger 1234\r\n", key);
        checkInvalid("l\r\n", key);
        checkInvalid("la\r\n", key);
        checkInvalid("lar\r\n", key);
        checkInvalid("larg\r\n", key);
        checkInvalid("large\r\n", key);
        checkInvalid("larger\r\n", key);
        checkInvalid("larger \r\n", key);
        checkInvalid("larger peach\r\n", key);
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
