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
package org.apache.james.imap.message.request.imap4rev1;

import javax.mail.Flags;

import org.apache.james.api.imap.ImapCommand;
import org.apache.james.api.imap.message.IdRange;
import org.apache.james.api.imap.message.StoreDirective;
import org.apache.james.imap.message.request.base.AbstractImapRequest;

public class StoreRequest extends AbstractImapRequest {
    private final IdRange[] idSet;

    private final StoreDirective directive;

    private final Flags flags;

    private final boolean useUids;

    public StoreRequest(final ImapCommand command, final IdRange[] idSet,
            final StoreDirective directive, final Flags flags,
            final boolean useUids, final String tag) {
        super(tag, command);
        this.idSet = idSet;
        this.directive = directive;
        this.flags = flags;
        this.useUids = useUids;
    }

    public final StoreDirective getDirective() {
        return directive;
    }

    public final Flags getFlags() {
        return flags;
    }

    public final IdRange[] getIdSet() {
        return idSet;
    }

    public final boolean isUseUids() {
        return useUids;
    }
}
