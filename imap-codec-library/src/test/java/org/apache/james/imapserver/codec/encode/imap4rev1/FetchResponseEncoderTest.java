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

import javax.mail.Flags;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class FetchResponseEncoderTest extends MockObjectTestCase {

    Flags flags;
    ImapResponseComposer composer;
    Mock mockComposer;
    Mock mockNextEncoder;
    FetchResponseEncoder encoder;
    Mock mockCommand;
    
    protected void setUp() throws Exception {
        super.setUp();
        mockComposer = mock(ImapResponseComposer.class);
        composer = (ImapResponseComposer) mockComposer.proxy();
        mockNextEncoder = mock(ImapEncoder.class);
        encoder = new FetchResponseEncoder((ImapEncoder) mockNextEncoder.proxy());
        mockCommand = mock(ImapCommand.class);
        flags = new Flags(Flags.Flag.DELETED);
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testShouldNotAcceptUnknownResponse() throws Exception {
        assertFalse(encoder.isAcceptable((ImapMessage)mock(ImapMessage.class).proxy()));
    }
    
    public void testShouldAcceptFetchResponse() throws Exception {
        assertTrue(encoder.isAcceptable(new FetchResponse(11, null, null)));
    }
    
    public void testShouldEncodeFlagsResponse() throws Exception {
        FetchResponse message = new FetchResponse(100, flags, null);
        mockComposer.expects(once()).method("openFetchResponse").with(eq(100L));
        mockComposer.expects(once()).method("flags").with(eq(flags));
        mockComposer.expects(once()).method("closeFetchResponse");
        encoder.doEncode(message, composer);
    }
    
    public void testShouldEncodeUidResponse() throws Exception {
        FetchResponse message = new FetchResponse(100, null, new Long(72));
        mockComposer.expects(once()).method("openFetchResponse").with(eq(100L));
        mockComposer.expects(once()).method("message").with(eq("UID"));
        mockComposer.expects(once()).method("message").with(eq(72L));
        mockComposer.expects(once()).method("closeFetchResponse");
        encoder.doEncode(message, composer);
    }
    
    public void testShouldEncodeAllResponse() throws Exception {
        FetchResponse message = new FetchResponse(100, flags, new Long(72));
        mockComposer.expects(once()).method("openFetchResponse").with(eq(100L));
        mockComposer.expects(once()).method("flags").with(eq(flags));
        mockComposer.expects(once()).method("message").with(eq("UID"));
        mockComposer.expects(once()).method("message").with(eq(72L));
        mockComposer.expects(once()).method("closeFetchResponse");
        encoder.doEncode(message, composer);
    }
}
