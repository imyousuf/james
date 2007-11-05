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

package org.apache.james.imapserver.processor.base;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.api.imap.message.request.ImapRequest;
import org.apache.james.api.imap.message.response.ImapResponseMessage;
import org.apache.james.api.imap.process.ImapProcessor;
import org.apache.james.api.imap.process.ImapSession;
import org.apache.james.imap.message.response.imap4rev1.legacy.BadResponse;

public class UnknownRequestImapProcessor extends AbstractLogEnabled implements ImapProcessor {

    public ImapResponseMessage process(ImapMessage message, ImapSession session) {
        Logger logger = getLogger();
        if (logger != null && logger.isDebugEnabled()) {
            logger.debug("Unknown message: " + message);
        }
        final ImapResponseMessage result;
        if (message instanceof ImapRequest) {
            ImapRequest request = (ImapRequest) message;
            result = new BadResponse("Unknown command.", request.getTag());
        } else {
            result = new BadResponse("Unknown command.");
        }
        return result;
    }

    public void process(ImapMessage message, Responder responder, ImapSession session) {
        final ImapResponseMessage response = process(message, session);
        responder.respond(response);
    }

}
