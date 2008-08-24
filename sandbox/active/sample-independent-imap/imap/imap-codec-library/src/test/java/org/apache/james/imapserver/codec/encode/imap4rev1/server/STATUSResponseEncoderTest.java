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
import org.apache.james.imap.message.response.imap4rev1.server.STATUSResponse;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.jmock.Mock;
import org.jmock.MockObjectTestCase;
import org.jmock.core.Constraint;

public class STATUSResponseEncoderTest extends MockObjectTestCase{

    STATUSResponseEncoder encoder;
    Mock mockNextEncoder;
    Mock composer;
    
    protected void setUp() throws Exception {
        super.setUp();
        mockNextEncoder = mock(ImapEncoder.class);
        composer = mock(ImapResponseComposer.class);    
        encoder = new STATUSResponseEncoder((ImapEncoder) mockNextEncoder.proxy());
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testIsAcceptable() throws Exception {
        assertTrue(encoder.isAcceptable(new STATUSResponse(null, null, null, null, null,"mailbox")));
        assertFalse(encoder.isAcceptable((ImapMessage) mock(ImapMessage.class).proxy()));
        assertFalse(encoder.isAcceptable(null));
    }
    
    public void testDoEncode() throws Exception {
        Long messages = new Long(2);
        Long recent = new Long(3);
        Long uidNext = new Long(5);
        Long uidValidity = new Long(7);
        Long unseen = new Long(11);
        String mailbox = "A mailbox named desire";
        Constraint[] args = {same(messages), same(recent), same(uidNext), 
                same(uidValidity), same(unseen), same(mailbox)};
        
        composer.expects(once()).method("statusResponse").with(args);
        encoder.encode(new STATUSResponse(messages, recent, uidNext, uidValidity, unseen, mailbox),
                (ImapResponseComposer) composer.proxy());
    }
}
