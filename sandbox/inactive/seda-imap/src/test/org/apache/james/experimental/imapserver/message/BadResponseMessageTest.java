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
package org.apache.james.experimental.imapserver.message;

import org.apache.james.experimental.imapserver.ImapConstants;
import org.apache.james.experimental.imapserver.ImapResponse;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.MockImapResponseWriter;
import org.apache.james.experimental.imapserver.MockImapSession;
import org.apache.james.experimental.imapserver.message.BadResponseMessage;
import org.apache.james.experimental.imapserver.message.ImapResponseMessage;

import junit.framework.TestCase;

public class BadResponseMessageTest extends TestCase {

    private static final String MESSAGE = "A Message";
    
    MockImapResponseWriter writer;
    ImapResponse response;
    BadResponseMessage message;
    ImapSession session;
    
    protected void setUp() throws Exception {
        super.setUp();
        writer = new MockImapResponseWriter();
        response = new ImapResponse(writer);
        message = new BadResponseMessage(MESSAGE);
        session = new MockImapSession();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testProcessAndEncode() throws Exception {
            ImapResponseMessage response = message.process(session);
            assertNotNull("Response is required", response);
            response.encode(this.response, session);
            assertEquals(4, this.writer.operations.size());
            assertEquals(new MockImapResponseWriter.UntaggedOperation(), writer.operations.get(0));
            assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.BAD), 
                    writer.operations.get(1));
            assertEquals(new MockImapResponseWriter.TextMessageOperation(MESSAGE),
                    writer.operations.get(2));
            assertEquals(new MockImapResponseWriter.EndOperation(), 
                    writer.operations.get(3));
    }

}
