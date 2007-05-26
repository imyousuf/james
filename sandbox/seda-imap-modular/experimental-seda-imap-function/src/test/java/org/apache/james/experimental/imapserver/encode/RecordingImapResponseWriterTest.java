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
package org.apache.james.experimental.imapserver.encode;

import org.apache.james.experimental.imapserver.ImapResponse;
import org.apache.james.experimental.imapserver.MockImapResponseWriter;
import org.apache.james.experimental.imapserver.MockImapSession;
import org.apache.james.experimental.imapserver.encode.RecordingImapResponseWriter;

import junit.framework.TestCase;

public class RecordingImapResponseWriterTest extends TestCase {

    RecordingImapResponseWriter writer;
    MockImapResponseWriter out;
    ImapResponse response;
    
    protected void setUp() throws Exception {
        super.setUp();
        out = new MockImapResponseWriter();
        response = new ImapResponse(out);
        writer = new RecordingImapResponseWriter();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testCommandName() {
        String name = "A command name";
        writer.commandName(name);
        writer.encode(response, null);
        assertEquals("Reply response", 1, out.operations.size());
        assertEquals("Reply response", new MockImapResponseWriter.CommandNameOperation(name), 
                out.operations.get(0));
    }

    public void testEnd() {
        writer.end();
        writer.encode(response, null);
        assertEquals("Reply response", 1, out.operations.size());
        assertEquals("Reply response", new MockImapResponseWriter.EndOperation(), 
                out.operations.get(0));
    }

    public void testMessageString() {
        String message = "A message";
        writer.message(message);
        writer.encode(response, null);
        assertEquals("Reply response", 1, out.operations.size());
        assertEquals("Reply response", new MockImapResponseWriter.TextMessageOperation(message), 
                out.operations.get(0));
    }

    public void testMessageInt() {
        int message = 42;
        writer.message(message);
        writer.encode(response, null);
        assertEquals("Reply response", 1, out.operations.size());
        assertEquals("Reply response", new MockImapResponseWriter.NumericMessageOperation(message), 
                out.operations.get(0));
    }

    public void testResponseCode() {
        String code = "A response code";
        writer.responseCode(code);
        writer.encode(response, null);
        assertEquals("Reply response", 1, out.operations.size());
        assertEquals("Reply response", new MockImapResponseWriter.ResponseCodeOperation(code), 
                out.operations.get(0));
    }

    public void testTag() {
        String tag = "A tag";
        writer.tag(tag);
        writer.encode(response, null);
        assertEquals("Reply response", 1, out.operations.size());
        assertEquals("Reply response", new MockImapResponseWriter.TagOperation(tag), 
                out.operations.get(0));
    }

    public void testUntagged() {
        writer.untagged();
        writer.encode(response, null);
        assertEquals("Reply response", 1, out.operations.size());
        assertEquals("Reply response", new MockImapResponseWriter.UntaggedOperation(), 
                out.operations.get(0));
    }

}
