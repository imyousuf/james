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

import java.util.List;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.imap.message.response.imap4rev1.server.SearchResponse;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.apache.james.imapserver.codec.encode.base.AbstractChainedImapEncoder;

/**
 * Encoders IMAP4rev1 <code>SEARCH</code> responses.
 */
public class SearchResponseEncoder extends AbstractChainedImapEncoder {
    
    public SearchResponseEncoder(ImapEncoder next) {
        super(next);
    }

    protected void doEncode(ImapMessage acceptableMessage, ImapResponseComposer composer) {
        SearchResponse response = (SearchResponse) acceptableMessage;
        final long[] ids = response.getIds();
        composer.searchResponse(ids);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof SearchResponse);
    } 
}
