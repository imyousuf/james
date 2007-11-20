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
import java.io.OutputStream;

import javax.mail.Flags;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1CommandFactory;
import org.apache.james.api.imap.imap4rev1.Imap4Rev1MessageFactory;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.imapserver.codec.decode.ImapRequestLineReader;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.Constraint;

public class StoreCommandParserTest extends MockObjectTestCase {

    StoreCommandParser parser;
    Mock mockCommandFactory;
    Mock mockMessageFactory;
    Mock mockCommand;
    Mock mockMessage;
    ImapCommand command;
    ImapMessage message;
    
    protected void setUp() throws Exception {
        super.setUp();
        parser = new StoreCommandParser();
        mockCommandFactory = mock(Imap4Rev1CommandFactory.class);
        mockCommandFactory.expects(once()).method("getStore");
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

    public void testShouldParseSilentDraftFlagged() throws Exception {
        IdRange[] ranges = {new IdRange(1)};
        Flags flags = new Flags();
        flags.add(Flags.Flag.DRAFT);
        flags.add(Flags.Flag.FLAGGED);
        check("1 FLAGS.SILENT (\\Draft \\Flagged)\r\n", ranges, true, null, flags, 
                false, "A01");
    }
    
    private void check(String input, final IdRange[] idSet, boolean silent, Boolean sign, 
            final Flags flags, final boolean useUids, String tag) throws Exception {
        ImapRequestLineReader reader = new ImapRequestLineReader(new ByteArrayInputStream(input.getBytes("US-ASCII")), 
                    new ByteArrayOutputStream());

        Constraint[] constraints = {eq(command), eq(idSet), eq(silent), eq(sign), 
                eq(flags), eq(useUids), same(tag)};
        mockMessageFactory.expects(once()).method("createStoreMessage").with(constraints).will(returnValue(message));
        parser.decode(command, reader, tag, useUids);
    }
}
