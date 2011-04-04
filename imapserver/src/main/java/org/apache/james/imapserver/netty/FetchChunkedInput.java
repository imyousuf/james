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

import java.util.Iterator;
import java.util.List;

import org.apache.james.imap.api.message.FetchData;
import org.apache.james.imap.api.process.ImapSession;
import org.apache.james.imap.api.process.ImapProcessor.Responder;
import org.apache.james.imap.message.response.FetchResponse;
import org.apache.james.imap.processor.fetch.EnvelopeBuilder;
import org.apache.james.imap.processor.fetch.FetchResponseBuilder;
import org.apache.james.mailbox.MailboxException;
import org.apache.james.mailbox.MailboxSession;
import org.apache.james.mailbox.MessageManager;
import org.apache.james.mailbox.MessageRange;
import org.apache.james.mailbox.MessageRangeException;
import org.apache.james.mailbox.MessageResult;
import org.apache.james.mailbox.MessageManager.MessageCallback;
import org.apache.james.mailbox.MessageResult.FetchGroup;
import org.apache.james.mime4j.field.address.parser.ParseException;
import org.jboss.netty.handler.stream.ChunkedInput;

/**
 * {@link ChunkedInput} implementation which fetch {@link MessageRange} in
 * batches and hand them over the the {@link Responder}
 */
public class FetchChunkedInput implements ChunkedInput {

    private Iterator<MessageRange> ranges;
    private MessageManager mailbox;
    private ImapSession session;
    private FetchData fetch;
    private boolean useUids;
    private MailboxSession mailboxSession;
    private FetchGroup group;
    private FetchResponseBuilder builder;
    private Responder responder;

    public FetchChunkedInput(final ImapSession session, final MessageManager mailbox, final List<MessageRange> ranges, final FetchData fetch, FetchGroup group, final boolean useUids, final MailboxSession mailboxSession, final Responder responder) {
        this.ranges = ranges.iterator();
        this.mailbox = mailbox;
        this.session = session;
        this.fetch = fetch;
        this.useUids = useUids;
        this.mailboxSession = mailboxSession;
        this.group = group;
        builder = new FetchResponseBuilder(new EnvelopeBuilder(session.getLog()));
        this.responder = responder;

    }

    /**
     * Do nothing
     */
    public void close() throws Exception {
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.netty.handler.stream.ChunkedInput#hasNextChunk()
     */
    public boolean hasNextChunk() throws Exception {
        return ranges.hasNext();
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.jboss.netty.handler.stream.ChunkedInput#isEndOfInput()
     */
    public boolean isEndOfInput() throws Exception {
        return !ranges.hasNext();
    }

    /**
     * Fetch the next batch of messages and write the {@link FetchResponse} to
     * the {@link Responder}. After that is done this method will return null
     * 
     * TODO: Maybe it would make sense to only write one FetchResponse per
     * {@link #nextChunk()} call. Need to test this
     */
    public Object nextChunk() throws Exception {
        if (hasNextChunk()) {
            mailbox.getMessages(ranges.next(), group, mailboxSession, new MessageCallback() {

                public void onMessages(Iterator<MessageResult> it) throws MailboxException {
                    while (it.hasNext()) {
                        final MessageResult result = it.next();
                        try {
                            final FetchResponse response = builder.build(fetch, result, mailbox, session, useUids);
                            responder.respond(response);
                        } catch (ParseException e) {
                            // we can't for whatever reason parse the
                            // message so just skip it and log it to debug
                            session.getLog().debug("Unable to parse message with uid " + result.getUid(), e);
                        } catch (MessageRangeException e) {
                            // we can't for whatever reason find the message
                            // so just skip it and log it to debug
                            session.getLog().debug("Unable to find message with uid " + result.getUid(), e);
                        }
                    }
                }
            });
        }

        return null;
    }
}
