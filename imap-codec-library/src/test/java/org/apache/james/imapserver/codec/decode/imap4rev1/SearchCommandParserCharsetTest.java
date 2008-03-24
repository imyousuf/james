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
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.api.imap.message.request.SearchKey;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponseFactory;
import org.apache.james.imapserver.codec.decode.ImapRequestLineReader;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class SearchCommandParserCharsetTest extends MockObjectTestCase {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Charset ASCII = Charset.forName("US-ASCII");
    private static final String TAG = "A1";
    private static final String ASCII_SEARCH_TERM = "A Search Term";
    private static final String NON_ASCII_SEARCH_TERM = "\u043A\u0430\u043A \u0414\u0435\u043B\u0430?";
    private static final byte[] BYTES_NON_ASCII_SEARCH_TERM = NioUtils.toBytes(NON_ASCII_SEARCH_TERM, UTF8); 
    private static final byte[] BYTES_UTF8_NON_ASCII_SEARCH_TERM = NioUtils.add(NioUtils.toBytes(" {16}\r\n", ASCII), BYTES_NON_ASCII_SEARCH_TERM);
    private static final byte[] CHARSET = NioUtils.toBytes("CHARSET UTF-8 ", ASCII);
    
    SearchCommandParser parser;
    Mock mockStatusResponseFactory;
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
        mockStatusResponseFactory = mock(StatusResponseFactory.class);
        message = (ImapMessage) mockMessage.proxy();
        parser.init((Imap4Rev1CommandFactory) mockCommandFactory.proxy());
        parser.setMessageFactory((Imap4Rev1MessageFactory) mockMessageFactory.proxy());
        parser.setStatusResponseFactory((StatusResponseFactory) mockStatusResponseFactory.proxy());
        parser.enableLogging(new MockLogger());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testBadCharset() throws Exception {
        Collection charsetNames = new HashSet();
        for (final Iterator it = Charset.availableCharsets().values().iterator(); it.hasNext();) {
            final Charset charset = (Charset) it.next();
            final Set aliases = charset.aliases();
            charsetNames.addAll(aliases);
        }
        mockStatusResponseFactory.expects(once()).method("taggedNo").with(eq(TAG), same(command), eq(HumanReadableTextKey.BAD_CHARSET), eq(StatusResponse.ResponseCode.badCharset(charsetNames)));
        ImapRequestLineReader reader = new ImapRequestLineReader(new ByteArrayInputStream("CHARSET BOGUS ".getBytes("US-ASCII")), 
                new ByteArrayOutputStream());
        parser.decode(command, reader, TAG, false);
    }
    
    public void testBCCShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey.buildBcc(NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("BCC".getBytes("US-ASCII"), key);
    }
    
    public void testBODYShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey.buildBody(NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("BODY".getBytes("US-ASCII"), key);
    }
    
    public void testCCShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey.buildCc(NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("CC".getBytes("US-ASCII"), key);   
    }
    
    public void testFROMShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey.buildFrom(NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("FROM".getBytes("US-ASCII"), key);
    }
    
    public void testHEADERShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey.buildHeader("whatever", NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("HEADER whatever".getBytes("US-ASCII"), key);
    }
    
    public void testSUBJECTShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey.buildSubject(NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("SUBJECT".getBytes("US-ASCII"), key);
    }
    
    public void testTEXTShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey.buildText(NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("TEXT".getBytes("US-ASCII"), key);
    }
    
    public void testTOShouldConvertCharset() throws Exception {
        SearchKey key = SearchKey.buildTo(NON_ASCII_SEARCH_TERM);
        checkUTF8Valid("TO".getBytes("US-ASCII"), key);
    }
    
    public void testASCIICharset() throws Exception {
        SearchKey key = SearchKey.buildBcc(ASCII_SEARCH_TERM);
        checkValid("CHARSET US-ASCII BCC \"" + ASCII_SEARCH_TERM + "\"", key, true, "US-ASCII");
    }
    
    public void testSimpleUTF8Charset() throws Exception {
        SearchKey key = SearchKey.buildBcc(ASCII_SEARCH_TERM);
        checkValid("CHARSET UTF-8 BCC \"" + ASCII_SEARCH_TERM + "\"", key, true, "US-ASCII");
    }
    
    private void checkUTF8Valid(byte[] term, final SearchKey key) throws Exception {
        ImapRequestLineReader reader = new ImapRequestLineReader(new ByteArrayInputStream(NioUtils.add(NioUtils.add(CHARSET, term), BYTES_UTF8_NON_ASCII_SEARCH_TERM)), 
                new ByteArrayOutputStream());
        final SearchKey searchKey = parser.searchKey(reader, null, true);
        assertEquals(key, searchKey);
    }
    
    private void checkValid(String input, final SearchKey key, boolean isFirst, String charset) throws Exception {
        ImapRequestLineReader reader = new ImapRequestLineReader(new ByteArrayInputStream(input.getBytes(charset)), 
                new ByteArrayOutputStream());

        final SearchKey searchKey = parser.searchKey(reader, null, isFirst);
        assertEquals(key, searchKey);
    }
    
}
