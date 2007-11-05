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

package org.apache.james.imapserver.codec.encode.imap4rev1;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.display.HumanReadableTextKey;
import org.apache.james.api.imap.message.response.imap4rev1.StatusResponse;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.apache.james.imapserver.codec.encode.ImapResponseComposerImpl;
import org.apache.james.imapserver.codec.encode.imap4rev1.legacy.MockImapResponseWriter;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class StatusResponseEncoderTest extends MockObjectTestCase {

    private static final String COMMAND = "COMMAND";
    private static final String TAG = "TAG";
    private static final HumanReadableTextKey KEY = new HumanReadableTextKey("KEY", "TEXT");
    
    MockImapResponseWriter writer;
    ImapResponseComposer response;
    Mock mockNextEncoder;
    Mock mockStatusResponse;
    StatusResponseEncoder encoder;
    Mock mockCommand;
    
    protected void setUp() throws Exception {
        super.setUp();
        writer = new MockImapResponseWriter();
        response = new ImapResponseComposerImpl(writer);
        mockNextEncoder = mock(ImapEncoder.class);
        mockStatusResponse = mock(StatusResponse.class);
        encoder = new StatusResponseEncoder((ImapEncoder) mockNextEncoder.proxy());
        mockCommand = mock(ImapCommand.class);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testTaggedOkCode() throws Exception {
        execute(StatusResponse.Type.OK, StatusResponse.ResponseCode.ALERT, KEY, TAG);
        assertEquals(6, this.writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.OK), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(StatusResponse.ResponseCode.ALERT.getCode()),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.CommandNameOperation(COMMAND),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(KEY.getDefaultValue()),
                writer.operations.get(4));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(5));        
    }
    
    public void testTaggedOkNoCode() throws Exception {
        execute(StatusResponse.Type.OK, null, KEY, TAG);
        assertEquals(5, this.writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.OK), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.CommandNameOperation(COMMAND),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(KEY.getDefaultValue()),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(4));        
    }

    public void testTaggedBadCode() throws Exception {
        execute(StatusResponse.Type.BAD, StatusResponse.ResponseCode.ALERT, KEY, TAG);
        assertEquals(6, this.writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.BAD), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(StatusResponse.ResponseCode.ALERT.getCode()),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.CommandNameOperation(COMMAND),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(KEY.getDefaultValue()),
                writer.operations.get(4));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(5));        
    }
    
    public void testTaggedBadNoCode() throws Exception {
        execute(StatusResponse.Type.BAD, null, KEY, TAG);
        assertEquals(5, this.writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.BAD), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.CommandNameOperation(COMMAND),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(KEY.getDefaultValue()),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(4));        
    }

    public void testTaggedNoCode() throws Exception {
        execute(StatusResponse.Type.NO, StatusResponse.ResponseCode.ALERT, KEY, TAG);
        assertEquals(6, this.writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.NO), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(StatusResponse.ResponseCode.ALERT.getCode()),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.CommandNameOperation(COMMAND),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(KEY.getDefaultValue()),
                writer.operations.get(4));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(5));        
    }
    
    public void testTaggedNoNoCode() throws Exception {
        execute(StatusResponse.Type.NO, null, KEY, TAG);
        assertEquals(5, this.writer.operations.size());
        assertEquals(new MockImapResponseWriter.TagOperation(TAG), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.NO), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.CommandNameOperation(COMMAND),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(KEY.getDefaultValue()),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(4));        
    }
    

    public void testUntaggedOkCode() throws Exception {
        execute(StatusResponse.Type.OK, StatusResponse.ResponseCode.ALERT, KEY, null);
        assertEquals(5, this.writer.operations.size());
        assertEquals(new MockImapResponseWriter.UntaggedOperation(), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.OK), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(StatusResponse.ResponseCode.ALERT.getCode()),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(KEY.getDefaultValue()),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(4));        
    }
    
    public void testUntaggedOkNoCode() throws Exception {
        execute(StatusResponse.Type.OK, null, KEY, null);
        assertEquals(4, this.writer.operations.size());
        assertEquals(new MockImapResponseWriter.UntaggedOperation(), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.OK), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(KEY.getDefaultValue()),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(3));        
    }

    public void testUntaggedBadCode() throws Exception {
        execute(StatusResponse.Type.BAD, StatusResponse.ResponseCode.ALERT, KEY, null);
        assertEquals(5, this.writer.operations.size());
        assertEquals(new MockImapResponseWriter.UntaggedOperation(), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.BAD), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(StatusResponse.ResponseCode.ALERT.getCode()),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(KEY.getDefaultValue()),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(4));        
    }
    
    public void testUntaggedBadNoCode() throws Exception {
        execute(StatusResponse.Type.BAD, null, KEY, null);
        assertEquals(4, this.writer.operations.size());
        assertEquals(new MockImapResponseWriter.UntaggedOperation(), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.BAD), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(KEY.getDefaultValue()),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(3));        
    }

    public void testUntaggedNoCode() throws Exception {
        execute(StatusResponse.Type.NO, StatusResponse.ResponseCode.ALERT, KEY, null);
        assertEquals(5, this.writer.operations.size());
        assertEquals(new MockImapResponseWriter.UntaggedOperation(), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.NO), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(StatusResponse.ResponseCode.ALERT.getCode()),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(KEY.getDefaultValue()),
                writer.operations.get(3));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(4));        
    }
    
    public void testUntaggedNoNoCode() throws Exception {
        execute(StatusResponse.Type.NO, null, KEY, null);
        assertEquals(4, this.writer.operations.size());
        assertEquals(new MockImapResponseWriter.UntaggedOperation(), writer.operations.get(0));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(ImapConstants.NO), 
                writer.operations.get(1));
        assertEquals(new MockImapResponseWriter.TextMessageOperation(KEY.getDefaultValue()),
                writer.operations.get(2));
        assertEquals(new MockImapResponseWriter.EndOperation(), 
                writer.operations.get(3));        
    }
    

    
    private void execute(StatusResponse.Type type, StatusResponse.ResponseCode code, 
            HumanReadableTextKey key, String tag) throws Exception {
        configure(type, code, key, tag);
        compose();
    }
    
    
    private void compose() throws Exception {
       encoder.doEncode((ImapMessage) mockStatusResponse.proxy(), response); 
    }
    
    private void configure(StatusResponse.Type type, StatusResponse.ResponseCode code, 
            HumanReadableTextKey key, String tag) {
        mockStatusResponse.expects(once()).method("getServerResponseType").will(returnValue(type));
        mockStatusResponse.expects(once()).method("getTag").will(returnValue(tag));
        mockStatusResponse.expects(once()).method("getTextKey").will(returnValue(key));
        mockStatusResponse.expects(once()).method("getResponseCode").will(returnValue(code));

        if (tag == null) {
            mockStatusResponse.expects(once()).method("getCommand").will(returnValue(null));
        } else {
            mockCommand.expects(once()).method("getName").will(returnValue(COMMAND));
            mockStatusResponse.expects(once()).method("getCommand").will(returnValue(mockCommand.proxy()));
        }
    }
}
