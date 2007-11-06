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

import junit.framework.TestCase;

import org.apache.james.api.imap.ImapConstants;
import org.apache.james.imap.message.response.imap4rev1.legacy.BadResponse;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.apache.james.imapserver.codec.encode.base.ImapResponseComposerImpl;
import org.jmock.Mock;

public class BadResponseEncodeTest extends TestCase {

    private static final String MESSAGE = "A Message";
    
    MockImapResponseWriter writer;
    ImapResponseComposer response;
    BadResponse message;
    Mock mockNextEncoder;
    BadResponseEncoder encoder;
    
    protected void setUp() throws Exception {
        super.setUp();
        mockNextEncoder = new Mock(ImapEncoder.class);
        encoder = new BadResponseEncoder((ImapEncoder) mockNextEncoder.proxy());
        writer = new MockImapResponseWriter();
        response = new ImapResponseComposerImpl(writer);
        message = new BadResponse(MESSAGE);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testEncode() throws Exception {
        encoder.encode(message, this.response);
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
