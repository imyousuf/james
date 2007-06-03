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
package org.apache.james.experimental.imapserver.message.response.imap4rev1.legacy;

import javax.mail.Flags;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.experimental.imapserver.message.response.AbstractImapResponse;

/**
 * @deprecated responses should correspond directly to the specification
 */
public class ExamineAndSelectResponse extends AbstractImapResponse {
    private final Flags permanentFlags;
    private final boolean writeable ;
    private final int recentCount;
    private final long uidValidity;
    private final int firstUnseenMessageNumber;
    private final int messageCount;

    public ExamineAndSelectResponse(ImapCommand command, final Flags permanentFlags,
            final boolean writeable, final int recentCount, 
            final long uidValidity, final int firstUnseenMessageNumber,
            final int messageCount, final String tag) {
        super(command, tag);
        this.permanentFlags = permanentFlags;
        this.writeable = writeable;
        this.recentCount = recentCount;
        this.uidValidity = uidValidity;
        this.firstUnseenMessageNumber = firstUnseenMessageNumber;
        this.messageCount = messageCount;
    }

    public final int getFirstUnseenMessageNumber() {
        return firstUnseenMessageNumber;
    }

    public final int getMessageCount() {
        return messageCount;
    }

    public final Flags getPermanentFlags() {
        return permanentFlags;
    }

    public final int getRecentCount() {
        return recentCount;
    }

    public final long getUidValidity() {
        return uidValidity;
    }

    public final boolean isWriteable() {
        return writeable;
    }        
    
}
