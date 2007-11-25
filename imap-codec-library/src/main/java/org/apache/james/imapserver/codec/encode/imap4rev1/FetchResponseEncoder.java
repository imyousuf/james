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

import java.io.IOException;

import javax.mail.Flags;

import org.apache.james.api.imap.ImapConstants;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.imap.message.response.imap4rev1.FetchResponse;
import org.apache.james.imap.message.response.imap4rev1.LegacyFetchResponse;
import org.apache.james.imapserver.codec.encode.ImapEncoder;
import org.apache.james.imapserver.codec.encode.ImapResponseComposer;
import org.apache.james.imapserver.codec.encode.base.AbstractChainedImapEncoder;

public class FetchResponseEncoder extends AbstractChainedImapEncoder {

    public FetchResponseEncoder(final ImapEncoder next) {
        super(next);
    }

    public boolean isAcceptable(final ImapMessage message) {
        return (message instanceof LegacyFetchResponse) 
            || (message instanceof FetchResponse);
    }

    protected void doEncode(ImapMessage acceptableMessage, ImapResponseComposer composer) throws IOException {
        if (acceptableMessage instanceof FetchResponse) {
            final FetchResponse fetchResponse = (FetchResponse) acceptableMessage;
            final long messageNumber = fetchResponse.getMessageNumber();
            composer.openFetchResponse(messageNumber);
            final Flags flags = fetchResponse.getFlags();
            if (flags != null) {
                composer.flags(flags);
            }
            final Long uid = fetchResponse.getUid();
            if (uid != null) {
                composer.message(ImapConstants.UID);
                composer.message(uid.longValue());
            }
            composer.closeFetchResponse();
        } else {
            final LegacyFetchResponse fetchResponse = (LegacyFetchResponse) acceptableMessage;
            encodeLegacy(composer, fetchResponse);
        }
    }

    private void encodeLegacy(ImapResponseComposer composer, final LegacyFetchResponse fetchResponse) throws IOException {
        // TODO: this is inefficient
        final String data = fetchResponse.getData();
        final int number = fetchResponse.getNumber();
        composer.legacyFetchResponse(number, data);
    }

}
