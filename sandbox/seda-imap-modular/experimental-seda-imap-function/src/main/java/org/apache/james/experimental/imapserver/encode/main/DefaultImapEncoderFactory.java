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

package org.apache.james.experimental.imapserver.encode.main;

import org.apache.james.experimental.imapserver.encode.ImapEncoder;
import org.apache.james.experimental.imapserver.encode.base.EndImapEncoder;
import org.apache.james.experimental.imapserver.encode.imap4rev1.*;
import org.apache.james.experimental.imapserver.encode.imap4rev1.status.*;
import org.apache.james.experimental.imapserver.encode.imap4rev1.legacy.*;

/**
 * TODO: perhaps a POJO would be better
 */
public class DefaultImapEncoderFactory {
    
    public static final ImapEncoder createDefaultEncoder() {
        final EndImapEncoder endImapEncoder = new EndImapEncoder();
        final UntaggedNoResponseEncoder untaggedNoResponseEncoder = new UntaggedNoResponseEncoder(endImapEncoder);
        final RecentResponseEncoder recentResponseEncoder = new RecentResponseEncoder(untaggedNoResponseEncoder);
        final FetchResponseEncoder fetchResponseEncoder = new FetchResponseEncoder(recentResponseEncoder);
        final ExpungeResponseEncoder expungeResponseEncoder = new ExpungeResponseEncoder(fetchResponseEncoder);
        final ExistsResponseEncoder existsResponseEncoder = new ExistsResponseEncoder(expungeResponseEncoder);
        final StoreResponseEncoder storeResponseEncoder = new StoreResponseEncoder(existsResponseEncoder);
        final StatusResponseEncoder statusResponseEncoder = new StatusResponseEncoder(storeResponseEncoder);
        final SearchResponseEncoder searchResponseEncoder = new SearchResponseEncoder(statusResponseEncoder);
        final LogoutResponseEncoder logoutResponseEncoder = new LogoutResponseEncoder(searchResponseEncoder);
        final ListResponseEncoder listResponseEncoder = new ListResponseEncoder(logoutResponseEncoder);
        final LegacyFetchResponseEncoder legacyFetchResponseEncoder = new LegacyFetchResponseEncoder(listResponseEncoder);
        final ExamineAndSelectResponseEncoder examineAndSelectResponseEncoder = new ExamineAndSelectResponseEncoder(legacyFetchResponseEncoder);
        final ErrorResponseEncoder errorResponseEncoder = new ErrorResponseEncoder(examineAndSelectResponseEncoder);
        final CommandFailedResponseEncoder commandFailedResponseEncoder = new CommandFailedResponseEncoder(errorResponseEncoder);
        final CommandCompleteResponseEncoder commandCompleteResponseEncoder = new CommandCompleteResponseEncoder(commandFailedResponseEncoder);
        final CloseResponseEncoder closeResponseEncoder = new CloseResponseEncoder(commandCompleteResponseEncoder);
        final CapabilityResponseEncoder capabilityResponseEncoder = new CapabilityResponseEncoder(closeResponseEncoder);
        final BadResponseEncoder result = new BadResponseEncoder(capabilityResponseEncoder);
        return result;
    }
   
}
