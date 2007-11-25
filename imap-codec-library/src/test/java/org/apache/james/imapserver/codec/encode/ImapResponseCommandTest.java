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

package org.apache.james.imapserver.codec.encode;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.imapserver.codec.encode.base.ImapResponseComposerImpl;
import org.apache.james.imapserver.codec.encode.imap4rev1.legacy.MockImapResponseWriter;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class ImapResponseCommandTest extends MockObjectTestCase {

    private static final String TAG = "TAG"; 
    private static final String NAME = "NAME";
    
    ImapResponseComposer response;
    MockImapResponseWriter writer;
    Mock mockCommand;
    ImapCommand command;
    
    protected void setUp() throws Exception {
        super.setUp();
        mockCommand = mock(ImapCommand.class);
        mockCommand.expects(atLeastOnce()).method("getName").will(returnValue(NAME));
        command = (ImapCommand) mockCommand.proxy();
        writer = new MockImapResponseWriter();
        response = new ImapResponseComposerImpl(writer);;
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCommandCompleteImapCommand() throws Exception {
        response.commandComplete(command, TAG);
        assertEquals(5, writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.OK), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.CommandNameOperation(NAME),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.TextMessageOperation("completed."),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(4));
    }

    public void testCommandCompleteImapCommandString() throws Exception  {
        final String code = "responseCode";
        response.commandComplete(command, code, TAG);
        assertEquals(6, writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.OK), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.ResponseCodeOperation(code),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.CommandNameOperation(NAME),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.TextMessageOperation("completed."),
                writer.operations.get(4));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(5));
    }

    public void testCommandFailedImapCommandString() throws Exception {
        final String reason = "A reason";
        response.commandFailed(command, reason, TAG);
        assertEquals(6, writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.NO), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.CommandNameOperation(NAME),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapResponseComposerImpl.FAILED),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(reason), 
                writer.operations.get(4));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(5));
    }

    public void testCommandFailedImapCommandStringString() throws Exception {
        final String reason = "A reason";
        final String code = "A code";
        response.commandFailed(command, code, reason, TAG);
        assertEquals(7, writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.NO), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.ResponseCodeOperation(code),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.CommandNameOperation(NAME),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapResponseComposerImpl.FAILED),
                writer.operations.get(4));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(reason), 
                writer.operations.get(5));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(6));
    }

    public void testCommandResponse() throws Exception {
        String message = "A message";
        response.commandResponse(command, message);
        assertEquals(4, writer.operations.size());
        assertEquals(new MockImapResponseWriter.UntaggedOperation(), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.CommandNameOperation(command.getName()),
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(message),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(3));
        
    }
}
