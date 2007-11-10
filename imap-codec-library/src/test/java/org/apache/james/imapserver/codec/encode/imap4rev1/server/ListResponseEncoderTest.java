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

package org.apache.james.imapserver.codec.encode.imap4rev1.server;

import org.apache.james.api.imap.ImapMessage;
import org.apache.james.imap.message.response.imap4rev1.server.SearchResponse;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class ListResponseEncoderTest extends MockObjectTestCase {

    private static final long[] IDS = {1, 4, 9, 16};
    
    SearchResponse response;
    SearchResponseEncoder encoder;
    Mock mockNextEncoder;
    Mock composer;
    
    protected void setUp() throws Exception {
        super.setUp();
        mockNextEncoder = mock(ImapEncoder.class);
        composer = mock(ImapResponseComposer.class);    
        response = new SearchResponse(IDS);
        encoder = new SearchResponseEncoder((ImapEncoder) mockNextEncoder.proxy());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testIsAcceptable() {
        assertTrue(encoder.isAcceptable(response));
        assertFalse(encoder.isAcceptable((ImapMessage) mock(ImapMessage.class).proxy()));
        assertFalse(encoder.isAcceptable(null));
    }
    
    public void testEncode() throws Exception {
        composer.expects(once()).method("searchResponse").with(same(IDS));      
        encoder.encode(response, (ImapResponseComposer) composer.proxy());
    }
}
