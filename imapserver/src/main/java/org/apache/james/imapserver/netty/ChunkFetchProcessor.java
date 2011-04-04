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
package org.apache.james.imapserver.netty;

import java.util.List;

import org.apache.james.imap.api.message.FetchData;
import org.apache.james.imap.api.message.response.StatusResponseFactory;
import org.apache.james.imap.api.process.ImapProcessor;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.processor.fetch.FetchProcessor;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxManager;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageRange;
import org.jboss.netty.channel.Channel;

/**
 * {@link FetchProcessor} implementation which does some optimization to support
 * good performance in NIO and IO related to NETTY
 */
public class ChunkFetchProcessor extends FetchProcessor {

    public ChunkFetchProcessor(ImapProcessor next, MailboxManager mailboxManager, StatusResponseFactory factory, int batchSize) {
        super(next, mailboxManager, factory, batchSize);
    }

    /**
     * Wrap the given parameters in a {@link FetchChunkedInput} and write it the
     * the {@link Channel}
     */
    @Override
    protected void processMessageRanges(ImapSession session, MessageManager mailbox, List<MessageRange> range, FetchData fetch, boolean useUids, MailboxSession mailboxSession, Responder responder) throws MailboxException {
        Channel channel = ((NettyImapSession) session).getChannel();
        channel.write(new FetchChunkedInput(session, mailbox, range, fetch, getFetchGroup(fetch), useUids, mailboxSession, responder));
    }

}
