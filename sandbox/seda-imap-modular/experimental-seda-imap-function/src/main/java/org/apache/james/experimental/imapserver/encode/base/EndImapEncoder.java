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

package org.apache.james.experimental.imapserver.encode.base;

import org.apache.avalon.framework.logger.AbstractLogEnabled;
import org.apache.avalon.framework.logger.Logger;
import org.apache.james.api.imap.ImapMessage;
import org.apache.james.experimental.imapserver.ImapSession;
import org.apache.james.experimental.imapserver.encode.ImapEncoder;
import org.apache.james.experimental.imapserver.encode.ImapResponseComposer;

public class EndImapEncoder extends AbstractLogEnabled implements ImapEncoder {

    public void encode(ImapMessage message, ImapResponseComposer composer, ImapSession session) {
        final Logger logger = getLogger();
        logger.warn("Unknown message");
        if (logger.isDebugEnabled()) {
            logger.debug("Chain end reached for " + message);
        }
        composer.untaggedNoResponse("Unknown message in pipeline", null);
    }
}
