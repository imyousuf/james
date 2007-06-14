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

package org.apache.james.imapserver.codec.encode.imap4rev1.legacy;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.imap.message.response.imap4rev1.legacy.CommandFailedResponse;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class CommandFailedResponseEncoderTest extends MockObjectTestCase {

    private static final String TAG = "A Tag";

    private static final String NAME = "NAME";
    
    MockImapResponseWriter writer;
    ImapResponseComposer response;
    Mock command;
    Mock mockNextEncoder;
    CommandFailedResponseEncoder encoder;
    
    protected void setUp() throws Exception {
        super.setUp();
        writer = new MockImapResponseWriter();
        mockNextEncoder = mock(ImapEncoder.class);
        encoder = new CommandFailedResponseEncoder((ImapEncoder) mockNextEncoder.proxy());
        response = new ImapResponseComposer(writer);
        command = mock(ImapCommand.class);
        command.expects(atLeastOnce()).method("getName").will(returnValue(NAME));
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testWithResponseCode() throws Exception {
        String code = "A code";
        String message = "A message";
        CommandFailedResponse responseMessage 
            = new CommandFailedResponse((ImapCommand) command.proxy(), code, message, TAG);
        encoder.encode(responseMessage, response);
        assertEquals(7, writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.NO), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.ResponseCodeOperation(code),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.CommandNameOperation(NAME),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapResponseComposer.FAILED),
                writer.operations.get(4));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(message), 
                writer.operations.get(5));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(6));
    }
    
    public void testWithoutResponseCode() throws Exception {
        String message = "A message";
        CommandFailedResponse responseMessage 
            = new CommandFailedResponse((ImapCommand) command.proxy(), message, TAG);
        encoder.encode(responseMessage, response);
        assertEquals(6, writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.NO), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.CommandNameOperation(NAME),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapResponseComposer.FAILED),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(message), 
                writer.operations.get(4));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(5));
    }
}
