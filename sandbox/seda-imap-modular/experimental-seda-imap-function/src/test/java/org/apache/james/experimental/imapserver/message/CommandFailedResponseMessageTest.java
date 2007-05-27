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

import org.apache.james.api.imap.ImapConstants;
import org.apache.james.experimental.imapserver.ImapResponse;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.MockImapResponseWriter;
import org.apache.james.experimental.imapserver.MockImapSession;
import org.apache.james.experimental.imapserver.commands.MockCommand;
import org.apache.james.experimental.imapserver.message.response.imap4rev1.CommandFailedResponse;

import junit.framework.TestCase;

public class CommandFailedResponseMessageTest extends TestCase {

    private static final String TAG = "A Tag";
    
    MockImapResponseWriter writer;
    ImapResponse response;
    MockCommand command;
    ImapSession session;
    
    protected void setUp() throws Exception {
        super.setUp();
        writer = new MockImapResponseWriter();
        response = new ImapResponse(writer);
        command = new MockCommand();
        session = new MockImapSession();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testWithResponseCode() throws Exception {
        String code = "A code";
        String message = "A message";
        CommandFailedResponse responseMessage 
            = new CommandFailedResponse(command, code, message, TAG);
        responseMessage.encode(response, session);
        assertEquals(7, writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.NO), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.ResponseCodeOperation(code),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.CommandNameOperation(MockCommand.NAME),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapResponse.FAILED),
                writer.operations.get(4));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(message), 
                writer.operations.get(5));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(6));
    }
    
    public void testWithoutResponseCode() throws Exception {
        String message = "A message";
        CommandFailedResponse responseMessage 
            = new CommandFailedResponse(command, message, TAG);
        responseMessage.encode(response, session);
        assertEquals(6, writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.NO), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.CommandNameOperation(MockCommand.NAME),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapResponse.FAILED),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(message), 
                writer.operations.get(4));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(5));
    }
}
