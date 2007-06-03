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
package org.apache.james.experimental.imapserver.encode.imap4rev1.legacy;

import java.util.Iterator;
import java.util.List;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.experimental.imapserver.encode.ImapEncoder;
import org.apache.james.experimental.imapserver.encode.ImapResponseComposer;
import org.apache.james.experimental.imapserver.encode.base.AbstractChainedImapEncoder;
import org.apache.james.experimental.imapserver.message.response.imap4rev1.legacy.FetchResponse;

/**
 * @deprecated responses should correspond directly to the specification
 */
public class LegacyFetchResponseEncoder extends AbstractChainedImapEncoder {

    public LegacyFetchResponseEncoder(ImapEncoder next) {
        super(next);
    }

        protected void doEncode(ImapMessage acceptableMessage,
            ImapResponseComposer composer) {
        FetchResponse response = (FetchResponse) acceptableMessage;
        final List messageData = response.getMessageData();
        for (final Iterator it = messageData.iterator(); it.hasNext();) {
            FetchResponse.MessageData data = (FetchResponse.MessageData) it.next();
            final int number = data.getNumber();
            final String content = data.getData();
            composer.fetchResponse(number, content);
        }
        List unsolicitedResponses = response.getUnsolicatedResponses();
        chainEncodeAll(unsolicitedResponses, composer);
        final ImapCommand command = response.getCommand();
        final String tag = response.getTag();
        composer.commandComplete(command, tag);
    }

    protected boolean isAcceptable(ImapMessage message) {
        return (message instanceof FetchResponse);
    }
}
