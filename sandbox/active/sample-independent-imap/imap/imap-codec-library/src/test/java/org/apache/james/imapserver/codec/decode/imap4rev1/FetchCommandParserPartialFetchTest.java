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
import org.apache.james.api.imap.message.BodyFetchElement;
import org.apache.james.api.imap.message.FetchData;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.imapserver.codec.ProtocolException;
import org.apache.james.imapserver.codec.decode.ImapRequestLineReader;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.Constraint;

public class FetchCommandParserPartialFetchTest extends MockObjectTestCase {

    FetchCommandParser parser;
    Mock mockCommandFactory;
    Mock mockMessageFactory;
    Mock mockCommand;
    Mock mockMessage;
    ImapCommand command;
    ImapMessage message;
    
    protected void setUp() throws Exception {
        super.setUp();
        parser = new FetchCommandParser();
        mockCommandFactory = mock(Imap4Rev1CommandFactory.class);
        mockCommandFactory.expects(once()).method("getFetch");
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

    public void testShouldParseZeroAndLength() throws Exception {
        IdRange[] ranges = {new IdRange(1)};
        FetchData data = new FetchData();
        data.add(new BodyFetchElement("BODY[]", BodyFetchElement.CONTENT, null, null, 
                new Long(0), new Long(100)), false);
        check("1 (BODY[]<0.100>)\r\n", ranges, false, data, "A01");
    }
    
    public void testShouldParseNonZeroAndLength() throws Exception {
        IdRange[] ranges = {new IdRange(1)};
        FetchData data = new FetchData();
        data.add(new BodyFetchElement("BODY[]", BodyFetchElement.CONTENT, null, null, 
                new Long(20), new Long(12342348)), false);
        check("1 (BODY[]<20.12342348>)\r\n", ranges, false, data, "A01");
    }
    
    public void testShouldNotParseZeroLength() throws Exception {
        try {
            ImapRequestLineReader reader = new ImapRequestLineReader(new ByteArrayInputStream("1 (BODY[]<20.0>)\r\n".getBytes("US-ASCII")), 
                    new ByteArrayOutputStream());        
            parser.decode(command, reader, "A01", false);
            fail("Number of octets must be non-zero");
        } catch (ProtocolException e) {
            // expected
        }
    }
    
    
    private void check(String input, final IdRange[] idSet, final boolean useUids, FetchData data, String tag) throws Exception {
        ImapRequestLineReader reader = new ImapRequestLineReader(new ByteArrayInputStream(input.getBytes("US-ASCII")), 
                    new ByteArrayOutputStream());        
        Constraint[] constraints = {eq(command), eq(useUids), eq(idSet), eq(data), same(tag)};
        mockMessageFactory.expects(once()).method("createFetchMessage").with(constraints).will(returnValue(message));
        parser.decode(command, reader, tag, useUids);
    }
}
