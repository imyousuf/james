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

import java.util.Arrays;

import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.imap.message.response.imap4rev1.server.LSubResponse;
import org.apache.james.imap.message.response.imap4rev1.server.ListResponse;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

public class LSubResponseEncoderTest extends MockObjectTestCase {

    LSubResponseEncoder encoder;
    Mock mockNextEncoder;
    Mock composer;
    
    protected void setUp() throws Exception {
        super.setUp();
        mockNextEncoder = mock(ImapEncoder.class);
        composer = mock(ImapResponseComposer.class);    
        encoder = new LSubResponseEncoder((ImapEncoder) mockNextEncoder.proxy());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testIsAcceptable() {
        assertFalse(encoder.isAcceptable(new ListResponse(true, true, true, true, ".", "name")));
        assertTrue(encoder.isAcceptable(new LSubResponse(true, true, true, true, ".", "name")));
        assertFalse(encoder.isAcceptable((ImapMessage) mock(ImapMessage.class).proxy()));
        assertFalse(encoder.isAcceptable(null));
    }
    
    public void testName() throws Exception {
        composer.expects(once()).method("listResponse").with(same("LSUB"), NULL, same("."), same("INBOX.name"));      
        encoder.encode(new LSubResponse(false, false, false, false, ".", "INBOX.name"), (ImapResponseComposer) composer.proxy());
    }
    
    public void testDelimiter() throws Exception {
        composer.expects(once()).method("listResponse").with(same("LSUB"), NULL, same("@"), same("INBOX.name"));      
        encoder.encode(new LSubResponse(false, false, false, false, "@", "INBOX.name"), (ImapResponseComposer) composer.proxy());
    }
    
    public void testNoDelimiter() throws Exception {
        composer.expects(once()).method("listResponse").with(same("LSUB"), NULL, NULL, same("INBOX.name"));      
        encoder.encode(new LSubResponse(false, false, false, false, null, "INBOX.name"), (ImapResponseComposer) composer.proxy());
    }
    
    public void testAllAttributes() throws Exception {
        String[] all = {ImapConstants.NAME_ATTRIBUTE_NOINFERIORS, ImapConstants.NAME_ATTRIBUTE_NOSELECT, 
                ImapConstants.NAME_ATTRIBUTE_MARKED, ImapConstants.NAME_ATTRIBUTE_UNMARKED};
        composer.expects(once()).method("listResponse").with(same("LSUB"), eq(Arrays.asList(all)), same("."), same("INBOX.name"));      
        encoder.encode(new LSubResponse(true, true, true, true, ".", "INBOX.name"), (ImapResponseComposer) composer.proxy());
    }
    
    public void testNoInferiors() throws Exception {
        String[] values = {ImapConstants.NAME_ATTRIBUTE_NOINFERIORS};
        composer.expects(once()).method("listResponse").with(same("LSUB"), eq(Arrays.asList(values)), same("."), same("INBOX.name"));      
        encoder.encode(new LSubResponse(true, false, false, false, ".", "INBOX.name"), (ImapResponseComposer) composer.proxy());
    }
    
    public void testNoSelect() throws Exception {
        String[] values = {ImapConstants.NAME_ATTRIBUTE_NOSELECT};
        composer.expects(once()).method("listResponse").with(same("LSUB"), eq(Arrays.asList(values)), same("."), same("INBOX.name"));      
        encoder.encode(new LSubResponse(false, true, false, false, ".", "INBOX.name"), (ImapResponseComposer) composer.proxy());
    }
    
    public void testMarked() throws Exception {
        String[] values = {ImapConstants.NAME_ATTRIBUTE_MARKED};
        composer.expects(once()).method("listResponse").with(same("LSUB"), eq(Arrays.asList(values)), same("."), same("INBOX.name"));      
        encoder.encode(new LSubResponse(false, false, true, false, ".", "INBOX.name"), (ImapResponseComposer) composer.proxy());
    }
    
    public void testUnmarked() throws Exception {
        String[] values = {ImapConstants.NAME_ATTRIBUTE_UNMARKED};
        composer.expects(once()).method("listResponse").with(same("LSUB"), eq(Arrays.asList(values)), same("."), same("INBOX.name"));      
        encoder.encode(new LSubResponse(false, false, false, true, ".", "INBOX.name"), (ImapResponseComposer) composer.proxy());
    }
}
